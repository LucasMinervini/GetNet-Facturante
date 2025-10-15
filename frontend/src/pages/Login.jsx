import React, { useState } from 'react'
import axios from 'axios'
import { API } from '../api'

export default function Login({ onSuccess }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await axios.post(`${API}/api/auth/login`, { username, password })
      const { accessToken, refreshToken } = res.data || {}
      if (!accessToken) throw new Error('Token no recibido')
      localStorage.setItem('accessToken', accessToken)
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
      // Recargar la página para que App.jsx detecte el token
      window.location.reload()
    } catch (err) {
      setError('Credenciales inválidas o servidor no disponible')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <form onSubmit={handleSubmit} style={{
        background: 'linear-gradient(180deg, rgba(30,41,59,0.9), rgba(15,23,42,0.95))',
        padding: '2rem',
        borderRadius: '16px',
        boxShadow: '0 10px 30px rgba(0,0,0,0.3)',
        width: '100%',
        maxWidth: '380px',
        color: 'white',
        border: '1px solid rgba(255,255,255,0.08)'
      }}>
        <h2 style={{ marginBottom: '1rem', textAlign: 'center' }}>Ingresar</h2>
        <div className="form-group" style={{ marginBottom: '12px' }}>
          <label className="form-label">Usuario</label>
          <input className="form-input" value={username} onChange={e=>setUsername(e.target.value)} placeholder="admin" />
        </div>
        <div className="form-group" style={{ marginBottom: '12px' }}>
          <label className="form-label">Contraseña</label>
          <input className="form-input" type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="••••••" />
        </div>
        {error && <div style={{ color: '#fca5a5', marginBottom: '10px' }}>{error}</div>}
        <button disabled={loading} className="btn btn-primary" style={{ width: '100%' }}>
          {loading ? 'Ingresando...' : 'Ingresar'}
        </button>
        <div style={{ marginTop: '10px', fontSize: '12px', opacity: 0.8, textAlign: 'center' }}>
          ¿No tienes cuenta? <a href="/register" style={{ color: '#93c5fd' }}>Regístrate</a>
        </div>
      </form>
    </div>
  )
}


