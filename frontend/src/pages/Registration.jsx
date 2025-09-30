import React, { useState } from 'react'
import axios from 'axios'
import { API } from '../api'

export default function Registration({ onSuccess }) {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    if (!username || !email || !password || !confirmPassword) {
      setError('Todos los campos son obligatorios')
      return
    }
    if (password !== confirmPassword) {
      setError('Las contraseñas no coinciden')
      return
    }
    setLoading(true)
    try {
      const res = await axios.post(`${API}/api/auth/register`, {
        username,
        email,
        password
      })
      setSuccess('Registro exitoso. Ahora puedes iniciar sesión.')
      if (onSuccess) onSuccess()
    } catch (err) {
      const msg = err?.response?.data?.error || 'No se pudo registrar'
      setError(msg)
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
        maxWidth: '420px',
        color: 'white',
        border: '1px solid rgba(255,255,255,0.08)'
      }}>
        <h2 style={{ marginBottom: '1rem', textAlign: 'center' }}>Crear cuenta</h2>
        <div className="form-group" style={{ marginBottom: '12px' }}>
          <label className="form-label">Usuario</label>
          <input className="form-input" value={username} onChange={e=>setUsername(e.target.value)} placeholder="tu_usuario" />
        </div>
        <div className="form-group" style={{ marginBottom: '12px' }}>
          <label className="form-label">Email</label>
          <input className="form-input" type="email" value={email} onChange={e=>setEmail(e.target.value)} placeholder="tu@email.com" />
        </div>
        <div className="form-group" style={{ marginBottom: '12px' }}>
          <label className="form-label">Contraseña</label>
          <input className="form-input" type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="••••••" />
        </div>
        <div className="form-group" style={{ marginBottom: '12px' }}>
          <label className="form-label">Confirmar Contraseña</label>
          <input className="form-input" type="password" value={confirmPassword} onChange={e=>setConfirmPassword(e.target.value)} placeholder="••••••" />
        </div>
        {error && <div style={{ color: '#fca5a5', marginBottom: '10px' }}>{error}</div>}
        {success && <div style={{ color: '#34d399', marginBottom: '10px' }}>{success}</div>}
        <button disabled={loading} className="btn btn-primary" style={{ width: '100%' }}>
          {loading ? 'Creando cuenta...' : 'Registrarme'}
        </button>
        <div style={{ marginTop: '10px', fontSize: '12px', opacity: 0.8, textAlign: 'center' }}>
          ¿Ya tienes cuenta? <a href="/" style={{ color: '#93c5fd' }}>Inicia sesión</a>
        </div>
      </form>
    </div>
  )
}


