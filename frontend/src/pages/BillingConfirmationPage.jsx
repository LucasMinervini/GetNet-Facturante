import React, { useState } from 'react'
import axios from 'axios'

const API = import.meta.env.VITE_API_URL || 'http://localhost:1234'

export default function BillingConfirmationPage({ 
  transaction, 
  onDecision, 
  onBackToHome, 
  onNextTransaction, 
  onPreviousTransaction, 
  hasNext, 
  hasPrevious, 
  currentIndex, 
  totalTransactions 
}) {
  const [processing, setProcessing] = useState(false)
  const [action, setAction] = useState(null)
  const [success, setSuccess] = useState(false)

  const formatCurrency = (value, currency) => {
    try {
      const formatted = new Intl.NumberFormat('es-AR', {
        style: 'currency',
        currency: currency || 'ARS',
        minimumFractionDigits: 2
      }).format(Number(value))
      return formatted.replace(/^BRL\s*/, 'R$ ')
    } catch (e) {
      return `$ ${Number(value || 0).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    }
  }

  const handleConfirm = async () => {
    setProcessing(true)
    setAction('confirm')
    
    try {
      await axios.post(`${API}/api/transactions/${transaction.id}/confirm-billing`)
      // Mostrar éxito visualmente
      setSuccess(true)
      // Pequeño delay para mostrar que se procesó correctamente
      setTimeout(() => {
        onDecision('confirmed', '✅ Facturación confirmada exitosamente')
      }, 1500)
    } catch (error) {
      console.error('Error confirming billing:', error)
      setProcessing(false)
      setSuccess(false)
      onDecision('error', '❌ Error al confirmar facturación: ' + (error.response?.data?.message || error.message))
    }
  }

  return (
    <div className="billing-confirmation-page">
      {/* Contenido principal */}
      <div className="billing-confirmation-container">
        <div className="billing-confirmation-card">
          {/* Navegación entre transacciones */}
          {totalTransactions > 1 && (
            <div className="transaction-navigation">
              <button
                onClick={onPreviousTransaction}
                disabled={!hasPrevious}
                className={`nav-button ${!hasPrevious ? 'disabled' : ''}`}
              >
                ←
              </button>

              <div className="transaction-counter">
                {currentIndex} de {totalTransactions}
              </div>

              <button
                onClick={onNextTransaction}
                disabled={!hasNext}
                className={`nav-button ${!hasNext ? 'disabled' : ''}`}
              >
                →
              </button>
            </div>
          )}

          {/* Header */}
          <div className="billing-header">
            <div className="header-icon-modern">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                <path d="M2 17l10 5 10-5"/>
                <path d="M2 12l10 5 10-5"/>
              </svg>
            </div>
            
            <h1 className="billing-title">
              Transacción Pagada
            </h1>
            
            <p className="billing-subtitle">
              ¿Deseas facturar esta transacción?
            </p>
          </div>

          {/* Información de la transacción */}
          <div className="transaction-info">
            <div className="info-grid">
              <div className="info-card">
                <label className="info-label">
                  <span className="label-icon">💰</span> MONTO
                </label>
                <p className="amount-value">
                  {formatCurrency(transaction.amount, transaction.currency)}
                </p>
              </div>
              
              <div className="info-card">
                <label className="info-label">
                  <span className="label-icon">🆔</span> 
                  {(transaction.customerName && transaction.customerName !== 'Consumidor Final') || 
                   (transaction.externalId && transaction.externalId !== 'Consumidor Final') ? 'CUIT/CUIL' : 'DOCUMENTO'}
                </label>
                <p className="transaction-id">
                  {transaction.customerDoc ? 
                    (transaction.customerDoc.length === 8 ? 
                      transaction.customerDoc.replace(/(\d{2})(\d{3})(\d{3})/, '$1.$2.$3') :
                      transaction.customerDoc.length === 11 ?
                      transaction.customerDoc.replace(/(\d{2})(\d{8})(\d{1})/, '$1-$2-$3') :
                      transaction.customerDoc
                    ) : 
                    'N/A'
                  }
                </p>
              </div>
              
              <div className="info-card">
                <label className="info-label">
                  <span className="label-icon">🏢</span> CLIENTE
                </label>
                <p className="customer-name">
                  {transaction.customerName || transaction.externalId || 'Consumidor Final'}
                </p>
              </div>
              
              <div className="info-card">
                <label className="info-label">
                  <span className="label-icon">⏰</span> FECHA
                </label>
                <p className="date-info">
                  {new Date(transaction.createdAt).toLocaleString('es-AR')}
                </p>
              </div>
            </div>
          </div>

          {/* Botón Volver al Inicio */}
          <div className="action-buttons">
            <button
              onClick={() => onBackToHome && onBackToHome()}
              className="back-button"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9,22 9,12 15,12 15,22"/>
              </svg>
              Volver al Inicio
            </button>
          </div>

          {/* Botón de acción principal */}
          <div className="main-action">
            <button
              onClick={handleConfirm}
              disabled={processing}
              className={`confirm-button ${success && action === 'confirm' ? 'success' : ''} ${processing && action === 'confirm' ? 'processing' : ''}`}
            >
              {success && action === 'confirm' ? (
                <>
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5">
                    <path d="M20 6L9 17l-5-5"/>
                  </svg>
                  ¡Completado!
                </>
              ) : processing && action === 'confirm' ? (
                <>
                  <div className="loading-spinner"></div>
                  Procesando...
                </>
              ) : (
                <>
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5">
                    <path d="M20 6L9 17l-5-5"/>
                  </svg>
                  Sí, Facturar
                </>
              )}
            </button>
          </div>

          {/* Información adicional */}
          <div className="info-tip">
            <p className="tip-text">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              <span>
                <strong>Información:</strong> Si eliges "Sí, Facturar" se generará automáticamente la factura en Facturante.
              </span>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}