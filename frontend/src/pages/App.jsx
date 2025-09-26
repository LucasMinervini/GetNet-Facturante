import React, { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import TransactionDetail from './TransactionDetail'
import BillingSettings from './BillingSettings'
import BillingConfirmationPage from './BillingConfirmationPage'

const API = import.meta.env.VITE_API_URL || 'http://localhost:1234'
const USE_MOCKS = String(import.meta.env.VITE_USE_MOCKS || 'false').toLowerCase() === 'true'

// --- Mocks ligeros para avanzar sin backend ---
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

const apiGet = async (url) => {
  if (!USE_MOCKS) return axios.get(url)
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
  // default not found
  return { data: {} }
}

const apiPost = async (url) => {
  if (!USE_MOCKS) return axios.post(url)
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

const HTTP = USE_MOCKS ? { get: apiGet, post: apiPost } : axios

export default function App() {
  const [health, setHealth] = useState('ping...')
  const [transactions, setTransactions] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [loading, setLoading] = useState(false)
  
  // Estado para transacciones pendientes de confirmación
  const [pendingTransactions, setPendingTransactions] = useState([])
  const [pendingTotalPages, setPendingTotalPages] = useState(0)
  const [pendingCurrentPage, setPendingCurrentPage] = useState(0)
  const [pendingLoading, setPendingLoading] = useState(false)
  
  // Estado para confirmación simple
  const [pendingConfirmationTransaction, setPendingConfirmationTransaction] = useState(null)
  const [selectedTransactionId, setSelectedTransactionId] = useState(null)
  const [currentView, setCurrentView] = useState('list') // 'list' | 'detail' | 'settings' | 'pending-billing' | 'confirmation'
  
  // Estado para navegación entre transacciones pendientes
  const [allPendingTransactions, setAllPendingTransactions] = useState([])
  const [currentPendingIndex, setCurrentPendingIndex] = useState(0)
  
  // Estado para selección múltiple y facturación masiva
  const [selectedTransactions, setSelectedTransactions] = useState(new Set())
  const [showBulkActions, setShowBulkActions] = useState(false)
  const [bulkProcessing, setBulkProcessing] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [toastMsg, setToastMsg] = useState('')
  // dark mode eliminado: la web usa tema oscuro por defecto
  // Modal para cuando no hay transacciones pendientes de confirmación
  const [showNoPendingModal, setShowNoPendingModal] = useState(false)
  
  // Filtros
  const [filters, setFilters] = useState({
    status: '',
    billingStatus: '',
    minAmount: '',
    maxAmount: '',
    startDate: '',
    endDate: '',
    search: '',
    sortBy: 'createdAt',
    sortDir: 'desc'
  })

  useEffect(() => {
    setSearchInput(filters.search || '')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Debounce del buscador
  useEffect(() => {
    const handle = setTimeout(() => {
      setFilters(prev => ({ ...prev, search: searchInput }))
      setCurrentPage(0)
    }, 400)
    return () => clearTimeout(handle)
  }, [searchInput])

  const fetchTransactions = async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString(),
        sortBy: filters.sortBy,
        sortDir: filters.sortDir
      })
      
      // Agregar filtros solo si tienen valor
      if (filters.status) params.append('status', filters.status)
      if (filters.billingStatus) params.append('billingStatus', filters.billingStatus)
      if (filters.minAmount) params.append('minAmount', filters.minAmount)
      if (filters.maxAmount) params.append('maxAmount', filters.maxAmount)
      console.log('Filtros completos:', filters)
      
      if (filters.startDate) {
        // Convertir datetime-local a ISO 8601 con zona horaria
        const startDate = new Date(filters.startDate)
        // Ajustar al inicio del día (00:00:00)
        startDate.setHours(0, 0, 0, 0)
        console.log('StartDate original:', filters.startDate)
        console.log('StartDate convertida (inicio del día):', startDate.toISOString())
        params.append('startDate', startDate.toISOString())
      }
      if (filters.endDate) {
        // Convertir datetime-local a ISO 8601 con zona horaria
        const endDate = new Date(filters.endDate)
        // Ajustar al fin del día (23:59:59.999)
        endDate.setHours(23, 59, 59, 999)
        console.log('EndDate original:', filters.endDate)
        console.log('EndDate convertida (fin del día):', endDate.toISOString())
        params.append('endDate', endDate.toISOString())
      } else {
        console.log('EndDate está vacío o no definido')
      }
      if (filters.search) params.append('search', filters.search)
      
      const url = `${API}/api/transactions/list-native?${params}`
      console.log('URL de la petición:', url)
      
      const response = await HTTP.get(url)
      const data = response.data
      
      console.log('Respuesta del backend:', data)
      
      setTransactions(data.content || [])
      setTotalPages(data.totalPages || 0)
      setTotalElements(data.totalElements || 0)
    } catch (error) {
      console.error('Error fetching transactions:', error)
      setTransactions([])
    } finally {
      setLoading(false)
    }
  }



  useEffect(() => {
    HTTP.get(`${API}/api/health`).then(r => setHealth(r.data.status)).catch(() => setHealth('down'))
    // Inicializar estados de facturación automáticamente al cargar la app
    initializeBillingStatusSilently()
  }, [])

  useEffect(() => {
    fetchTransactions()
  }, [currentPage, pageSize, filters])

  useEffect(() => {
    if (currentView === 'pending-billing') {
      fetchPendingTransactions()
    }
  }, [currentView, pendingCurrentPage])

  // Verificar transacciones pendientes cada 10 segundos
  useEffect(() => {
    const interval = setInterval(() => {
      checkForPendingTransactions()
    }, 10000) // Verificar cada 10 segundos

    // Verificar inmediatamente al cargar
    checkForPendingTransactions()

    return () => clearInterval(interval)
  }, [currentView, pendingConfirmationTransaction])

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }))
    setCurrentPage(0) // Reset to first page when filtering
  }

  const handleSearch = () => {
    setFilters(prev => ({ ...prev, search: searchInput }))
    setCurrentPage(0)
  }

  const clearFilters = () => {
    setFilters({
      status: '',
      billingStatus: '',
      minAmount: '',
      maxAmount: '',
      startDate: '',
      endDate: '',
      search: '',
      sortBy: 'createdAt',
      sortDir: 'desc'
    })
    setSearchInput('')
    setCurrentPage(0)
  }

  const handleTransactionClick = (transactionId) => {
    setSelectedTransactionId(transactionId)
    setCurrentView('detail')
  }

  // Funciones para transacciones pendientes de confirmación
  const fetchPendingTransactions = async () => {
    setPendingLoading(true)
    try {
      const params = new URLSearchParams({
        page: pendingCurrentPage.toString(),
        size: pageSize.toString()
      })
      
      const response = await HTTP.get(`${API}/api/transactions/pending-billing-confirmation?${params}`)
      const data = response.data
      
      setPendingTransactions(data.content || [])
      setPendingTotalPages(data.totalPages || 0)
      if ((data.content || []).length === 0) {
        setShowNoPendingModal(true)
      }
    } catch (error) {
      console.error('Error fetching pending transactions:', error)
      setPendingTransactions([])
    } finally {
      setPendingLoading(false)
    }
  }

  const confirmBilling = async (transactionId) => {
    try {
      await HTTP.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
      setToastMsg('Facturación confirmada exitosamente')
      setTimeout(() => setToastMsg(''), 3000)
      fetchPendingTransactions() // Refrescar la lista de pendientes
      fetchTransactions() // Refrescar la lista principal
    } catch (error) {
      console.error('Error confirming billing:', error)
      setToastMsg('Error al confirmar facturación')
      setTimeout(() => setToastMsg(''), 3000)
    }
  }


  // Función simple para verificar si hay transacciones pendientes (solo informativo)
  const checkForPendingTransactions = async () => {
    try {
      const response = await HTTP.get(`${API}/api/transactions/pending-billing-confirmation?page=0&size=1`)
      const pendingTransactions = response.data.content || []
      
      // Ya no navegamos automáticamente - solo guardamos la información
      if (pendingTransactions.length > 0) {
        console.log('Hay transacciones pendientes de confirmación disponibles')
      }
    } catch (error) {
      console.error('Error checking for pending transactions:', error)
    }
  }

  // Función para ir manualmente a la ventana de confirmación
  const goToConfirmationView = async () => {
    try {
      const response = await HTTP.get(`${API}/api/transactions/pending-billing-confirmation?page=0&size=50`)
      const pendingTransactions = response.data.content || []
      
      if (pendingTransactions.length > 0) {
        setAllPendingTransactions(pendingTransactions)
        setCurrentPendingIndex(0)
        setPendingConfirmationTransaction(pendingTransactions[0])
        setCurrentView('confirmation')
      } else {
        setShowNoPendingModal(true)
      }
    } catch (error) {
      console.error('Error fetching pending transactions:', error)
      setToastMsg('Error al buscar transacciones pendientes')
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  const handleConfirmationDecision = (decision, message) => {
    console.log('Decision received:', decision, message)
    
    // Mostrar mensaje de éxito/error
    setToastMsg(message)
    setTimeout(() => setToastMsg(''), 4000)
    
    // Remover la transacción procesada de la lista
    const updatedPendingTransactions = allPendingTransactions.filter(t => t.id !== pendingConfirmationTransaction.id)
    setAllPendingTransactions(updatedPendingTransactions)
    
    // Si hay más transacciones pendientes, mostrar la siguiente
    if (updatedPendingTransactions.length > 0) {
      const nextIndex = Math.min(currentPendingIndex, updatedPendingTransactions.length - 1)
      setCurrentPendingIndex(nextIndex)
      setPendingConfirmationTransaction(updatedPendingTransactions[nextIndex])
    } else {
      // No hay más transacciones, volver a la lista principal
      setPendingConfirmationTransaction(null)
      setCurrentView('list')
      setShowNoPendingModal(true)
    }
    
    // Refrescar las listas inmediatamente
    fetchTransactions()
  }

  // Función para navegar a la siguiente transacción pendiente
  const goToNextPendingTransaction = () => {
    if (currentPendingIndex < allPendingTransactions.length - 1) {
      const nextIndex = currentPendingIndex + 1
      setCurrentPendingIndex(nextIndex)
      setPendingConfirmationTransaction(allPendingTransactions[nextIndex])
    }
  }

  // Función para navegar a la transacción pendiente anterior
  const goToPreviousPendingTransaction = () => {
    if (currentPendingIndex > 0) {
      const prevIndex = currentPendingIndex - 1
      setCurrentPendingIndex(prevIndex)
      setPendingConfirmationTransaction(allPendingTransactions[prevIndex])
    }
  }

  // Función para manejar confirmación desde la tabla
  const handleBillingConfirmation = async (transactionId, e) => {
    e.stopPropagation()
    
    try {
      await HTTP.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
      setToastMsg('✅ Facturación confirmada exitosamente')
      
      setTimeout(() => setToastMsg(''), 3000)
      
      // Refrescar ambas listas inmediatamente para mostrar el cambio
      fetchTransactions()
      fetchPendingTransactions()
      
    } catch (error) {
      console.error('Error al procesar confirmación:', error)
      setToastMsg('❌ Error al procesar confirmación: ' + (error.response?.data?.message || error.message))
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  // Funciones para selección múltiple
  const handleTransactionSelect = (transactionId, isSelected) => {
    const newSelected = new Set(selectedTransactions)
    if (isSelected) {
      newSelected.add(transactionId)
    } else {
      newSelected.delete(transactionId)
    }
    setSelectedTransactions(newSelected)
    setShowBulkActions(newSelected.size > 0)
  }

  const handleSelectAll = (isSelected) => {
    if (isSelected) {
      // Seleccionar solo transacciones pendientes
      const pendingTransactionIds = transactions
        .filter(t => t.billingStatus === 'pending')
        .map(t => t.id)
      setSelectedTransactions(new Set(pendingTransactionIds))
      setShowBulkActions(pendingTransactionIds.length > 0)
    } else {
      setSelectedTransactions(new Set())
      setShowBulkActions(false)
    }
  }

  const handleBulkBilling = async () => {
    if (selectedTransactions.size === 0) return

    const confirmed = window.confirm(
      `¿Confirmar facturación de ${selectedTransactions.size} transacciones seleccionadas?\n\n` +
      `Esto generará ${selectedTransactions.size} facturas automáticamente.`
    )
    
    if (!confirmed) return

    setBulkProcessing(true)
    let successCount = 0
    let errorCount = 0

    try {
      // Procesar cada transacción seleccionada
      for (const transactionId of selectedTransactions) {
        try {
          await HTTP.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
          successCount++
        } catch (error) {
          console.error(`Error facturando transacción ${transactionId}:`, error)
          errorCount++
        }
      }

      // Mostrar resultado
      if (errorCount === 0) {
        setToastMsg(`🎉 ¡${successCount} facturas generadas exitosamente!`)
      } else {
        setToastMsg(`⚠️ ${successCount} exitosas, ${errorCount} con errores`)
      }

      // Limpiar selección y refrescar
      setSelectedTransactions(new Set())
      setShowBulkActions(false)
      fetchTransactions()
      fetchPendingTransactions()

    } catch (error) {
      setToastMsg('❌ Error en facturación masiva: ' + error.message)
    } finally {
      setBulkProcessing(false)
      setTimeout(() => setToastMsg(''), 5000)
    }
  }


  // Función para inicializar estados de facturación automáticamente
  const initializeBillingStatusSilently = async () => {
    try {
      await HTTP.post(`${API}/api/transactions/initialize-billing-status`)
      console.log('Estados de facturación inicializados automáticamente')
    } catch (error) {
      console.warn('Error inicializando estados automáticamente:', error)
    }
  }


  const handleBackToList = () => {
    setCurrentView('list')
    setSelectedTransactionId(null)
  }

  const handleOpenSettings = () => {
    setCurrentView('settings')
  }

  const handleBackFromSettings = () => {
    setCurrentView('list')
  }



  const getRelativeTime = (dateString) => {
    if (!dateString) return ''
    const rtf = new Intl.RelativeTimeFormat('es-AR', { numeric: 'auto' })
    const now = new Date()
    const date = new Date(dateString)
    const diffMs = date.getTime() - now.getTime()
    const minutes = Math.round(diffMs / 60000)
    const hours = Math.round(minutes / 60)
    const days = Math.round(hours / 24)
    if (Math.abs(minutes) < 60) return rtf.format(minutes, 'minute')
    if (Math.abs(hours) < 24) return rtf.format(hours, 'hour')
    return rtf.format(days, 'day')
  }

  const pageSubtotal = useMemo(() => {
    if (!Array.isArray(transactions)) return 0
    return transactions.reduce((sum, t) => sum + Number(t.amount || 0), 0)
  }, [transactions])

  const formatCurrency = (value, currency) => {
    try {
      const formatted = new Intl.NumberFormat('es-AR', { 
        style: 'currency', 
        currency: currency || 'ARS', 
        minimumFractionDigits: 2 
      }).format(Number(value))
      
      // Quitar el prefijo "BRL" si aparece
      return formatted.replace(/^BRL\s*/, 'R$ ')
    } catch (e) {
      return `$${Number(value || 0).toFixed(2)}`
    }
  }

  const translateStatus = (status) => {
    const statusMap = {
      'AUTHORIZED': 'Autorizada',
      'PAID': 'Pagado',
      'REFUNDED': 'Reembolsado',
      'FAILED': 'Falló'
    }
    return statusMap[status] || status
  }

  const handleDownloadInvoice = (transactionId, e) => {
    e.stopPropagation()
    window.open(`${API}/api/invoices/pdf/${transactionId}`, '_blank')
  }

  const handleProcessRefund = async (transactionId, e) => {
    e.stopPropagation()
    
    const refundReason = prompt('Ingrese el motivo del reembolso:', 'Reembolso solicitado por el cliente')
    if (!refundReason) return
    
    try {
      const response = await HTTP.post(`${API}/api/credit-notes/refund/${transactionId}?refundReason=${encodeURIComponent(refundReason)}`)
      
      if (response.status === 200) {
        setToastMsg('Reembolso procesado exitosamente')
        setTimeout(() => setToastMsg(''), 2000)
        // Recargar transacciones para mostrar el cambio
        fetchTransactions()
      }
    } catch (error) {
      console.error('Error al procesar reembolso:', error)
      setToastMsg('Error al procesar reembolso: ' + (error.response?.data?.message || error.message))
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  const handleDownloadCreditNote = (transactionId, e) => {
    e.stopPropagation()
    window.open(`${API}/api/credit-notes/pdf/${transactionId}`, '_blank')
  }

  // Renderizar vista de detalle si está seleccionada
  if (currentView === 'detail' && selectedTransactionId) {
    return (
      <TransactionDetail 
        transactionId={selectedTransactionId} 
        onBack={handleBackToList} 
      />
    )
  }

  // Renderizar vista de configuración si está seleccionada
  if (currentView === 'settings') {
    return (
      <BillingSettings 
        onBack={handleBackFromSettings} 
      />
    )
  }

  // Renderizar página de confirmación si hay transacción pendiente
  if (currentView === 'confirmation' && pendingConfirmationTransaction) {
    return (
      <BillingConfirmationPage 
        transaction={pendingConfirmationTransaction}
        onDecision={handleConfirmationDecision}
        onBackToHome={() => setCurrentView('list')}
        onNextTransaction={goToNextPendingTransaction}
        onPreviousTransaction={goToPreviousPendingTransaction}
        hasNext={currentPendingIndex < allPendingTransactions.length - 1}
        hasPrevious={currentPendingIndex > 0}
        currentIndex={currentPendingIndex + 1}
        totalTransactions={allPendingTransactions.length}
      />
    )
  }

  // Renderizar vista de transacciones pendientes de confirmación
  if (currentView === 'pending-billing') {
    return (
      <div style={{ padding: '2rem', minHeight: '100vh' }}>
        {/* Header */}
        <div className="header">
          {/* Capa de íconos animados */}
          <div className="header-icons-layer" aria-hidden="true">
            <div className="icon-swarm">
              <span></span>
              <span></span>
              <span></span>
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          <div className="header-buttons">
            <button
              onClick={() => setCurrentView('list')}
              className="btn btn-secondary"
              style={{ padding: '0.5rem 1rem' }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M19 12H5M12 19l-7-7 7-7"/>
              </svg>
              Volver a Lista Principal
            </button>
          </div>
          <div className="header-content">
            <div className="header-title">
              <h1>
                <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14,2 14,8 20,8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10,9 9,9 8,9"/>
                </svg>
                Confirmación de Facturación
              </h1>
              <h2>Transacciones Pendientes</h2>
            </div>
          </div>
        </div>

        {/* Toast notification */}
        {toastMsg && (
          <div className="toast-notification">
            {toastMsg}
          </div>
        )}

        {/* Contenido principal */}
        <div className="main-content">
          {pendingLoading ? (
            <div className="loading-container">
              <div className="loading-spinner"></div>
              <p>Cargando transacciones pendientes...</p>
            </div>
          ) : pendingTransactions.length === 0 ? (
            <div className="empty-state">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 12l2 2 4-4"/>
                <path d="M21 12c0 4.97-4.03 9-9 9s-9-4.03-9-9 4.03-9 9-9c1.66 0 3.22.45 4.56 1.23"/>
              </svg>
              <h3>¡Excelente!</h3>
              <p>No hay transacciones pendientes de confirmación de facturación.</p>
            </div>
          ) : (
            <>
              <div className="table-container">
                <table className="transactions-table">
                  <thead>
                    <tr>
                      <th>ID Externo</th>
                      <th>Monto</th>
                      <th>Cliente</th>
                      <th>Fecha</th>
                      <th>Estado</th>
                      <th>Estado Facturación</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pendingTransactions.map((transaction) => (
                      <tr key={transaction.id}>
                        <td>
                          <span className="transaction-id">{transaction.externalId}</span>
                        </td>
                        <td>
                          <span className="amount">{formatCurrency(transaction.amount)}</span>
                        </td>
                        <td>
                          <div className="customer-info">
                            <span className="customer-name">{transaction.customerName || 'N/A'}</span>
                            <span className="customer-doc">{transaction.customerDoc || 'N/A'}</span>
                          </div>
                        </td>
                        <td>
                          <span className="date">{new Date(transaction.createdAt).toLocaleDateString('es-AR')}</span>
                        </td>
                        <td>
                          <span className="status-badge status-pending-billing">
                            Pendiente Confirmación
                          </span>
                        </td>
                        <td>
                          <div className="action-buttons">
                            <button
                              onClick={() => confirmBilling(transaction.id)}
                              className="btn btn-success btn-sm"
                              title="Confirmar facturación"
                            >
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M20 6L9 17l-5-5"/>
                              </svg>
                              Facturar
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Paginación */}
              {pendingTotalPages > 1 && (
                <div className="pagination">
                  <button
                    onClick={() => setPendingCurrentPage(Math.max(0, pendingCurrentPage - 1))}
                    disabled={pendingCurrentPage === 0}
                    className="btn btn-secondary btn-sm"
                  >
                    ← Anterior
                  </button>
                  <span className="page-info">
                    Página {pendingCurrentPage + 1} de {pendingTotalPages}
                  </span>
                  <button
                    onClick={() => setPendingCurrentPage(Math.min(pendingTotalPages - 1, pendingCurrentPage + 1))}
                    disabled={pendingCurrentPage >= pendingTotalPages - 1}
                    className="btn btn-secondary btn-sm"
                  >
                    Siguiente →
                  </button>
                </div>
              )}
            </>
          )}
        </div>

        {showNoPendingModal && (
          <div style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999
          }}>
            <div style={{
              background: 'white',
              color: '#111',
              borderRadius: '16px',
              padding: '24px',
              width: 'min(90vw, 420px)',
              boxShadow: '0 10px 30px rgba(0,0,0,0.25)',
              textAlign: 'center'
            }}>
              <div style={{
                marginBottom: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                fontWeight: 800,
                fontSize: '1.1rem'
              }}>
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#111" strokeWidth="2.2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M9 12l2 2 4-4"/>
                </svg>
                <strong style={{ color: '#111' }}>No hay transacciones pendientes</strong>
              </div>
              <div style={{ color: '#4b5563', marginBottom: '18px' }}>
                Ya no quedan facturas para confirmar.
              </div>
              <button
                onClick={() => setShowNoPendingModal(false)}
                style={{
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '10px',
                  padding: '10px 16px',
                  cursor: 'pointer',
                  fontWeight: 700
                }}
              >
                Entendido
              </button>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div style={{ padding: '2rem', minHeight: '100vh' }}>
      {/* Header moderno */}
      <div className="header">
        {/* Capa de íconos animados */}
        <div className="header-icons-layer" aria-hidden="true">
          <div className="icon-swarm">
            <span></span>
            <span></span>
            <span></span>
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
        <div className="header-buttons">
          <button
            onClick={handleOpenSettings}
            className="btn btn-secondary"
            style={{ padding: '0.5rem 1rem' }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1 1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
            Configuración
          </button>
          {/* Toggle de tema eliminado */}
        </div>
        <div className="header-content">
          <div className="header-title">
            <h1>
              <svg
                width="56"
                height="56" 
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <rect x="2" y="4" width="20" height="16" rx="2"/>
                <path d="M2 10h20"/>
                <path d="M6 14h4"/>
                <path d="M14 14h4"/>
                <path d="M6 17h4"/>
                <path d="M14 17h4"/>
              </svg>
              Getnet → Facturante
            </h1>
            <h2>Transacciones</h2>
          </div>
        </div>
      </div>

      {/* Cards de estadísticas */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-value">{totalElements}</div>
          <div className="stat-label">Total Transacciones</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{formatCurrency(pageSubtotal, transactions[0]?.currency || 'ARS')}</div>
          <div className="stat-label">Subtotal Página</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{transactions.filter(t => t.status === 'PAID').length}</div>
          <div className="stat-label">Transacciones Pagadas</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{transactions.filter(t => t.status === 'AUTHORIZED').length}</div>
          <div className="stat-label">Transacciones Autorizadas</div>
        </div>
      </div>
      


      
      {/* Filtros Mejorados - Diseño UX/UI */}
      <div className="filters-section">
        <div className="filters-header">
          <div className="filters-title-wrapper">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polygon points="22,3 2,3 10,12.46 10,19 14,21 14,12.46"/>
            </svg>
            <h3 className="filters-title">Filtros de Transacciones</h3>
          </div>
          <div className="filters-subtitle">Personaliza tu búsqueda para encontrar transacciones específicas</div>
        </div>
        
        <div className="filters-grid">
          {/* Estado */}
          <div className="form-group filter-card">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              Estado de Transacción
            </label>
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="form-select enhanced-select"
            >
              <option value="">Todos los estados</option>
              <option value="AUTHORIZED">⏳ Autorizada</option>
              <option value="PAID">✅ Pagado</option>
              <option value="REFUNDED">🔄 Reembolsado</option>
              <option value="FAILED">❌ Falló</option>
            </select>
          </div>
          
          {/* Estado de Facturación */}
          <div className="form-group filter-card">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
              </svg>
              Estado de Facturación
            </label>
            <select
              value={filters.billingStatus}
              onChange={(e) => handleFilterChange('billingStatus', e.target.value)}
              className="form-select enhanced-select"
            >
              <option value="">Todos los estados</option>
              <option value="pending">⏳ Pendiente confirmación</option>
              <option value="billed">✅ Facturado</option>
              <option value="error">⚠️ Error</option>
              <option value="not_applicable">➖ No aplicable</option>
            </select>
          </div>
          
          {/* Monto mínimo */}
          <div className="form-group filter-card">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
              </svg>
              Monto Mínimo
            </label>
            <div className="input-with-icon enhanced-input">
              <span className="input-prefix">$</span>
              <input
                type="number"
                value={filters.minAmount}
                onChange={(e) => handleFilterChange('minAmount', e.target.value)}
                placeholder="0.00"
                className="form-input"
                step="0.01"
                min="0"
              />
            </div>
          </div>
          
          {/* Monto máximo */}
          <div className="form-group filter-card">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
              </svg>
              Monto Máximo
            </label>
            <div className="input-with-icon enhanced-input">
              <span className="input-prefix">$</span>
              <input
                type="number"
                value={filters.maxAmount}
                onChange={(e) => handleFilterChange('maxAmount', e.target.value)}
                placeholder="999999.99"
                className="form-input"
                step="0.01"
                min="0"
              />
            </div>
          </div>
          
          {/* Fecha inicio */}
          <div className="form-group filter-card">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" >
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              Fecha de Inicio
            </label>
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => handleFilterChange('startDate', e.target.value)}
              className="form-input enhanced-date-input"
            />
          </div>
          
          {/* Fecha fin */}
          <div className="form-group filter-card">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              Fecha de Fin
            </label>
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => handleFilterChange('endDate', e.target.value)}
              className="form-input enhanced-date-input"
            />
          </div>
          
          {/* Búsqueda */}
          <div className="form-group filter-card search-group">
            <label className="form-label">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"/>
                <path d="m21 21-4.35-4.35"/>
              </svg>
              Buscar Transacción
            </label>
            <div className="search-container enhanced-search">
              <input
                type="text"
                className="form-input search-input"
                placeholder="Buscar por ID, external ID, email del cliente..."
                value={filters.search || ''}
                onChange={(e) => handleFilterChange('search', e.target.value)}
              />
              <button 
                className="search-btn enhanced-search-btn"
                onClick={() => setCurrentPage(0)}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8"/>
                  <path d="m21 21-4.35-4.35"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
        
        <div className="filters-actions">
          <div className="filters-left">
            <button onClick={clearFilters} className="btn btn-clear enhanced-clear-btn">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="3,6 5,6 21,6"/>
                <path d="m19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"/>
                <line x1="10" y1="11" x2="10" y2="17"/>
                <line x1="14" y1="11" x2="14" y2="17"/>
              </svg>
              Limpiar Filtros
            </button>
            
            <button 
              onClick={goToConfirmationView} 
              className="btn btn-primary enhanced-confirmation-btn"
              style={{
                marginLeft: '12px',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                border: 'none',
                color: 'white',
                borderRadius: '8px',
                padding: '10px 16px',
                fontSize: '0.9rem',
                fontWeight: '500',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                transition: 'all 0.3s ease',
                boxShadow: '0 2px 8px rgba(102, 126, 234, 0.3)'
              }}
              onMouseOver={(e) => {
                e.target.style.transform = 'translateY(-2px)'
                e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.4)'
              }}
              onMouseOut={(e) => {
                e.target.style.transform = 'translateY(0)'
                e.target.style.boxShadow = '0 2px 8px rgba(102, 126, 234, 0.3)'
              }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
              </svg>
              Confirmar Facturación
            </button>
          </div>
          
          <div className="filters-right">
            <div className="page-size-selector enhanced-selector">
              <label className="form-label">Elementos por página:</label>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value))
                  setCurrentPage(0)
                }}
                className="form-select enhanced-select"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </div>
          </div>
        </div>
      </div>
      
      {/* Información de resultados */}
      <div style={{
        marginBottom: '1rem',
        color: 'var(--text-secondary)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        fontSize: '0.875rem'
      }}>
        <span>
          Mostrando {transactions.length} de {totalElements} transacciones
          {loading && ' (Cargando...)'}
        </span>
      </div>

      {/* Barra de acciones masivas */}
      {showBulkActions && (
        <div style={{
          position: 'sticky',
          top: '0',
          zIndex: 100,
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          padding: '15px 20px',
          borderRadius: '12px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 4px 20px rgba(102, 126, 234, 0.3)',
          animation: 'slideInDown 0.3s ease-out'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
            <div style={{
              background: 'rgba(255, 255, 255, 0.2)',
              padding: '8px',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
              </svg>
            </div>
            <div>
              <div style={{ fontWeight: '700', fontSize: '1.1rem' }}>
                {selectedTransactions.size} transacciones seleccionadas
              </div>
              <div style={{ opacity: 0.9, fontSize: '0.9rem' }}>
                Listas para facturación masiva
              </div>
            </div>
          </div>
          
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={handleBulkBilling}
              disabled={bulkProcessing}
              style={{
                background: bulkProcessing ? 'rgba(255, 255, 255, 0.3)' : 'rgba(46, 204, 113, 0.9)',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                padding: '10px 20px',
                cursor: bulkProcessing ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontWeight: '600',
                fontSize: '0.9rem',
                transition: 'all 0.2s ease'
              }}
              onMouseEnter={(e) => {
                if (!bulkProcessing) {
                  e.target.style.background = 'rgba(46, 204, 113, 1)'
                  e.target.style.transform = 'translateY(-1px)'
                }
              }}
              onMouseLeave={(e) => {
                if (!bulkProcessing) {
                  e.target.style.background = 'rgba(46, 204, 113, 0.9)'
                  e.target.style.transform = 'translateY(0)'
                }
              }}
            >
              {bulkProcessing ? (
                <>
                  <div style={{
                    width: '16px',
                    height: '16px',
                    border: '2px solid white',
                    borderTop: '2px solid transparent',
                    borderRadius: '50%',
                    animation: 'spin 1s linear infinite'
                  }}></div>
                  Procesando...
                </>
              ) : (
                <>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14,2 14,8 20,8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <polyline points="10,9 9,9 8,9"/>
                  </svg>
                  Facturar Todas
                </>
              )}
            </button>
            
            <button
              onClick={handleBulkReject}
              disabled={bulkProcessing}
              style={{
                background: bulkProcessing ? 'rgba(255, 255, 255, 0.3)' : 'rgba(231, 76, 60, 0.9)',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                padding: '10px 20px',
                cursor: bulkProcessing ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontWeight: '600',
                fontSize: '0.9rem',
                transition: 'all 0.2s ease'
              }}
              onMouseEnter={(e) => {
                if (!bulkProcessing) {
                  e.target.style.background = 'rgba(231, 76, 60, 1)'
                  e.target.style.transform = 'translateY(-1px)'
                }
              }}
              onMouseLeave={(e) => {
                if (!bulkProcessing) {
                  e.target.style.background = 'rgba(231, 76, 60, 0.9)'
                  e.target.style.transform = 'translateY(0)'
                }
              }}
            >
              {bulkProcessing ? (
                <>
                  <div style={{
                    width: '16px',
                    height: '16px',
                    border: '2px solid white',
                    borderTop: '2px solid transparent',
                    borderRadius: '50%',
                    animation: 'spin 1s linear infinite'
                  }}></div>
                  Procesando...
                </>
              ) : (
                <>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M18 6L6 18M6 6l12 12"/>
                  </svg>
                  Rechazar Todas
                </>
              )}
            </button>
            
            <button
              onClick={() => {
                setSelectedTransactions(new Set())
                setShowBulkActions(false)
              }}
              style={{
                background: 'rgba(255, 255, 255, 0.2)',
                color: 'white',
                border: '1px solid rgba(255, 255, 255, 0.3)',
                borderRadius: '8px',
                padding: '10px 15px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontWeight: '500',
                fontSize: '0.9rem',
                transition: 'all 0.2s ease'
              }}
              onMouseEnter={(e) => {
                e.target.style.background = 'rgba(255, 255, 255, 0.3)'
              }}
              onMouseLeave={(e) => {
                e.target.style.background = 'rgba(255, 255, 255, 0.2)'
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
              Cancelar
            </button>
          </div>
        </div>
      )}

      {/* Tabla moderna */}
      <div className="table-container">
        <table className="table">
        <thead>
          <tr>
            <th style={{textAlign: 'center'}}>CLIENTE</th>
            <th style={{textAlign: 'center'}}>ESTADO DE PAGO</th>
            <th style={{textAlign: 'center'}}>MONTO</th>
            <th style={{textAlign: 'center'}}>MONEDA</th>
            <th style={{textAlign: 'center'}}>FECHA FACTURA</th>
            <th style={{textAlign: 'center'}}>REEMBOLSO (motivo)</th>
            <th style={{textAlign: 'center'}}>FACTURACIÓN</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map(t => (
            <tr 
              key={t.id} 
              onClick={() => handleTransactionClick(t.id)}
            >
              <td style={{textAlign: 'center'}}>{t.externalId}</td>
              <td style={{textAlign: 'center'}}>
                <span className={`status-badge status-${t.status.toLowerCase()}`}>
                  {translateStatus(t.status)}
                </span>
              </td>
              <td style={{textAlign: 'center'}}>
                {formatCurrency(t.amount, t.currency)}
              </td>
              <td style={{textAlign: 'center'}}>{t.currency}</td>
              <td style={{textAlign: 'center'}} title={getRelativeTime(t.capturedAt || t.createdAt)}>
                {(() => {
                  const dateValue = t.capturedAt || t.createdAt
                  return dateValue ? new Date(dateValue).toLocaleDateString('es-AR') : '—'
                })()}
              </td>
              <td style={{textAlign: 'center'}}>
                {t.status === 'REFUNDED' ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'center' }}>
                    <span
                      title={t.refundReason || t.creditNoteReason || 'Reembolso aplicado'}
                      style={{ fontSize: '0.9rem', color: '#ffffff', fontWeight: 600 }}
                    >
                      {t.refundReason || t.creditNoteReason || 'Reembolso aplicado'}
                    </span>
                    {t.creditNoteNumber && (
                      <span style={{
                        display: 'inline-block',
                        padding: '2px 8px',
                        borderRadius: '999px',
                        background: '#f1f5f9',
                        color: '#374151',
                        border: '1px solid #e5e7eb',
                        fontSize: '0.72rem',
                        fontWeight: 700
                      }}>
                        {t.creditNoteNumber}
                      </span>
                    )}
                  </div>
                ) : (
                  '—'
                )}
              </td>
              <td style={{textAlign: 'center'}}>
                {/* Columna unificada de facturación */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' }}>
                  
                  {/* Estado y botón de confirmación */}
                  {t.status === 'REFUNDED' && t.creditNoteNumber ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' }}>
                      <span style={{
                        padding: '4px 10px',
                        borderRadius: '999px',
                        fontSize: '0.9rem',
                        fontWeight: 800,
                        color: '#374151',
                        background: '#e9f7ef',
                        border: '1px solid #2e7d3224',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px'
                      }}>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                          <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
                          <path d="M21 3v5h-5"/>
                        </svg>
                        Reembolsado
                      </span>
                      <button
                        onClick={(e) => handleDownloadCreditNote(t.id, e)}
                        title="Descargar nota de crédito PDF"
                        style={{
                          background: 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)',
                          color: '#111111',
                          border: 'none',
                          borderRadius: '8px',
                          padding: '8px 12px',
                          fontSize: '0.75rem',
                          fontWeight: '600',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                          boxShadow: '0 4px 12px rgba(39, 174, 96, 0.3)',
                          transition: 'all 0.3s ease',
                          minWidth: '110px'
                        }}
                        onMouseEnter={(e) => {
                          e.target.style.transform = 'translateY(-2px)'
                          e.target.style.boxShadow = '0 6px 20px rgba(39, 174, 96, 0.4)'
                          e.target.style.background = 'linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)'
                        }}
                        onMouseLeave={(e) => {
                          e.target.style.transform = 'translateY(0)'
                          e.target.style.boxShadow = '0 4px 12px rgba(39, 174, 96, 0.3)'
                          e.target.style.background = 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)'
                        }}
                      >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                          <polyline points="7,10 12,15 17,10"/>
                          <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                        <span>NC PDF</span>
                      </button>
                    </div>
                  ) : (t.status === 'PAID' && t.billingStatus === 'billed') ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'center' }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        color: '#27ae60',
                        backgroundColor: '#e8f5e8',
                        border: '1px solid #27ae6020',
                        display: 'inline-block',
                        minWidth: '80px'
                      }}>
                        ✅ Facturado
                      </span>
                      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', justifyContent: 'center' }}>
                        <button
                          onClick={(e) => handleDownloadInvoice(t.id, e)}
                          title="Descargar factura PDF"
                          style={{
                            background: 'linear-gradient(135deg, #3498db 0%, #2980b9 100%)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '8px',
                            padding: '8px 12px',
                            fontSize: '0.75rem',
                            fontWeight: '600',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            boxShadow: '0 4px 12px rgba(52, 152, 219, 0.3)',
                            transition: 'all 0.3s ease',
                            minWidth: '70px'
                          }}
                          onMouseEnter={(e) => {
                            e.target.style.transform = 'translateY(-2px)'
                            e.target.style.boxShadow = '0 6px 20px rgba(52, 152, 219, 0.4)'
                            e.target.style.background = 'linear-gradient(135deg, #2980b9 0%, #3498db 100%)'
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.transform = 'translateY(0)'
                            e.target.style.boxShadow = '0 4px 12px rgba(52, 152, 219, 0.3)'
                            e.target.style.background = 'linear-gradient(135deg, #3498db 0%, #2980b9 100%)'
                          }}
                        >
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7,10 12,15 17,10"/>
                            <line x1="12" y1="15" x2="12" y2="3"/>
                          </svg>
                          <span>PDF</span>
                        </button>
                        <button
                          onClick={(e) => handleProcessRefund(t.id, e)}
                          title="Procesar reembolso"
                          style={{
                            background: 'linear-gradient(135deg, #e67e22 0%, #d35400 100%)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '8px',
                            padding: '8px 12px',
                            fontSize: '0.75rem',
                            fontWeight: '600',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            boxShadow: '0 4px 12px rgba(230, 126, 34, 0.3)',
                            transition: 'all 0.3s ease',
                            minWidth: '85px'
                          }}
                          onMouseEnter={(e) => {
                            e.target.style.transform = 'translateY(-2px)'
                            e.target.style.boxShadow = '0 6px 20px rgba(230, 126, 34, 0.4)'
                            e.target.style.background = 'linear-gradient(135deg, #d35400 0%, #e67e22 100%)'
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.transform = 'translateY(0)'
                            e.target.style.boxShadow = '0 4px 12px rgba(230, 126, 34, 0.3)'
                            e.target.style.background = 'linear-gradient(135deg, #e67e22 0%, #d35400 100%)'
                          }}
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
                            <path d="M21 3v5h-5"/>
                            <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
                            <path d="M3 21v-5h5"/>
                          </svg>
                          <span>Reemb.</span>
                        </button>
                      </div>
                    </div>
                  ) : t.billingStatus === 'error' ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'center' }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        color: '#e67e22',
                        backgroundColor: '#fef5e7',
                        border: '1px solid #e67e2220',
                        display: 'inline-block',
                        minWidth: '80px'
                      }}>
                        ⚠️ Error
                      </span>
                    </div>
                  ) : t.status === 'PAID' && (t.billingStatus === 'pending' || !t.billingStatus) ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'center' }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        color: '#f39c12',
                        backgroundColor: '#fef9e7',
                        border: '1px solid #f39c1220',
                        display: 'inline-block',
                        minWidth: '80px'
                      }}>
                        ⏳ Pendiente
                      </span>
                      <button
                        onClick={(e) => handleBillingConfirmation(t.id, e)}
                        title="Confirmar facturación - Generar factura"
                        style={{
                          background: 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)',
                          color: 'white',
                          border: 'none',
                          borderRadius: '8px',
                          padding: '8px 14px',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '0.75rem',
                          fontWeight: '600',
                          boxShadow: '0 4px 12px rgba(39, 174, 96, 0.3)',
                          transition: 'all 0.3s ease',
                          gap: '6px',
                          minWidth: '90px'
                        }}
                        onMouseEnter={(e) => {
                          e.target.style.transform = 'translateY(-2px)'
                          e.target.style.boxShadow = '0 6px 20px rgba(39, 174, 96, 0.4)'
                          e.target.style.background = 'linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)'
                        }}
                        onMouseLeave={(e) => {
                          e.target.style.transform = 'translateY(0)'
                          e.target.style.boxShadow = '0 4px 12px rgba(39, 174, 96, 0.3)'
                          e.target.style.background = 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)'
                        }}
                      >
                        ✓ Facturar
                      </button>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'center' }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: '12px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        color: '#95a5a6',
                        backgroundColor: '#f8f9fa',
                        border: '1px solid #95a5a620',
                        display: 'inline-block',
                        minWidth: '80px'
                      }}>
                        ➖ N/A
                      </span>
                      <small style={{fontSize: '10px', opacity: 0.7, color: '#95a5a6'}}>
                        {t.status === 'AUTHORIZED' ? 'Solo PAID' : 'No disponible'}
                      </small>
                    </div>
                  )}
                </div>
              </td>
            </tr>
          ))}
          {transactions.length === 0 && !loading && (
            <tr>
              <td colSpan="7" style={{ 
                padding: '2rem', 
                textAlign: 'center',
                color: 'var(--text-secondary)'
              }}>
                {Object.values(filters).some(f => f) ? 'No se encontraron transacciones con los filtros aplicados.' : 'Sin datos aún. Envía un webhook de prueba a /api/webhooks/getnet.'}
              </td>
            </tr>
          )}
          {loading && (
            <tr>
              <td colSpan="7" style={{ 
                padding: '2rem', 
                textAlign: 'center',
                color: 'var(--text-secondary)'
              }}>
                Cargando transacciones...
              </td>
            </tr>
          )}
        </tbody>
      </table>
      </div>
      {toastMsg && <div className="toast">{toastMsg}</div>}

      {showNoPendingModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.45)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999
        }}>
          <div style={{
            background: 'white',
            color: '#111',
            borderRadius: '16px',
            padding: '24px',
            width: 'min(90vw, 420px)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.25)',
            textAlign: 'center'
          }}>
            <div style={{
              marginBottom: '12px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              fontWeight: 800,
              fontSize: '1.1rem'
            }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#111" strokeWidth="2.2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9 12l2 2 4-4"/>
              </svg>
              <strong style={{ color: '#111' }}>No hay transacciones pendientes</strong>
            </div>
            <div style={{ color: '#4b5563', marginBottom: '18px' }}>
              Ya no quedan facturas para confirmar.
            </div>
            <button
              onClick={() => setShowNoPendingModal(false)}
              style={{
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                border: 'none',
                borderRadius: '10px',
                padding: '10px 16px',
                cursor: 'pointer',
                fontWeight: 700
              }}
            >
              Entendido
            </button>
          </div>
        </div>
      )}
      
      {/* Paginación */}
      {totalPages > 1 && (
        <div className="pagination">
          <button
            onClick={() => setCurrentPage(0)}
            disabled={currentPage === 0}
            className="pagination-btn"
          >
            Primera
          </button>
          
          <button
            onClick={() => setCurrentPage(currentPage - 1)}
            disabled={currentPage === 0}
            className="pagination-btn"
          >
            Anterior
          </button>
          
          <span className="pagination-info">
            Página {currentPage + 1} de {totalPages}
          </span>
          
          <button
            onClick={() => setCurrentPage(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="pagination-btn"
          >
            Siguiente
          </button>
          
          <button
            onClick={() => setCurrentPage(totalPages - 1)}
            disabled={currentPage >= totalPages - 1}
            className="pagination-btn"
          >
            Última
          </button>
        </div>
      )}
    </div>
  )
}
