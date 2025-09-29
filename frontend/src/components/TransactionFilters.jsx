import React from 'react'

export default function TransactionFilters({ filters, onChange, onClear, onGoToConfirmation, pageSize, onChangePageSize, setCurrentPage }) {
  return (
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
            onChange={(e) => onChange('status', e.target.value)}
            className="form-select enhanced-select"
          >
            <option value="">Todos los estados</option>
            <option value="AUTHORIZED">⏳ Autorizada</option>
            <option value="PAID">✅ Pagado</option>
            <option value="REFUNDED">🔄 Reembolsado</option>
            <option value="FAILED">❌ Falló</option>
          </select>
        </div>

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
            onChange={(e) => onChange('billingStatus', e.target.value)}
            className="form-select enhanced-select"
          >
            <option value="">Todos los estados</option>
            <option value="pending">⏳ Pendiente confirmación</option>
            <option value="billed">✅ Facturado</option>
            <option value="error">⚠️ Error</option>
            <option value="not_applicable">➖ No aplicable</option>
          </select>
        </div>

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
              onChange={(e) => onChange('minAmount', e.target.value)}
              placeholder="0.00"
              className="form-input"
              step="0.01"
              min="0"
            />
          </div>
        </div>

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
              onChange={(e) => onChange('maxAmount', e.target.value)}
              placeholder="999999.99"
              className="form-input"
              step="0.01"
              min="0"
            />
          </div>
        </div>

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
            onChange={(e) => onChange('startDate', e.target.value)}
            className="form-input enhanced-date-input"
          />
        </div>

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
            onChange={(e) => onChange('endDate', e.target.value)}
            className="form-input enhanced-date-input"
          />
        </div>

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
              onChange={(e) => onChange('search', e.target.value)}
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
          <button onClick={onClear} className="btn btn-clear enhanced-clear-btn">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="3,6 5,6 21,6"/>
              <path d="m19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"/>
              <line x1="10" y1="11" x2="10" y2="17"/>
              <line x1="14" y1="11" x2="14" y2="17"/>
            </svg>
            Limpiar Filtros
          </button>

          <button 
            onClick={onGoToConfirmation} 
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
              onChange={(e) => { onChangePageSize(Number(e.target.value)); setCurrentPage(0) }}
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
  )
}


