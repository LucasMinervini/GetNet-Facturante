import axios from 'axios'

const API = import.meta.env.VITE_API_URL || 'http://localhost:1234'
const USE_MOCKS = String(import.meta.env.VITE_USE_MOCKS || 'false').toLowerCase() === 'true'

// ---- Mocks locales para avanzar sin backend ----
let MOCK_TRANSACTIONS = []

const ensureMockData = () => {
  if (MOCK_TRANSACTIONS.length > 0) return
  const statuses = ['AUTHORIZED', 'PAID', 'FAILED', 'REFUNDED']
  const currencies = ['ARS', 'BRL']
  const now = Date.now()
  const total = 137
  for (let i = 1; i <= total; i++) {
    const status = statuses[i % statuses.length]
    const createdAt = new Date(now - i * 3600_000).toISOString()
    const capturedAt = status === 'PAID' ? new Date(now - i * 3500_000).toISOString() : null
    const amount = Number(((i * 1234) % 99999) + 100).toFixed(2)
    const billingStatus = status === 'PAID' ? (i % 4 === 0 ? 'billed' : (i % 5 === 0 ? 'error' : 'pending')) : 'not_applicable'
    const creditNoteNumber = status === 'REFUNDED' ? `NC-${1000 + i}` : null
    MOCK_TRANSACTIONS.push({
      id: String(i),
      externalId: `EXT-${100000 + i}`,
      status,
      amount: Number(amount),
      currency: currencies[i % currencies.length],
      createdAt,
      capturedAt,
      customerName: `Cliente ${i}`,
      customerDoc: `DOC${20000000 + i}`,
      billingStatus,
      creditNoteNumber
    })
  }
}

const mockDelay = (ms = 300) => new Promise(res => setTimeout(res, ms))

const applyFiltersSortPaginate = (items, params) => {
  let list = [...items]
  const status = params.get('status') || ''
  const billingStatus = params.get('billingStatus') || ''
  const minAmount = parseFloat(params.get('minAmount'))
  const maxAmount = parseFloat(params.get('maxAmount'))
  const search = (params.get('search') || '').toLowerCase()
  const startDate = params.get('startDate') ? new Date(params.get('startDate')).getTime() : null
  const endDate = params.get('endDate') ? new Date(params.get('endDate')).getTime() : null
  const sortBy = params.get('sortBy') || 'createdAt'
  const sortDir = (params.get('sortDir') || 'desc').toLowerCase()

  if (status) list = list.filter(i => i.status === status)
  if (billingStatus) list = list.filter(i => (i.billingStatus || ''))
    .filter(i => String(i.billingStatus || '') === billingStatus)
  if (!Number.isNaN(minAmount)) list = list.filter(i => Number(i.amount) >= minAmount)
  if (!Number.isNaN(maxAmount)) list = list.filter(i => Number(i.amount) <= maxAmount)
  if (search) {
    list = list.filter(i =>
      String(i.id).toLowerCase().includes(search) ||
      String(i.externalId).toLowerCase().includes(search) ||
      String(i.customerName || '').toLowerCase().includes(search)
    )
  }
  if (startDate) list = list.filter(i => new Date(i.createdAt).getTime() >= startDate)
  if (endDate) list = list.filter(i => new Date(i.createdAt).getTime() <= endDate)

  list.sort((a, b) => {
    const av = a[sortBy]
    const bv = b[sortBy]
    let cmp = 0
    if (av == null && bv != null) cmp = -1
    else if (av != null && bv == null) cmp = 1
    else if (av < bv) cmp = -1
    else if (av > bv) cmp = 1
    return sortDir === 'asc' ? cmp : -cmp
  })

  const page = parseInt(params.get('page') || '0', 10)
  const size = parseInt(params.get('size') || '20', 10)
  const start = page * size
  const end = start + size
  const content = list.slice(start, end)
  const totalElements = list.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  return { content, totalElements, totalPages }
}

// Axios instance con Authorization automático
const axiosInstance = axios.create({ baseURL: API })
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
axiosInstance.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      try { localStorage.removeItem('accessToken') } catch {}
      if (typeof window !== 'undefined') {
        // Redirigir a login si hay navegación disponible
        window.location.href = '/'
      }
    }
    return Promise.reject(err)
  }
)

const apiGet = async (url) => {
  if (!USE_MOCKS) return axiosInstance.get(url)
  ensureMockData()
  await mockDelay()
  if (url.endsWith('/api/health')) {
    return { data: { status: 'ok' } }
  }
  if (url.includes('/api/transactions/list-native?')) {
    const qs = url.split('?')[1] || ''
    const params = new URLSearchParams(qs)
    const result = applyFiltersSortPaginate(MOCK_TRANSACTIONS, params)
    return { data: result }
  }
  if (url.includes('/api/transactions/pending-billing-confirmation')) {
    const qs = url.split('?')[1] || ''
    const params = new URLSearchParams(qs)
    const base = MOCK_TRANSACTIONS.filter(t => t.status === 'PAID' && (t.billingStatus === 'pending' || !t.billingStatus))
    const result = applyFiltersSortPaginate(base, params)
    return { data: result }
  }
  return { data: {} }
}

const apiPost = async (url) => {
  if (!USE_MOCKS) return axiosInstance.post(url)
  ensureMockData()
  await mockDelay()
  if (url.endsWith('/api/transactions/initialize-billing-status')) {
    MOCK_TRANSACTIONS = MOCK_TRANSACTIONS.map(t => {
      if (t.status === 'PAID' && (t.billingStatus === 'not_applicable' || !t.billingStatus)) {
        return { ...t, billingStatus: 'pending' }
      }
      return t
    })
    return { data: { ok: true } }
  }
  const confirmMatch = url.match(/\/api\/transactions\/(\w+)\/confirm-billing$/)
  if (confirmMatch) {
    const id = confirmMatch[1]
    const idx = MOCK_TRANSACTIONS.findIndex(t => String(t.id) === String(id))
    if (idx >= 0) {
      const t = MOCK_TRANSACTIONS[idx]
      MOCK_TRANSACTIONS[idx] = { ...t, billingStatus: 'billed' }
    }
    return { data: { ok: true } }
  }
  const refundMatch = url.match(/\/api\/credit-notes\/refund\/(\w+)/)
  if (refundMatch) {
    const id = refundMatch[1]
    const idx = MOCK_TRANSACTIONS.findIndex(t => String(t.id) === String(id))
    if (idx >= 0) {
      const t = MOCK_TRANSACTIONS[idx]
      MOCK_TRANSACTIONS[idx] = { ...t, status: 'REFUNDED', creditNoteNumber: `NC-${1000 + Number(id)}` }
    }
    return { data: { ok: true } }
  }
  return { data: {} }
}

const HTTP = USE_MOCKS ? { get: apiGet, post: apiPost } : { get: apiGet, post: apiPost }

export { API, USE_MOCKS, HTTP }


