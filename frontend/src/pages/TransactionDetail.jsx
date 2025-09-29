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
      // Debug: mostrar el estado de la transacción
      console.log('Transaction status:', response.data.status)
      console.log('Invoice status:', response.data.invoiceStatus)
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

  const handleDownloadCreditNote = () => {
    window.open(`${API}/api/credit-notes/pdf/${transactionId}`, '_blank')
  }

  const handleProcessRefund = async () => {
    const refundReason = prompt('Ingrese el motivo del reembolso:', 'Reembolso solicitado por el cliente')
    if (!refundReason) return
    
    try {
      setResending(true)
      const response = await axios.post(`${API}/api/credit-notes/refund/${transactionId}?refundReason=${encodeURIComponent(refundReason)}`)
      
      if (response.status === 200) {
        setNotification({ type: 'success', message: 'Reembolso procesado exitosamente' })
        setTimeout(() => setNotification(null), 3000)
        // Refrescar los datos de la transacción
        fetchTransactionDetail()
      }
    } catch (error) {
      console.error('Error processing refund:', error)
      setNotification({ type: 'error', message: 'Error al procesar el reembolso' })
      setTimeout(() => setNotification(null), 3000)
    } finally {
      setResending(false)
    }
  }

  const handleSendNotification = (type) => {
    // Stub para notificaciones
    setNotification({ 
      type: 'info', 
      message: `Notificación por ${type} enviada (simulado)` 
    })
    setTimeout(() => setNotification(null), 3000)
  }

  const confirmBilling = async (transactionId) => {
    try {
      await axios.post(`${API}/api/transactions/${transactionId}/confirm-billing`)
      setNotification({ 
        type: 'success', 
        message: 'Facturación confirmada exitosamente' 
      })
      setTimeout(() => setNotification(null), 3000)
      // Refrescar los datos de la transacción
      fetchTransactionDetail()
    } catch (error) {
      console.error('Error confirming billing:', error)
      setNotification({ 
        type: 'error', 
        message: 'Error al confirmar facturación' 
      })
      setTimeout(() => setNotification(null), 3000)
    }
  }

  const handleConfirmBilling = async () => {
    try {
      setConfirmingBilling(true)
      await confirmBilling(transactionId)
    } catch (error) {
      console.error('Error in handleConfirmBilling:', error)
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

  // Función para detectar si es empresa basado en el CUIT/CUIL
  const isEmpresa = (doc) => {
    if (!doc) return false
    // CUIT de empresa: formato XX-XXXXXXXX-X (donde XX es 20, 23, 24, 25, 26, 27, 30, 33, 34)
    const cuitPattern = /^(20|23|24|25|26|27|30|33|34)-\d{8}-\d$/
    return cuitPattern.test(doc)
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

  // Función para traducir el estado de la transacción
  const translateStatus = (status) => {
    const statusMap = {
      'AUTHORIZED': 'Autorizada',
      'PAID': 'Pagado',
      'REFUNDED': 'Reembolsado',
      'FAILED': 'Falló',
      'PENDING_BILLING_CONFIRMATION': 'Pendiente Confirmación'
    }
    return statusMap[status] || status
  }

  // Función para traducir el estado de la factura
  const translateInvoiceStatus = (status) => {
    const statusMap = {
      'sent': 'Enviada',
      'pending': 'Pendiente',
      'error': 'Error',
      'processing': 'Procesando'
    }
    return statusMap[status] || status
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
      fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif', 
      padding: '1.5rem',
      minHeight: '100vh',
      background: `
        linear-gradient(135deg, 
          #0f172a 0%, 
          #1e293b 25%, 
          #334155 50%, 
          #1e293b 75%, 
          #0f172a 100%),
        radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.1) 0%, transparent 50%),
        radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.1) 0%, transparent 50%),
        radial-gradient(circle at 40% 40%, rgba(120, 219, 255, 0.1) 0%, transparent 50%)`,
      backgroundSize: '400% 400%, 100% 100%, 100% 100%, 100% 100%',
      animation: 'gradientShift 25s ease-in-out infinite',
      color: '#f8fafc',
      position: 'relative',
      overflow: 'hidden'
    }}>
      {/* Header moderno con efectos avanzados */}
      <div className="header" style={{
        background: `
          linear-gradient(135deg, 
            #667eea 0%, 
            #764ba2 15%, 
            #f093fb 30%, 
            #f5576c 45%, 
            #4facfe 60%, 
            #667eea 75%, 
            #764ba2 100%),
          radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
          radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.3) 0%, transparent 50%),
          radial-gradient(circle at 40% 40%, rgba(120, 219, 255, 0.2) 0%, transparent 50%)`,
        backgroundSize: '400% 400%, 100% 100%, 100% 100%, 100% 100%',
        animation: 'gradientShift 8s ease-in-out infinite',
        borderRadius: '24px',
        padding: '2rem',
        marginBottom: '2rem',
        boxShadow: `
          0 25px 50px rgba(0, 0, 0, 0.4), 
          0 15px 35px rgba(102, 126, 234, 0.2), 
          0 5px 15px rgba(0, 0, 0, 0.3),
          inset 0 1px 0 rgba(255, 255, 255, 0.1)`,
        position: 'relative',
        overflow: 'hidden',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        backdropFilter: 'blur(20px)'
      }}>
        {/* Efectos de fondo decorativos */}
        <div style={{
          position: 'absolute',
          top: '-50%',
          right: '-20%',
          width: '200px',
          height: '200px',
          background: 'radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%)',
          borderRadius: '50%',
          animation: 'float 6s ease-in-out infinite'
        }}></div>
        <div style={{
          position: 'absolute',
          bottom: '-30%',
          left: '-10%',
          width: '150px',
          height: '150px',
          background: 'radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%)',
          borderRadius: '50%',
          animation: 'float 8s ease-in-out infinite reverse'
        }}></div>
        
        {/* Botón Volver */}
        <div style={{
          position: 'absolute',
          top: '1.5rem',
          left: '1.5rem',
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
              fontWeight: '600',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2)'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = 'rgba(255,255,255,0.25)'
              e.target.style.transform = 'translateY(-2px)'
              e.target.style.boxShadow = '0 6px 16px rgba(0, 0, 0, 0.3)'
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'rgba(255,255,255,0.15)'
              e.target.style.transform = 'translateY(0)'
              e.target.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.2)'
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            Volver
          </button>
        </div>
        
        {/* Contenido del header */}
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '180px',
          textAlign: 'center',
          position: 'relative',
          zIndex: 5
        }}>
          <div>
            <h1 style={{
              fontSize: '3.5rem',
              fontWeight: '900',
              color: 'white',
              margin: '0 0 0.5rem 0',
              textShadow: '0 6px 12px rgba(0,0,0,0.4), 0 0 30px rgba(231, 76, 60, 0.2), 0 0 20px rgba(243, 156, 18, 0.1)',
              letterSpacing: '-0.03em',
              fontFamily: 'Inter, sans-serif',
              background: 'linear-gradient(135deg, #ffffff 0%, #f8f9fa 50%, #e9ecef 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text'
            }}>
              Detalle de Transacción
            </h1>
            <h2 style={{
              fontSize: '1.5rem',
              color: 'rgba(255,255,255,0.9)',
              margin: '0 0 1rem 0',
              fontWeight: 300,
              textShadow: '0 3px 6px rgba(0,0,0,0.3)',
              textTransform: 'uppercase',
              letterSpacing: '0.1em'
            }}>
              Información Completa
            </h2>
            {/* Línea decorativa */}
            <div style={{
              width: '60px',
              height: '3px',
              background: 'linear-gradient(90deg, #ff6b6b 0%, #ffa500 50%, #ff6b6b 100%)',
              margin: '0 auto',
              borderRadius: '2px',
              boxShadow: '0 2px 4px rgba(255, 107, 107, 0.3)'
            }}></div>
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
        background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.9) 0%, rgba(30, 41, 59, 0.95) 25%, rgba(51, 65, 85, 0.9) 50%, rgba(30, 41, 59, 0.95) 75%, rgba(15, 23, 42, 0.9) 100%)',
        borderRadius: '16px',
        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4), 0 10px 25px rgba(102, 126, 234, 0.15), 0 4px 12px rgba(0, 0, 0, 0.3)',
        border: '1px solid rgba(102, 126, 234, 0.2)',
        padding: '1.5rem',
        marginBottom: '1.5rem',
        backdropFilter: 'blur(20px)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <h2 style={{
          fontSize: '1.5rem',
          color: '#ffffff',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontWeight: '600',
          textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)',
          fontFamily: 'Inter, sans-serif'
        }}>
          <div style={{
            background: 'rgba(102, 126, 234, 0.15)',
            padding: '6px',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: '#667eea' }}>
              <rect x="2" y="4" width="20" height="16" rx="2"/>
              <path d="M2 10h20"/>
            </svg>
          </div>
          Información de la Transacción
        </h2>
        
        {/* Información Principal - Destacada */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '24px',
          marginBottom: '24px',
          padding: '20px',
          background: 'rgba(102, 126, 234, 0.05)',
          borderRadius: '12px',
          border: '1px solid rgba(102, 126, 234, 0.1)'
        }}>
          {/* Monto - Destacado */}
          <div style={{
            textAlign: 'center',
            padding: '16px',
            background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.05) 100%)',
            borderRadius: '12px',
            border: '1px solid rgba(16, 185, 129, 0.2)'
          }}>
            <label style={{ 
              fontWeight: '600', 
              color: '#10b981', 
              fontSize: '0.8rem',
              textTransform: 'uppercase',
              letterSpacing: '0.1em',
              marginBottom: '8px',
              display: 'block'
            }}>Monto</label>
            <p style={{ 
              margin: '0', 
              fontSize: '1.8rem', 
              fontWeight: '800', 
              color: '#10b981',
              textShadow: '0 2px 4px rgba(0, 0, 0, 0.3)'
            }}>
              {formatCurrency(transaction.amount, transaction.currency)}
            </p>
          </div>

          {/* Estado - Destacado */}
          <div style={{
            textAlign: 'center',
            padding: '16px',
            background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(79, 172, 254, 0.05) 100%)',
            borderRadius: '12px',
            border: '1px solid rgba(102, 126, 234, 0.2)'
          }}>
            <label style={{ 
              fontWeight: '600', 
              color: '#667eea', 
              fontSize: '0.8rem',
              textTransform: 'uppercase',
              letterSpacing: '0.1em',
              marginBottom: '8px',
              display: 'block'
            }}>Estado</label>
            <div style={{ margin: '0' }}>
              <span style={{
                ...getStatusBadgeStyle(transaction.status),
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                fontSize: '0.9rem',
                padding: '8px 16px',
                display: 'inline-block'
              }}>
                {transaction.status}
              </span>
            </div>
          </div>
        </div>

        {/* Información Secundaria - Grid compacto */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
          gap: '16px'
        }}>
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.8rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '6px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              {isEmpresa(transaction.customerDoc) ? (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M3 21h18"/>
                    <path d="M5 21V7l8-4v18"/>
                    <path d="M19 21V11l-6-4"/>
                  </svg>
                  Empresa
                </>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  Cliente
                </>
              )}
            </label>
            <p style={{ 
              margin: '0', 
              fontSize: '0.95rem', 
              fontWeight: '500',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.4)',
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.1)',
              minHeight: '20px',
              display: 'flex',
              alignItems: 'center'
            }}>
              {transaction.customerName || transaction.externalId || 'N/A'}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.8rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '6px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              {isEmpresa(transaction.customerDoc) ? (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                    <line x1="8" y1="21" x2="16" y2="21"/>
                    <line x1="12" y1="17" x2="12" y2="21"/>
                  </svg>
                  CUIT/CUIL
                </>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                    <line x1="8" y1="21" x2="16" y2="21"/>
                    <line x1="12" y1="17" x2="12" y2="21"/>
                  </svg>
                  Documento
                </>
              )}
            </label>
            <p style={{ 
              margin: '0', 
              fontSize: '0.95rem',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.4)',
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.1)',
              minHeight: '20px',
              display: 'flex',
              alignItems: 'center'
            }}>
              {transaction.customerDoc || 'N/A'}
            </p>
          </div>
          
          <div>
            <label style={{ 
              fontWeight: '600', 
              color: '#cbd5e1', 
              fontSize: '0.8rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '6px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              Fecha de Facturación
            </label>
            <p style={{ 
              margin: '0', 
              fontSize: '0.95rem',
              color: '#f1f5f9',
              background: 'rgba(51, 65, 85, 0.4)',
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(102, 126, 234, 0.1)',
              minHeight: '20px',
              display: 'flex',
              alignItems: 'center'
            }}>
              {new Date(transaction.createdAt).toLocaleDateString('es-AR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
              })}
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

      {/* Estado de Facturación - Sincronizado con App.jsx */}
      {transaction.status !== 'PENDING_BILLING_CONFIRMATION' && (
        <div style={{
          background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.9) 0%, rgba(30, 41, 59, 0.95) 25%, rgba(51, 65, 85, 0.9) 50%, rgba(30, 41, 59, 0.95) 75%, rgba(15, 23, 42, 0.9) 100%)',
          borderRadius: '16px',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4), 0 10px 25px rgba(16, 185, 129, 0.15), 0 4px 12px rgba(0, 0, 0, 0.3)',
          border: '1px solid rgba(16, 185, 129, 0.2)',
          padding: '1.5rem',
          marginBottom: '1.5rem',
          backdropFilter: 'blur(20px)',
          position: 'relative',
          overflow: 'hidden'
        }}>
          <h2 style={{
            fontSize: '1.5rem',
            color: '#ffffff',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontWeight: '600',
            textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)',
            fontFamily: 'Inter, sans-serif'
          }}>
            <div style={{
              background: 'rgba(16, 185, 129, 0.15)',
              padding: '6px',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: '#10b981' }}>
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14,2 14,8 20,8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10,9 9,9 8,9"/>
              </svg>
            </div>
            Estado de Facturación
          </h2>
          
          {/* Lógica sincronizada con App.jsx */}
          {transaction.status === 'REFUNDED' ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
              alignItems: 'flex-start'
            }}>
              <div style={{
                padding: '12px 16px',
                backgroundColor: 'rgba(231, 76, 60, 0.1)',
                border: '1px solid rgba(231, 76, 60, 0.3)',
                borderRadius: '8px',
                color: '#e74c3c',
                fontSize: '0.9rem',
                fontWeight: '500',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7,10 12,15 17,10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                {transaction.refundReason || transaction.creditNoteReason || 'Reembolso aplicado'}
              </div>
              {transaction.creditNoteNumber && (
                <div style={{
                  display: 'flex',
                  gap: '12px',
                  flexWrap: 'wrap',
                  alignItems: 'center'
                }}>
                  <button
                    onClick={handleDownloadCreditNote}
                    title="Descargar nota de crédito PDF"
                    style={{
                      background: 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)',
                      color: '#111111',
                      border: 'none',
                      borderRadius: '8px',
                      padding: '10px 16px',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      fontSize: '0.85rem',
                      fontWeight: '600',
                      boxShadow: '0 4px 12px rgba(39, 174, 96, 0.3)',
                      transition: 'all 0.3s ease'
                    }}
                    onMouseEnter={(e) => {
                      e.target.style.transform = 'translateY(-2px)'
                      e.target.style.boxShadow = '0 6px 20px rgba(39, 174, 96, 0.4)'
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.transform = 'translateY(0)'
                      e.target.style.boxShadow = '0 4px 12px rgba(39, 174, 96, 0.3)'
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                      <polyline points="7,10 12,15 17,10"/>
                      <line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                    <span>NC PDF</span>
                  </button>
                </div>
              )}
            </div>
          ) : (transaction.status === 'PAID' && transaction.billingStatus === 'billed') ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
              alignItems: 'flex-start'
            }}>
              <div style={{
                padding: '12px 16px',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                border: '1px solid rgba(16, 185, 129, 0.3)',
                borderRadius: '8px',
                color: '#10b981',
                fontSize: '0.9rem',
                fontWeight: '500',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 6L9 17l-5-5"/>
                </svg>
                ✅ Facturado
              </div>
              <div style={{
                display: 'flex',
                gap: '12px',
                flexWrap: 'wrap',
                alignItems: 'center'
              }}>
                <button
                  onClick={handleDownloadPdf}
                  title="Descargar factura PDF"
                  style={{
                    background: 'linear-gradient(135deg, #3498db 0%, #2980b9 100%)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    padding: '10px 16px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '0.85rem',
                    fontWeight: '600',
                    boxShadow: '0 4px 12px rgba(52, 152, 219, 0.3)',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.transform = 'translateY(-2px)'
                    e.target.style.boxShadow = '0 6px 20px rgba(52, 152, 219, 0.4)'
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.transform = 'translateY(0)'
                    e.target.style.boxShadow = '0 4px 12px rgba(52, 152, 219, 0.3)'
                  }}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7,10 12,15 17,10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  <span>PDF</span>
                </button>
                <button
                  onClick={handleProcessRefund}
                  title="Procesar reembolso"
                  disabled={resending}
                  style={{
                    background: resending ? '#95a5a6' : 'linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    padding: '10px 16px',
                    cursor: resending ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '0.85rem',
                    fontWeight: '600',
                    boxShadow: resending ? 'none' : '0 4px 12px rgba(231, 76, 60, 0.3)',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => {
                    if (!resending) {
                      e.target.style.transform = 'translateY(-2px)'
                      e.target.style.boxShadow = '0 6px 20px rgba(231, 76, 60, 0.4)'
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!resending) {
                      e.target.style.transform = 'translateY(0)'
                      e.target.style.boxShadow = '0 4px 12px rgba(231, 76, 60, 0.3)'
                    }
                  }}
                >
                  {resending ? (
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
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                        <polyline points="7,10 12,15 17,10"/>
                        <line x1="12" y1="15" x2="12" y2="3"/>
                      </svg>
                      <span>Reembolso</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          ) : transaction.status === 'PAID' && (transaction.billingStatus === 'pending' || !transaction.billingStatus) ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
              alignItems: 'flex-start'
            }}>
              <div style={{
                padding: '12px 16px',
                backgroundColor: 'rgba(243, 156, 18, 0.1)',
                border: '1px solid rgba(243, 156, 18, 0.3)',
                borderRadius: '8px',
                color: '#f39c12',
                fontSize: '0.9rem',
                fontWeight: '500',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12,6 12,12 16,14"/>
                </svg>
                ⏳ Pendiente
              </div>
              <button
                onClick={() => {
                  // Simular la misma lógica que goToConfirmationView en App.jsx
                  const transactionData = {
                    id: transaction.id,
                    externalId: transaction.externalId,
                    status: transaction.status,
                    amount: transaction.amount,
                    currency: transaction.currency,
                    createdAt: transaction.createdAt,
                    customerName: transaction.customerName,
                    customerDoc: transaction.customerDoc,
                    billingStatus: transaction.billingStatus
                  }
                  
                  // Guardar en localStorage para que App.jsx pueda acceder
                  localStorage.setItem('pendingTransaction', JSON.stringify(transactionData))
                  
                  // Redirigir a la página principal que manejará la confirmación
                  window.location.href = '/'
                }}
                title="Confirmar facturación - Generar factura"
                style={{
                  background: 'linear-gradient(135deg, #27ae60 0%, #2ecc71 100%)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '8px',
                  padding: '12px 20px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: '0.9rem',
                  fontWeight: '600',
                  boxShadow: '0 4px 12px rgba(39, 174, 96, 0.3)',
                  transition: 'all 0.3s ease'
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
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 6L9 17l-5-5"/>
                </svg>
                ✓ Facturar
              </button>
            </div>
          ) : transaction.billingStatus === 'error' ? (
            <div style={{
              padding: '12px 16px',
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: '8px',
              color: '#ef4444',
              fontSize: '0.9rem',
              fontWeight: '500',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 9v4"/>
                <path d="M12 17h.01"/>
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
              </svg>
              ❌ Error en Facturación
            </div>
          ) : (
            <div style={{
              padding: '12px 16px',
              backgroundColor: 'rgba(149, 165, 166, 0.1)',
              border: '1px solid rgba(149, 165, 166, 0.3)',
              borderRadius: '8px',
              color: '#95a5a6',
              fontSize: '0.9rem',
              fontWeight: '500',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              ➖ {transaction.status === 'AUTHORIZED' ? 'Solo PAID' : 'No disponible'}
            </div>
          )}
        </div>
      )}

      {/* Notifications Card */}
      <div style={{
        background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.9) 0%, rgba(30, 41, 59, 0.95) 25%, rgba(51, 65, 85, 0.9) 50%, rgba(30, 41, 59, 0.95) 75%, rgba(15, 23, 42, 0.9) 100%)',
        borderRadius: '16px',
        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4), 0 10px 25px rgba(245, 158, 11, 0.15), 0 4px 12px rgba(0, 0, 0, 0.3)',
        border: '1px solid rgba(245, 158, 11, 0.2)',
        padding: '1.5rem',
        backdropFilter: 'blur(20px)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <h2 style={{
          fontSize: '1.5rem',
          color: '#ffffff',
          marginBottom: '1.2rem',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontWeight: '600',
          textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)',
          fontFamily: 'Inter, sans-serif'
        }}>
          <div style={{
            background: 'rgba(245, 158, 11, 0.15)',
            padding: '6px',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: '#f59e0b' }}>
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          Notificaciones
        </h2>
        
        <p style={{ 
          color: '#cbd5e1', 
          marginBottom: '1.2rem',
          fontSize: '0.9rem',
          lineHeight: '1.5'
        }}>
          Enviar notificaciones al cliente sobre el estado de la factura
        </p>
        
        <div style={{
          display: 'flex',
          gap: '12px',
          flexWrap: 'wrap'
        }}>
          <button
            onClick={() => handleSendNotification('Email')}
            style={{
              padding: '0.8rem 1.2rem',
              background: 'linear-gradient(135deg, #dc2626 0%, #991b1b 100%)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '10px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '0.85rem',
              fontWeight: '600',
              transition: 'all 0.3s ease',
              boxShadow: '0 3px 8px rgba(220, 38, 38, 0.3)',
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
              padding: '0.8rem 1.2rem',
              background: 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '10px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '0.85rem',
              fontWeight: '600',
              transition: 'all 0.3s ease',
              boxShadow: '0 3px 8px rgba(22, 163, 74, 0.3)',
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
          
          @keyframes gradientShift {
            0%, 100% {
              background-position: 0% 50%;
            }
            50% {
              background-position: 100% 50%;
            }
          }
          
          @keyframes float {
            0%, 100% {
              transform: translateY(0px) rotate(0deg);
            }
            50% {
              transform: translateY(-20px) rotate(180deg);
            }
          }
          
          @keyframes titleGlow {
            0% {
              text-shadow: 0 6px 12px rgba(0,0,0,0.4), 0 0 30px rgba(231, 76, 60, 0.2), 0 0 20px rgba(243, 156, 18, 0.1);
            }
            50% {
              text-shadow: 0 6px 12px rgba(0,0,0,0.4), 0 0 40px rgba(231, 76, 60, 0.3), 0 0 30px rgba(243, 156, 18, 0.2);
            }
            100% {
              text-shadow: 0 6px 12px rgba(0,0,0,0.4), 0 0 30px rgba(231, 76, 60, 0.2), 0 0 20px rgba(243, 156, 18, 0.1);
            }
          }
        `}
      </style>
    </div>
  )
}