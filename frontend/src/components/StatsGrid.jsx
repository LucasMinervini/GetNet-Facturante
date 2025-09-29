import React from 'react'

export default function StatsGrid({ totalElements, pageSubtotal, currency, paidCount, authorizedCount, formatCurrency }) {
  return (
    <div className="stats-grid">
      <div className="stat-card">
        <div className="stat-value">{totalElements}</div>
        <div className="stat-label">Total Transacciones</div>
      </div>
      <div className="stat-card">
        <div className="stat-value">{formatCurrency(pageSubtotal, currency)}</div>
        <div className="stat-label">Subtotal Página</div>
      </div>
      <div className="stat-card">
        <div className="stat-value">{paidCount}</div>
        <div className="stat-label">Transacciones Pagadas</div>
      </div>
      <div className="stat-card">
        <div className="stat-value">{authorizedCount}</div>
        <div className="stat-label">Transacciones Autorizadas</div>
      </div>
    </div>
  )
}


