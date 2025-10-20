import React, { useEffect, useState } from 'react'
import useTransactions from '../hooks/useTransactions'
import TransactionDetail from './TransactionDetail'
import BillingSettings from './BillingSettings'
import BillingConfirmationPage from './BillingConfirmationPage'
import Login from './Login'
import Registration from './Registration'
import Dashboard from '../components/Dashboard'
import Reports from '../components/Reports'
import MainHeader from '../components/MainHeader'
import StatsGrid from '../components/StatsGrid'
import TransactionFilters from '../components/TransactionFilters'
import BulkActionsBar from '../components/BulkActionsBar'
import TransactionsTable from '../components/TransactionsTable'
import Pagination from '../components/Pagination'
import PendingBillingView from '../components/PendingBillingView'
import ResultsInfo from '../components/ResultsInfo'
import Toast from '../components/Toast'
import SimpleModal from '../components/SimpleModal'


export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('accessToken'))
  const [route, setRoute] = useState(() => (typeof window !== 'undefined' ? window.location.pathname : '/'))
  const [theme, setTheme] = useState(() => {
    try {
      const saved = localStorage.getItem('theme')
      return saved === 'light' || saved === 'dark' ? saved : 'dark'
    } catch {
      return 'dark'
    }
  })

  React.useEffect(() => {
    const onPop = () => setRoute(window.location.pathname)
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  // Aplicar tema al atributo data-theme del documento y persistir
  React.useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', theme)
    }
    try { localStorage.setItem('theme', theme) } catch {}
  }, [theme])

  const toggleTheme = () => setTheme(prev => (prev === 'dark' ? 'light' : 'dark'))

  const navigate = (path) => {
    // Guard de producción: si no hay token y se intenta ir a rutas protegidas, enviar a login
    const token = localStorage.getItem('accessToken')
    const protectedRoutes = ['/dashboard', '/reports']
    if (!token && protectedRoutes.includes(path)) {
      path = '/'
    }
    if (window.location.pathname !== path) {
      window.history.pushState({}, '', path)
      setRoute(path)
    }
  }

  if (!isAuthenticated) {
    if (route === '/register') {
      return <Registration onSuccess={() => navigate('/billing-settings')} />
    }
    return <Login />
  }

  // Delegar a un sub-componente autenticado para mantener el orden de hooks
  return <AuthenticatedApp route={route} navigate={navigate} theme={theme} onToggleTheme={toggleTheme} />
}

function AuthenticatedApp({ route, navigate, theme, onToggleTheme }) {
  const {
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
  } = useTransactions()

  // Renderizar Dashboard si está en la ruta /dashboard
  if (route === '/dashboard') {
    return (
      <div key="route-dashboard">
        <MainHeader onNavigate={navigate} currentRoute={route} theme={theme} onToggleTheme={onToggleTheme} />
        <Dashboard key="dashboard-view" />
      </div>
    )
  }

  // Renderizar Reportes si está en la ruta /reports
  if (route === '/reports') {
    return (
      <div key="route-reports">
        <MainHeader onNavigate={navigate} currentRoute={route} theme={theme} onToggleTheme={onToggleTheme} />
        <Reports key="reports-view" />
      </div>
    )
  }

  



  // La inicialización, fetch y polling ahora viven en useTransactions

  

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
      <PendingBillingView
        toastMsg={toastMsg}
        pendingLoading={pendingLoading}
        pendingTransactions={pendingTransactions}
        formatCurrency={formatCurrency}
        confirmBilling={confirmBilling}
        pendingTotalPages={pendingTotalPages}
        pendingCurrentPage={pendingCurrentPage}
        setPendingCurrentPage={setPendingCurrentPage}
        showNoPendingModal={showNoPendingModal}
        setCurrentView={setCurrentView}
        setShowNoPendingModal={setShowNoPendingModal}
      />
    )
  }

  return (
    <div style={{ padding: '2rem', minHeight: '100vh' }}>
      {/* Header moderno con navegación */}
      <MainHeader 
        onNavigate={navigate} 
        currentRoute={route} 
        theme={theme}
        onToggleTheme={onToggleTheme} 
        onOpenSettings={handleOpenSettings} 
      />

      {/* Controls for saved filters */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
        <button className="btn btn-secondary" aria-label="Guardar filtros" onClick={() => {
          try { localStorage.setItem('tx_filters', JSON.stringify(filters)); setToastMsg('Filtros guardados'); } catch {}
        }}>Guardar filtros</button>
        <button className="btn btn-secondary" aria-label="Cargar filtros" onClick={() => {
          try { const saved = JSON.parse(localStorage.getItem('tx_filters')||'{}'); setFilters(prev => ({...prev, ...saved})); setCurrentPage(0); setToastMsg('Filtros cargados'); } catch {}
        }}>Cargar filtros</button>
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
                list="tx-suggestions"
              />
              <datalist id="tx-suggestions">
                {transactions.slice(0, 10).map(t => (
                  <option key={t.id} value={t.externalId || ''} />
                ))}
              </datalist>
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
      <ResultsInfo count={transactions.length} total={totalElements} loading={loading} />

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
                        onClick={(e) => handleDownloadCreditNote(t, e)}
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
                          onClick={(e) => handleDownloadInvoice(t, e)}
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
      <Toast message={toastMsg} type={/error/i.test(String(toastMsg||'')) ? 'error' : 'info'} onClose={() => setToastMsg('')} />

      <SimpleModal
        open={showNoPendingModal}
        title="No hay transacciones pendientes"
        onClose={() => setShowNoPendingModal(false)}
      >
        Ya no quedan facturas para confirmar.
      </SimpleModal>
      
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
