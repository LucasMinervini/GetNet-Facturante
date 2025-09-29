import React from 'react'

export default function ResultsInfo({ count, total, loading }) {
  return (
    <div style={{
      marginBottom: '1rem',
      color: 'var(--text-secondary)',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: '0.875rem'
    }}>
      <span>
        Mostrando {count} de {total} transacciones{loading ? ' (Cargando...)' : ''}
      </span>
    </div>
  )
}


