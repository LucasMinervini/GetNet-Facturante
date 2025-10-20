import React, { useEffect, useState } from 'react'

export default function Toast({ message, type = 'info', duration = 3500, onClose }) {
  const [visible, setVisible] = useState(Boolean(message))

  useEffect(() => {
    if (!message) {
      setVisible(false)
      return
    }
    setVisible(true)
    const timer = setTimeout(() => {
      setVisible(false)
      if (onClose) onClose()
    }, Math.max(1500, duration))
    return () => clearTimeout(timer)
  }, [message, duration, onClose])

  if (!visible) return null

  const icon = type === 'success' ? (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 6L9 17l-5-5"/></svg>
  ) : type === 'error' ? (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
  ) : (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
  )

  return (
    <div className={`toast toast-${type}`} role="status" aria-live="polite">
      <span style={{ display: 'inline-flex', marginRight: '8px' }}>{icon}</span>
      {message}
    </div>
  )
}


