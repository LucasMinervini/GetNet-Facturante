import React, { useEffect, useState } from 'react'
import axios from 'axios'

const API = import.meta.env.VITE_API_URL || 'http://localhost:1234'

export default function TransactionDetail({ transactionId, onBack }) {
  const [transaction, setTransaction] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [resending, setResending] = useState(false)
  const [notification, setNotification] = useState(null)
  const [confirmingBilling, setConfirmingBilling] = useState(false)

  useEffect(() => {
    fetchTransactionDetail()
  }, [transactionId])

  const fetchTransactionDetail = async () => {
    try {
      setLoading(true)
      const response = await axios.get(`${API}/api/transactions/${transactionId}`)
      setTransaction(response.data)
    } catch (error) {
      console.error('Error fetching transaction detail:', error)
      setError('Error al cargar el detalle de la transacción')
    } finally {
      setLoading(false)
    }
  }

  const handleResendInvoice = async () => {
    try {
      setResending(true)
      await axios.post(`${API}/api/invoices/resend/${transactionId}`)
      setNotification({ type: 'success', message: 'Factura reenviada exitosamente' })
      setTimeout(() => setNotification(null), 3000)
    } catch (error) {
      console.error('Error resending invoice:', error)
      setNotification({ type: 'error', message: 'Error al reenviar la factura' })
      setTimeout(() => setNotification(null), 3000)
    } finally {
      setResending(false)
    }
  }

  const handleDownloadPdf = () => {
    window.open(`${API}/api/invoices/pdf/${transactionId}`, '_blank')
  }

  const handleSendNotification = (type) => {
    // Stub para notificaciones
    setNotification({ 
      type: 'info', 
      message: `Notificación por ${type} enviada (simulado)` 
    })
    setTimeout(() => setNotification(null), 3000)
  }

  const handleConfirmBilling = async () => {
    try {
      setConfirmingBilling(true)
      await axios.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
      setNotification({ 
        type: 'success', 
        message: 'Facturación confirmada y procesada exitosamente' 
      })
      setTimeout(() => setNotification(null), 3000)
      // Refrescar los datos de la transacción
      fetchTransactionDetail()
    } catch (error) {
      console.error('Error confirming billing:', error)
      setNotification({ 
        type: 'error', 
        message: 'Error al confirmar facturación: ' + (error.response?.data?.message || error.message)
      })
      setTimeout(() => setNotification(null), 3000)
    } finally {
      setConfirmingBilling(false)
    }
  }

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


  const getStatusBadgeStyle = (status) => {
    const baseStyle = {
      padding: '8px 16px',
      borderRadius: '20px',
      fontSize: '14px',
      fontWeight: 'bold',
      textTransform: 'uppercase',
      display: 'inline-block'
    }
    
    switch (status) {
      case 'PAID':
        return { ...baseStyle, backgroundColor: '#d4edda', color: '#155724' }
      case 'AUTHORIZED':
        return { ...baseStyle, backgroundColor: '#d1ecf1', color: '#0c5460' }
      case 'REFUNDED':
        return { ...baseStyle, backgroundColor: '#f8d7da', color: '#721c24' }
      case 'FAILED':
        return { ...baseStyle, backgroundColor: '#f5c6cb', color: '#721c24' }
      case 'PENDING_BILLING_CONFIRMATION':
        return { ...baseStyle, backgroundColor: '#fff3cd', color: '#856404' }
      case 'NO_BILLING_REQUIRED':
        return { ...baseStyle, backgroundColor: '#e2e3e5', color: '#383d41' }
      default:
        return { ...baseStyle, backgroundColor: '#e2e3e5', color: '#383d41' }
    }
  }

  const getInvoiceStatusBadgeStyle = (status) => {
    const baseStyle = {
      padding: '8px 16px',
      borderRadius: '20px',
      fontSize: '14px',
      fontWeight: 'bold',
      textTransform: 'uppercase',
      display: 'inline-block'
    }
    
    switch (status) {
      case 'sent':
        return { ...baseStyle, backgroundColor: '#d4edda', color: '#155724' }
      case 'pending':
        return { ...baseStyle, backgroundColor: '#fff3cd', color: '#856404' }
      case 'error':
        return { ...baseStyle, backgroundColor: '#f8d7da', color: '#721c24' }
      default:
        return { ...baseStyle, backgroundColor: '#e2e3e5', color: '#383d41' }
    }
  }

  if (loading) {
    return (
      <div style={{ 
        fontFamily: 'system-ui, sans-serif', 
        padding: 24,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '400px'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ 
            width: '40px', 
            height: '40px', 
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #007bff',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 16px'
          }}></div>
          <p>Cargando detalle de transacción...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ fontFamily: 'system-ui, sans-serif', padding: 24 }}>
        <div style={{
          backgroundColor: '#f8d7da',
          color: '#721c24',
          padding: '16px',
          borderRadius: '8px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
        <button
          onClick={onBack}
          style={{
            padding: '10px 20px',
            backgroundColor: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          ← Volver
        </button>
      </div>
    )
  }

  return (
    <div style={{ 
      fontFamily: 'Poppins, Inter, sans-serif', 
      padding: '2rem',
      minHeight: '100vh',
      background: `
        linear-gradient(135deg, 
          #1a202c 0%, 
          #2d3748 15%, 
          #4a5568 30%, 
          #2d3748 45%, 
          #1a202c 60%, 
          #2d3748 75%, 
          #1a202c 100%),
        linear-gradient(45deg, 
          rgba(102, 126, 234, 0.08) 0%, 
          rgba(118, 75, 162, 0.06) 25%, 
          rgba(240, 147, 251, 0.04) 50%, 
          rgba(245, 87, 108, 0.06) 75%, 
          rgba(79, 172, 254, 0.08) 100%)`,
      backgroundSize: '400% 400%, 200% 200%',
      animation: 'gradientShift 20s ease-in-out infinite',
      color: '#f7fafc',
      position: 'relative'
    }}>
      {/* Header moderno */}
      <div className="header" style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #f5576c 75%, #4facfe 100%)',
        borderRadius: '24px',
        padding: '2rem',
        marginBottom: '2rem',
        boxShadow: '0 25px 50px rgba(0, 0, 0, 0.5), 0 15px 35px rgba(102, 126, 234, 0.2)',
        position: 'relative',
        overflow: 'hidden',
        border: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <div style={{
          position: 'absolute',
          top: '2rem',
          left: '2rem',
          zIndex: 10
        }}>
          <button
            onClick={onBack}
            style={{
              padding: '0.75rem 1.5rem',
              background: 'rgba(255,255,255,0.15)',
              color: 'white',
              border: '1px solid rgba(255,255,255,0.2)',
              borderRadius: '12px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              backdropFilter: 'blur(10px)',
              transition: 'all 0.3s ease',
              fontSize: '0.9rem',
              fontWeight: '600'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = 'rgba(255,255,255,0.25)'
              e.target.style.transform = 'translateY(-2px)'
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'rgba(255,255,255,0.15)'
              e.target.style.transform = 'translateY(0)'
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            Volver
          </button>
        </div>
        
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '180px',
          textAlign: 'center'
        }}>
          <div>
            <h1 style={{
              fontSize: '3.5rem',
              fontWeight: '900',
              color: 'white',
              margin: '0 0 0.5rem 0',
              textShadow: '0 6px 12px rgba(0,0,0,0.4)',
              letterSpacing: '-0.03em',
              fontFamily: 'Inter, sans-serif'
            }}>
              Detalle de Transacción
            </h1>
            <h2 style={{
              fontSize: '1.5rem',
              color: 'rgba(255,255,255,0.9)',
              margin: 0,
              fontWeight: 300,
              textShadow: '0 3px 6px rgba(0,0,0,0.3)'
            }}>
              Información Completa
            </h2>
          </div>
        </div>
      </div>

      {/* Notification */}
      {notification && (
        <div style={{
          padding: '1rem 1.5rem',
          borderRadius: '16px',
          marginBottom: '1.5rem',
          background: notification.type === 'success' ? 
            'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.1) 100%)' : 
            notification.type === 'error' ? 
            'linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(220, 38, 38, 0.1) 100%)' : 
            'linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(37, 99, 235, 0.1) 100%)',
          border: notification.type === 'success' ? 
            '2px solid rgba(16, 185, 129, 0.3)' : 
            notification.type === 'error' ? 
            '2px solid rgba(239, 68, 68, 0.3)' : 
            '2px solid rgba(59, 130, 246, 0.3)',
          color: notification.type === 'success' ? '#10b981' : 
                 notification.type === 'error' ? '#ef4444' : '#3b82f6',
          fontWeight: '600',
          fontSize: '0.95rem',
          boxShadow: '0 8px 25px rgba(0, 0, 0, 0.2)',
          backdropFilter: 'blur(10px)'
        }}>
          {notification.message}
        </div>
      )}

      {/* Transaction Info Card */}
      <div style={{
        background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.98) 25%, rgba(51, 65, 85, 0.95) 50%, rgba(30, 41, 59, 0.98) 75%, rgba(15, 23, 42, 0.95) 100%)',
        borderRadius: '20px',
        boxShadow: '0 25px 50px rgba(0, 0, 0, 0.5), 0 15px 35px rgba(102, 126, 234, 0.2), 0 5px 15px rgba(0, 0, 0, 0.4)',
        border: '2px solid rgba(102, 126, 234, 0.4)',
        padding: '2rem',
        marginBottom: '2rem',
        backdropFilter: 'blur(25px)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <h2 style={{
          fontSize: '1.8rem',
          color: '#ffffff',
          marginBottom: '2rem',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontWeight: '700',
          textShadow: '0 2px 4px rgba(0, 0, 0, 0.7)',
          fontFamily: 'Inter, sans-serif'
        }}>
          <div style={{
            background: 'rgba(102, 126, 234, 0.2)',
            padding: '8px',
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: '#667eea' }}>
              <rect x="2" y="4" width="20" height="16" rx="2"/>
              <path d="M2 10h20"/>
            </svg>
          </div>
          Información de la Transacción
        </h2>
        
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: '20px'
        }}>
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>ID</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '0.75rem', 
              fontFamily: 'monospace', 
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '8px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.2)'
            }}>
              {transaction.id}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>ID Externo</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '1rem', 
              fontWeight: '600',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.2)'
            }}>
              {transaction.externalId}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>Estado</label>
            <div style={{ margin: '8px 0 16px' }}>
              <span style={{
                ...getStatusBadgeStyle(transaction.status),
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
                border: '1px solid rgba(255, 255, 255, 0.1)'
              }}>
                {transaction.status}
              </span>
            </div>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>Monto</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '1.25rem', 
              fontWeight: '700', 
              color: '#10b981',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(16, 185, 129, 0.3)',
              textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)'
            }}>
              {formatCurrency(transaction.amount, transaction.currency)}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>Cliente</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '1rem',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.2)'
            }}>
              {transaction.customerName || 'N/A'}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>Documento</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '1rem',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.2)'
            }}>
              {transaction.customerDoc || 'N/A'}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>Fecha de Creación</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '1rem',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.2)'
            }}>
              {new Date(transaction.createdAt).toLocaleString('es-AR')}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
              display: 'block'
            }}>Última Actualización</label>
            <p style={{ 
              margin: '8px 0 16px', 
              fontSize: '1rem',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.6)',
              padding: '12px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.2)'
            }}>
              {new Date(transaction.updatedAt).toLocaleString('es-AR')}
            </p>
          </div>
        </div>
      </div>

      {/* Billing Confirmation Card - Solo para transacciones pendientes */}
      {transaction.status === 'PENDING_BILLING_CONFIRMATION' && (
        <div style={{
          background: 'linear-gradient(145deg, rgba(245, 158, 11, 0.15) 0%, rgba(217, 119, 6, 0.1) 100%)',
          border: '2px solid rgba(245, 158, 11, 0.4)',
          borderRadius: '20px',
          boxShadow: '0 25px 50px rgba(0, 0, 0, 0.3), 0 15px 35px rgba(245, 158, 11, 0.2)',
          padding: '2rem',
          marginBottom: '2rem',
          position: 'relative',
          overflow: 'hidden',
          backdropFilter: 'blur(25px)'
        }}>
          {/* Decoración de fondo */}
          <div style={{
            position: 'absolute',
            top: '-20px',
            right: '-20px',
            width: '100px',
            height: '100px',
            background: 'linear-gradient(135deg, rgba(243, 156, 18, 0.1) 0%, rgba(243, 156, 18, 0.05) 100%)',
            borderRadius: '50%',
            zIndex: 0
          }}></div>
          
          <div style={{ position: 'relative', zIndex: 1 }}>
            <h2 style={{
              fontSize: '1.6rem',
              color: '#d68910',
              marginBottom: '15px',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              fontWeight: '700'
            }}>
              <div style={{
                backgroundColor: '#f39c12',
                borderRadius: '50%',
                padding: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5">
                  <path d="M12 9v4"/>
                  <path d="M12 17h.01"/>
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
                </svg>
              </div>
              ⚡ Confirmación de Facturación Requerida
            </h2>
            
            <p style={{
              color: '#b7950b',
              marginBottom: '25px',
              fontSize: '16px',
              lineHeight: '1.6',
              fontWeight: '500'
            }}>
              Esta transacción está esperando tu decisión. ¿Deseas generar una factura para esta transacción o marcarla como no facturable?
            </p>
            
            <div style={{
              display: 'flex',
              gap: '20px',
              flexWrap: 'wrap',
              alignItems: 'center'
            }}>
              <button
                onClick={handleConfirmBilling}
                disabled={confirmingBilling}
                style={{
                  padding: '16px 32px',
                  background: confirmingBilling ? '#95a5a6' : 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: confirmingBilling ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  fontSize: '16px',
                  fontWeight: '600',
                  boxShadow: confirmingBilling ? 'none' : '0 4px 12px rgba(46, 204, 113, 0.3)',
                  transition: 'all 0.3s ease',
                  transform: confirmingBilling ? 'none' : 'translateY(0)',
                  minWidth: '180px',
                  justifyContent: 'center'
                }}
                onMouseEnter={(e) => {
                  if (!confirmingBilling) {
                    e.target.style.transform = 'translateY(-2px)'
                    e.target.style.boxShadow = '0 6px 20px rgba(46, 204, 113, 0.4)'
                  }
                }}
                onMouseLeave={(e) => {
                  if (!confirmingBilling) {
                    e.target.style.transform = 'translateY(0)'
                    e.target.style.boxShadow = '0 4px 12px rgba(46, 204, 113, 0.3)'
                  }
                }}
              >
                {confirmingBilling ? (
                  <>
                    <div style={{
                      width: '20px',
                      height: '20px',
                      border: '2px solid white',
                      borderTop: '2px solid transparent',
                      borderRadius: '50%',
                      animation: 'spin 1s linear infinite'
                    }}></div>
                    Procesando...
                  </>
                ) : (
                  <>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <path d="M20 6L9 17l-5-5"/>
                    </svg>
                    ✅ Sí, Facturar
                  </>
                )}
              </button>
            </div>
            
            <div style={{
              marginTop: '20px',
              padding: '15px',
              backgroundColor: 'rgba(243, 156, 18, 0.1)',
              borderRadius: '8px',
              border: '1px solid rgba(243, 156, 18, 0.2)'
            }}>
              <p style={{
                margin: 0,
                fontSize: '14px',
                color: '#b7950b',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
                <strong>Tip:</strong> Si confirmas la facturación, se generará automáticamente la factura en Facturante.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Invoice Info Card */}
      <div style={{
        background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.98) 25%, rgba(51, 65, 85, 0.95) 50%, rgba(30, 41, 59, 0.98) 75%, rgba(15, 23, 42, 0.95) 100%)',
        borderRadius: '20px',
        boxShadow: '0 25px 50px rgba(0, 0, 0, 0.5), 0 15px 35px rgba(102, 126, 234, 0.2), 0 5px 15px rgba(0, 0, 0, 0.4)',
        border: '2px solid rgba(102, 126, 234, 0.4)',
        padding: '2rem',
        marginBottom: '2rem',
        backdropFilter: 'blur(25px)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <h2 style={{
          fontSize: '1.8rem',
          color: '#ffffff',
          marginBottom: '2rem',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontWeight: '700',
          textShadow: '0 2px 4px rgba(0, 0, 0, 0.7)',
          fontFamily: 'Inter, sans-serif'
        }}>
          <div style={{
            background: 'rgba(16, 185, 129, 0.2)',
            padding: '8px',
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: '#10b981' }}>
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14,2 14,8 20,8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10,9 9,9 8,9"/>
            </svg>
          </div>
          Estado de Factura
        </h2>
        
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '20px',
          marginBottom: '20px'
        }}>
          <div>
            <label style={{ fontWeight: 'bold', color: '#6c757d', fontSize: '14px' }}>Estado</label>
            <div style={{ margin: '5px 0' }}>
              <span style={getInvoiceStatusBadgeStyle(transaction.invoiceStatus)}>
                {transaction.invoiceStatus}
              </span>
            </div>
          </div>
        </div>
        
        {/* Action Buttons */}
        <div style={{
          display: 'flex',
          gap: '15px',
          flexWrap: 'wrap'
        }}>
          {transaction.status === 'PAID' && (
            <>
              <button
                onClick={handleResendInvoice}
                disabled={resending}
                style={{
                  padding: '12px 24px',
                  backgroundColor: resending ? '#6c757d' : '#007bff',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: resending ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: '14px',
                  fontWeight: '500'
                }}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                {resending ? 'Reenviando...' : 'Reemitir Factura'}
              </button>
              
              {transaction.pdfUrl && (
                <button
                  onClick={handleDownloadPdf}
                  style={{
                    padding: '12px 24px',
                    backgroundColor: '#28a745',
                    color: 'white',
                    border: 'none',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '14px',
                    fontWeight: '500'
                  }}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7,10 12,15 17,10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  Descargar PDF
                </button>
              )}
            </>
          )}
        </div>
      </div>

      {/* Notifications Card */}
      <div style={{
        background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.98) 25%, rgba(51, 65, 85, 0.95) 50%, rgba(30, 41, 59, 0.98) 75%, rgba(15, 23, 42, 0.95) 100%)',
        borderRadius: '20px',
        boxShadow: '0 25px 50px rgba(0, 0, 0, 0.5), 0 15px 35px rgba(102, 126, 234, 0.2), 0 5px 15px rgba(0, 0, 0, 0.4)',
        border: '2px solid rgba(102, 126, 234, 0.4)',
        padding: '2rem',
        backdropFilter: 'blur(25px)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <h2 style={{
          fontSize: '1.8rem',
          color: '#ffffff',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontWeight: '700',
          textShadow: '0 2px 4px rgba(0, 0, 0, 0.7)',
          fontFamily: 'Inter, sans-serif'
        }}>
          <div style={{
            background: 'rgba(245, 158, 11, 0.2)',
            padding: '8px',
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: '#f59e0b' }}>
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          Notificaciones
        </h2>
        
        <p style={{ 
          color: '#cbd5e1', 
          marginBottom: '1.5rem',
          fontSize: '1rem',
          lineHeight: '1.6'
        }}>
          Enviar notificaciones al cliente sobre el estado de la factura
        </p>
        
        <div style={{
          display: 'flex',
          gap: '15px',
          flexWrap: 'wrap'
        }}>
          <button
            onClick={() => handleSendNotification('Email')}
            style={{
              padding: '1rem 1.5rem',
              background: 'linear-gradient(135deg, #dc2626 0%, #991b1b 100%)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: '12px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              fontSize: '0.9rem',
              fontWeight: '600',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 12px rgba(220, 38, 38, 0.3)',
              backdropFilter: 'blur(10px)'
            }}
            onMouseEnter={(e) => {
              e.target.style.transform = 'translateY(-2px)'
              e.target.style.boxShadow = '0 8px 20px rgba(220, 38, 38, 0.4)'
            }}
            onMouseLeave={(e) => {
              e.target.style.transform = 'translateY(0)'
              e.target.style.boxShadow = '0 4px 12px rgba(220, 38, 38, 0.3)'
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            Notificar por Email
          </button>
          
          <button
            onClick={() => handleSendNotification('WhatsApp')}
            style={{
              padding: '1rem 1.5rem',
              background: 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: '12px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              fontSize: '0.9rem',
              fontWeight: '600',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 12px rgba(22, 163, 74, 0.3)',
              backdropFilter: 'blur(10px)'
            }}
            onMouseEnter={(e) => {
              e.target.style.transform = 'translateY(-2px)'
              e.target.style.boxShadow = '0 8px 20px rgba(22, 163, 74, 0.4)'
            }}
            onMouseLeave={(e) => {
              e.target.style.transform = 'translateY(0)'
              e.target.style.boxShadow = '0 4px 12px rgba(22, 163, 74, 0.3)'
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
            </svg>
            Notificar por WhatsApp
          </button>
        </div>
      </div>
      
      <style>
        {`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
      </style>
    </div>
  )
}