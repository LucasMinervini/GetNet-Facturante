import React from 'react'

export default function TransactionsTable({
  transactions,
  onRowClick,
  translateStatus,
  formatCurrency,
  getRelativeTime,
  onDownloadInvoice,
  onProcessRefund,
  onDownloadCreditNote,
  onConfirmBilling
}) {
  return (
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
            <tr key={t.id} onClick={() => onRowClick(t.id)}>
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
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' }}>
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
                        onClick={(e) => onDownloadCreditNote(t.id, e)}
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
                          onClick={(e) => onDownloadInvoice(t, e)}
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
                        >
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7,10 12,15 17,10"/>
                            <line x1="12" y1="15" x2="12" y2="3"/>
                          </svg>
                          <span>PDF</span>
                        </button>
                        <button
                          onClick={(e) => onProcessRefund(t.id, e)}
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
                        onClick={(e) => onConfirmBilling(t.id, e)}
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
        </tbody>
      </table>
    </div>
  )
}


