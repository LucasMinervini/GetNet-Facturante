import React, { useState, useEffect } from 'react'
import axios from 'axios'

const API = import.meta.env.VITE_API_URL || 'http://localhost:1234'

export default function BillingSettings({ onBack }) {
  const [settings, setSettings] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    loadSettings()
  }, [])

  const loadSettings = async () => {
    try {
      setLoading(true)
      const response = await axios.get(`${API}/api/billing-settings/active`)
      setSettings(response.data)
    } catch (err) {
      if (err.response?.status === 404) {
        // No hay configuración activa, crear una por defecto
        await createDefaultSettings()
      } else {
        setError('Error al cargar la configuración: ' + err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  const createDefaultSettings = async () => {
    try {
      const response = await axios.post(`${API}/api/billing-settings/init-default`)
      setSettings(response.data)
    } catch (err) {
      setError('Error al crear configuración por defecto: ' + err.message)
    }
  }

  const handleSave = async () => {
    try {
      setSaving(true)
      setError('')
      setMessage('')

      if (settings.id) {
        // Actualizar configuración existente
        await axios.put(`${API}/api/billing-settings/${settings.id}`, settings)
        setMessage('Configuración actualizada exitosamente')
      } else {
        // Crear nueva configuración
        const response = await axios.post(`${API}/api/billing-settings`, settings)
        setSettings(response.data)
        setMessage('Configuración creada exitosamente')
      }
    } catch (err) {
      setError('Error al guardar: ' + (err.response?.data || err.message))
    } finally {
      setSaving(false)
    }
  }

  const handleInputChange = (field, value) => {
    setSettings(prev => ({
      ...prev,
      [field]: value
    }))
  }

  const handleNumberInput = (field, value) => {
    const numValue = parseFloat(value) || 0
    handleInputChange(field, numValue)
  }

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>Cargando configuración...</p>
      </div>
    )
  }

  return (
    <div className="settings-container">
      {/* Header */}
      <div className="header">
        <div className="header-content">
          <button onClick={onBack} className="btn btn-secondary">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            Volver
          </button>
          <div className="header-title">
            <h1>Configuración de Facturación</h1>
            <h2>Configura los parámetros para la emisión de facturas</h2>
          </div>
          <div></div>
        </div>
      </div>

      {/* Mensajes */}
      {message && (
        <div className="alert alert-success">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          {message}
        </div>
      )}

      {error && (
        <div className="alert alert-error">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          {error}
        </div>
      )}

      {/* Formulario */}
      <div className="settings-form">
        <div className="form-section">
          <h3>Datos de la Empresa</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">CUIT Empresa *</label>
              <input
                type="text"
                value={settings?.cuitEmpresa || ''}
                onChange={(e) => handleInputChange('cuitEmpresa', e.target.value)}
                placeholder="20123456789"
                className="form-input"
                maxLength="11"
              />
              <small>Ingrese el CUIT de la empresa emisora</small>
            </div>

            <div className="form-group">
              <label className="form-label">Razón Social *</label>
              <input
                type="text"
                value={settings?.razonSocialEmpresa || ''}
                onChange={(e) => handleInputChange('razonSocialEmpresa', e.target.value)}
                placeholder="Mi Empresa S.A."
                className="form-input"
                maxLength="200"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Punto de Venta *</label>
              <input
                type="text"
                value={settings?.puntoVenta || ''}
                onChange={(e) => handleInputChange('puntoVenta', e.target.value)}
                placeholder="0001"
                className="form-input"
                maxLength="4"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Tipo de Comprobante *</label>
              <select
                value={settings?.tipoComprobante || 'FB'}
                onChange={(e) => handleInputChange('tipoComprobante', e.target.value)}
                className="form-select"
              >
                <option value="FA">Factura A</option>
                <option value="FB">Factura B</option>
                <option value="FC">Factura C</option>
              </select>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Configuración de IVA</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">IVA por Defecto (%)</label>
              <input
                type="number"
                value={settings?.ivaPorDefecto || 21}
                onChange={(e) => handleNumberInput('ivaPorDefecto', e.target.value)}
                placeholder="21"
                className="form-input"
                min="0"
                max="100"
                step="0.01"
              />
              <small>Porcentaje de IVA a aplicar por defecto</small>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Reglas de Facturación</h3>
          <div className="form-grid">
            <div className="form-group checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={settings?.facturarSoloPaid || false}
                  onChange={(e) => handleInputChange('facturarSoloPaid', e.target.checked)}
                  className="checkbox-input"
                />
                <span className="checkbox-text">Facturar solo transacciones PAID</span>
              </label>
              <small>Si está habilitado, solo se facturarán automáticamente las transacciones con estado PAID</small>
            </div>

            <div className="form-group checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={settings?.requireBillingConfirmation || false}
                  onChange={(e) => handleInputChange('requireBillingConfirmation', e.target.checked)}
                  className="checkbox-input"
                />
                <span className="checkbox-text">Requerir confirmación antes de facturar</span>
              </label>
              <small>Si está habilitado, las transacciones PAID esperarán confirmación manual antes de facturar</small>
            </div>

            <div className="form-group checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={settings?.consumidorFinalPorDefecto || false}
                  onChange={(e) => handleInputChange('consumidorFinalPorDefecto', e.target.checked)}
                  className="checkbox-input"
                />
                <span className="checkbox-text">Consumidor Final por defecto</span>
              </label>
              <small>Si está habilitado, se usará Consumidor Final cuando no se proporcione CUIT</small>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Configuración de Consumidor Final</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">CUIT Consumidor Final</label>
              <input
                type="text"
                value={settings?.cuitConsumidorFinal || '00000000000'}
                onChange={(e) => handleInputChange('cuitConsumidorFinal', e.target.value)}
                placeholder="00000000000"
                className="form-input"
                maxLength="11"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Razón Social Consumidor Final</label>
              <input
                type="text"
                value={settings?.razonSocialConsumidorFinal || 'Consumidor Final'}
                onChange={(e) => handleInputChange('razonSocialConsumidorFinal', e.target.value)}
                placeholder="Consumidor Final"
                className="form-input"
                maxLength="200"
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Configuración Adicional</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">Email de Facturación</label>
              <input
                type="email"
                value={settings?.emailFacturacion || ''}
                onChange={(e) => handleInputChange('emailFacturacion', e.target.value)}
                placeholder="facturacion@empresa.com"
                className="form-input"
                maxLength="200"
              />
              <small>Email donde se enviarán los comprobantes</small>
            </div>

            <div className="form-group checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={settings?.enviarComprobante || false}
                  onChange={(e) => handleInputChange('enviarComprobante', e.target.checked)}
                  className="checkbox-input"
                />
                <span className="checkbox-text">Enviar comprobante por email</span>
              </label>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Información General</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">Descripción</label>
              <input
                type="text"
                value={settings?.descripcion || ''}
                onChange={(e) => handleInputChange('descripcion', e.target.value)}
                placeholder="Configuración principal"
                className="form-input"
                maxLength="500"
              />
              <small>Descripción opcional para identificar esta configuración</small>
            </div>

            <div className="form-group checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={settings?.activo || false}
                  onChange={(e) => handleInputChange('activo', e.target.checked)}
                  className="checkbox-input"
                />
                <span className="checkbox-text">Configuración activa</span>
              </label>
              <small>Esta configuración será la utilizada para la facturación</small>
            </div>
          </div>
        </div>

        {/* Botones de acción */}
        <div className="form-actions">
          <button
            onClick={handleSave}
            disabled={saving}
            className="btn btn-primary"
          >
            {saving ? (
              <>
                <div className="spinner-small"></div>
                Guardando...
              </>
            ) : (
              <>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                  <polyline points="17,21 17,13 7,13 7,21"/>
                  <polyline points="7,3 7,8 15,8"/>
                </svg>
                Guardar Configuración
              </>
            )}
          </button>

          <button
            onClick={onBack}
            className="btn btn-primary"
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>
  )
}
