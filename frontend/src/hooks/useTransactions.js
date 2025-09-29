import { useEffect, useMemo, useState } from 'react'
import { API, HTTP } from '../api'
import { formatCurrency, translateStatus, getRelativeTime } from '../utils/formatters'

export default function useTransactions() {
  const [health, setHealth] = useState('ping...')
  const [transactions, setTransactions] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [loading, setLoading] = useState(false)

  const [pendingTransactions, setPendingTransactions] = useState([])
  const [pendingTotalPages, setPendingTotalPages] = useState(0)
  const [pendingCurrentPage, setPendingCurrentPage] = useState(0)
  const [pendingLoading, setPendingLoading] = useState(false)

  const [pendingConfirmationTransaction, setPendingConfirmationTransaction] = useState(null)
  const [selectedTransactionId, setSelectedTransactionId] = useState(null)
  const [currentView, setCurrentView] = useState('list')

  const [allPendingTransactions, setAllPendingTransactions] = useState([])
  const [currentPendingIndex, setCurrentPendingIndex] = useState(0)

  const [selectedTransactions, setSelectedTransactions] = useState(new Set())
  const [showBulkActions, setShowBulkActions] = useState(false)
  const [bulkProcessing, setBulkProcessing] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [toastMsg, setToastMsg] = useState('')
  const [showNoPendingModal, setShowNoPendingModal] = useState(false)

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
      if (filters.status) params.append('status', filters.status)
      if (filters.billingStatus) params.append('billingStatus', filters.billingStatus)
      if (filters.minAmount) params.append('minAmount', filters.minAmount)
      if (filters.maxAmount) params.append('maxAmount', filters.maxAmount)
      if (filters.startDate) {
        const startDate = new Date(filters.startDate)
        startDate.setHours(0, 0, 0, 0)
        params.append('startDate', startDate.toISOString())
      }
      if (filters.endDate) {
        const endDate = new Date(filters.endDate)
        endDate.setHours(23, 59, 59, 999)
        params.append('endDate', endDate.toISOString())
      }
      if (filters.search) params.append('search', filters.search)

      const url = `${API}/api/transactions/list-native?${params}`
      const response = await HTTP.get(url)
      const data = response.data
      setTransactions(data.content || [])
      setTotalPages(data.totalPages || 0)
      setTotalElements(data.totalElements || 0)
    } catch (error) {
      setTransactions([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    HTTP.get(`${API}/api/health`).then(r => setHealth(r.data.status)).catch(() => setHealth('down'))
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

  useEffect(() => {
    const interval = setInterval(() => {
      checkForPendingTransactions()
    }, 10000)
    checkForPendingTransactions()
    return () => clearInterval(interval)
  }, [currentView, pendingConfirmationTransaction])

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }))
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
      fetchPendingTransactions()
      fetchTransactions()
    } catch (error) {
      setToastMsg('Error al confirmar facturación')
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  const checkForPendingTransactions = async () => {
    try {
      const response = await HTTP.get(`${API}/api/transactions/pending-billing-confirmation?page=0&size=1`)
      const pending = response.data.content || []
      if (pending.length > 0) {
        // info only
      }
    } catch (error) {
      // ignore
    }
  }

  const goToConfirmationView = async () => {
    try {
      const response = await HTTP.get(`${API}/api/transactions/pending-billing-confirmation?page=0&size=50`)
      const pending = response.data.content || []
      if (pending.length > 0) {
        setAllPendingTransactions(pending)
        setCurrentPendingIndex(0)
        setPendingConfirmationTransaction(pending[0])
        setCurrentView('confirmation')
      } else {
        setShowNoPendingModal(true)
      }
    } catch (error) {
      setToastMsg('Error al buscar transacciones pendientes')
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  const handleConfirmationDecision = (decision, message) => {
    setToastMsg(message)
    setTimeout(() => setToastMsg(''), 4000)
    const updated = allPendingTransactions.filter(t => t.id !== pendingConfirmationTransaction.id)
    setAllPendingTransactions(updated)
    if (updated.length > 0) {
      const nextIndex = Math.min(currentPendingIndex, updated.length - 1)
      setCurrentPendingIndex(nextIndex)
      setPendingConfirmationTransaction(updated[nextIndex])
    } else {
      setPendingConfirmationTransaction(null)
      setCurrentView('list')
      setShowNoPendingModal(true)
    }
    fetchTransactions()
  }

  const goToNextPendingTransaction = () => {
    if (currentPendingIndex < allPendingTransactions.length - 1) {
      const nextIndex = currentPendingIndex + 1
      setCurrentPendingIndex(nextIndex)
      setPendingConfirmationTransaction(allPendingTransactions[nextIndex])
    }
  }

  const goToPreviousPendingTransaction = () => {
    if (currentPendingIndex > 0) {
      const prevIndex = currentPendingIndex - 1
      setCurrentPendingIndex(prevIndex)
      setPendingConfirmationTransaction(allPendingTransactions[prevIndex])
    }
  }

  const handleBillingConfirmation = async (transactionId, e) => {
    e?.stopPropagation?.()
    try {
      await HTTP.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
      setToastMsg('✅ Facturación confirmada exitosamente')
      setTimeout(() => setToastMsg(''), 3000)
      fetchTransactions()
      fetchPendingTransactions()
    } catch (error) {
      setToastMsg('❌ Error al procesar confirmación: ' + (error.response?.data?.message || error.message))
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  const handleBulkBilling = async () => {
    if (selectedTransactions.size === 0) return
    const confirmed = window.confirm(
      `¿Confirmar facturación de ${selectedTransactions.size} transacciones seleccionadas?\n\nEsto generará ${selectedTransactions.size} facturas automáticamente.`
    )
    if (!confirmed) return
    setBulkProcessing(true)
    let successCount = 0
    let errorCount = 0
    try {
      for (const transactionId of selectedTransactions) {
        try {
          await HTTP.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
          successCount++
        } catch (error) {
          errorCount++
        }
      }
      if (errorCount === 0) setToastMsg(`🎉 ¡${successCount} facturas generadas exitosamente!`)
      else setToastMsg(`⚠️ ${successCount} exitosas, ${errorCount} con errores`)
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

  const handleBulkReject = async () => {
    setToastMsg('Acción de rechazo masivo no implementada')
    setTimeout(() => setToastMsg(''), 3000)
  }

  const initializeBillingStatusSilently = async () => {
    try {
      await HTTP.post(`${API}/api/transactions/initialize-billing-status`)
    } catch (error) {
      // ignore
    }
  }

  const handleBackToList = () => {
    setCurrentView('list')
    setSelectedTransactionId(null)
  }

  const handleOpenSettings = () => setCurrentView('settings')
  const handleBackFromSettings = () => setCurrentView('list')

  const handleTransactionClick = (transactionId) => {
    setSelectedTransactionId(transactionId)
    setCurrentView('detail')
  }

  const handleDownloadInvoice = (transaction, e) => {
    e?.stopPropagation?.()
    // Preferir URL directa de Facturante si está disponible en la transacción
    if (transaction?.invoicePdfUrl) {
      window.open(transaction.invoicePdfUrl, '_blank')
      return
    }
    // Respaldo: endpoint del backend
    window.open(`${API}/api/invoices/pdf/${transaction?.id}`,'_blank')
  }

  const handleProcessRefund = async (transactionId, e) => {
    e?.stopPropagation?.()
    const refundReason = prompt('Ingrese el motivo del reembolso:', 'Reembolso solicitado por el cliente')
    if (!refundReason) return
    try {
      const response = await HTTP.post(`${API}/api/credit-notes/refund/${transactionId}?refundReason=${encodeURIComponent(refundReason)}`)
      if (response.status === 200) {
        setToastMsg('Reembolso procesado exitosamente')
        setTimeout(() => setToastMsg(''), 2000)
        fetchTransactions()
      }
    } catch (error) {
      setToastMsg('Error al procesar reembolso: ' + (error.response?.data?.message || error.message))
      setTimeout(() => setToastMsg(''), 3000)
    }
  }

  const handleDownloadCreditNote = (transaction, e) => {
    e?.stopPropagation?.()
    // Preferir URL directa de Facturante si está disponible en la transacción
    if (transaction?.creditNotePdfUrl) {
      window.open(transaction.creditNotePdfUrl, '_blank')
      return
    }
    // Respaldo: endpoint del backend
    window.open(`${API}/api/credit-notes/pdf/${transaction?.id}`,'_blank')
  }

  const pageSubtotal = useMemo(() => {
    if (!Array.isArray(transactions)) return 0
    return transactions.reduce((sum, t) => sum + Number(t.amount || 0), 0)
  }, [transactions])

  return {
    // state
    health,
    transactions,
    totalPages,
    totalElements,
    currentPage,
    setCurrentPage,
    pageSize,
    setPageSize,
    loading,
    filters,
    setFilters,
    searchInput,
    setSearchInput,
    currentView,
    setCurrentView,
    selectedTransactionId,
    setSelectedTransactionId,
    showBulkActions,
    setShowBulkActions,
    selectedTransactions,
    setSelectedTransactions,
    bulkProcessing,
    setBulkProcessing,
    toastMsg,
    setToastMsg,
    showNoPendingModal,
    setShowNoPendingModal,
    // pending
    pendingTransactions,
    pendingTotalPages,
    pendingCurrentPage,
    setPendingCurrentPage,
    pendingLoading,
    // values/formatters
    pageSubtotal,
    formatCurrency,
    translateStatus,
    getRelativeTime,
    // handlers
    handleFilterChange,
    clearFilters,
    handleTransactionClick,
    handleOpenSettings,
    handleBackFromSettings,
    handleBackToList,
    goToConfirmationView,
    handleBillingConfirmation,
    handleBulkBilling,
    handleBulkReject,
    handleDownloadInvoice,
    handleProcessRefund,
    handleDownloadCreditNote,
    // confirmation workflow
    pendingConfirmationTransaction,
    currentPendingIndex,
    allPendingTransactions,
    handleConfirmationDecision,
    goToNextPendingTransaction,
    goToPreviousPendingTransaction,
    confirmBilling
  }
}


