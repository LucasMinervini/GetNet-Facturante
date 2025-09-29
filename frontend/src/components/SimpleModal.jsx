import React from 'react'

export default function SimpleModal({ open, title, children, onClose }) {
  if (!open) return null
  return (
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
          <strong style={{ color: '#111' }}>{title}</strong>
        </div>
        <div style={{ color: '#4b5563', marginBottom: '18px' }}>
          {children}
        </div>
        <button
          onClick={onClose}
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
  )
}


