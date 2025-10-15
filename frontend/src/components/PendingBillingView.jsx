import React from 'react'

export default function PendingBillingView({
  toastMsg,
  pendingLoading,
  pendingTransactions,
  formatCurrency,
  confirmBilling,
  pendingTotalPages,
  pendingCurrentPage,
  setPendingCurrentPage,
  showNoPendingModal,
  setCurrentView,
  setShowNoPendingModal
}) {
  return (
    <div style={{ padding: '2rem', minHeight: '100vh' }}>
      <div className="header">
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

      {toastMsg && (
        <div className="toast-notification">{toastMsg}</div>
      )}

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
                        <span className="status-badge status-pending-billing">Pendiente Confirmación</span>
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


