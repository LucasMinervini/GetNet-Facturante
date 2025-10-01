import React, { useState, useEffect } from 'react';
import { api } from '../api';
import './Reports.css';

const Reports = () => {
  const [reports, setReports] = useState({
    transactionVolume: [],
    invoiceStats: [],
    errorStats: [],
    reconciliationStats: []
  });
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dateRange, setDateRange] = useState({
    start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0]
  });
  const [selectedReport, setSelectedReport] = useState('overview');

  useEffect(() => {
    loadReports();
  }, [dateRange, selectedReport]);

  const loadReports = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await api.get('/api/reports', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end,
          type: selectedReport
        }
      });

      setReports(response.data);
    } catch (err) {
      console.error('Error cargando reportes:', err);
      setError('Error al cargar los reportes');
    } finally {
      setLoading(false);
    }
  };

  const exportReport = async (format = 'csv') => {
    try {
      const response = await api.get('/api/reports/export', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end,
          type: selectedReport,
          format: format
        },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `reporte_${selectedReport}_${dateRange.start}_${dateRange.end}.${format}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error exportando reporte:', err);
      alert('Error al exportar el reporte');
    }
  };

  if (loading) {
    return (
      <div className="reports-loading">
        <div className="loading-spinner"></div>
        <p>Generando reportes...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="reports-error">
        <h2>Error en Reportes</h2>
        <p>{error}</p>
        <button onClick={loadReports} className="retry-button">
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="reports">
      <div className="reports-header">
        <h1>Reportes y Estadísticas</h1>
        <div className="reports-controls">
          <div className="date-range-selector">
            <label htmlFor="start-date">Desde:</label>
            <input
              id="start-date"
              type="date"
              value={dateRange.start}
              onChange={(e) => setDateRange(prev => ({ ...prev, start: e.target.value }))}
            />
            <label htmlFor="end-date">Hasta:</label>
            <input
              id="end-date"
              type="date"
              value={dateRange.end}
              onChange={(e) => setDateRange(prev => ({ ...prev, end: e.target.value }))}
            />
          </div>
          
          <div className="report-type-selector">
            <label htmlFor="report-type">Tipo de Reporte:</label>
            <select
              id="report-type"
              value={selectedReport}
              onChange={(e) => setSelectedReport(e.target.value)}
            >
              <option value="overview">Resumen General</option>
              <option value="transactions">Transacciones</option>
              <option value="invoices">Facturas</option>
              <option value="errors">Errores</option>
              <option value="reconciliation">Reconciliación</option>
            </select>
          </div>
          
          <div className="export-controls">
            <button onClick={() => exportReport('csv')} className="export-button">
              Exportar CSV
            </button>
            <button onClick={() => exportReport('xlsx')} className="export-button">
              Exportar Excel
            </button>
            <button onClick={loadReports} className="refresh-button">
              Actualizar
            </button>
          </div>
        </div>
      </div>

      <div className="reports-content">
        {selectedReport === 'overview' && <OverviewReport data={reports} />}
        {selectedReport === 'transactions' && <TransactionsReport data={reports.transactionVolume} />}
        {selectedReport === 'invoices' && <InvoicesReport data={reports.invoiceStats} />}
        {selectedReport === 'errors' && <ErrorsReport data={reports.errorStats} />}
        {selectedReport === 'reconciliation' && <ReconciliationReport data={reports.reconciliationStats} />}
      </div>
    </div>
  );
};

// Componente para reporte de resumen general
const OverviewReport = ({ data }) => (
  <div className="report-section">
    <h2>Resumen General</h2>
    <div className="overview-grid">
      <div className="overview-card">
        <h3>Volumen de Transacciones</h3>
        <div className="metric">
          <span className="value">{data.transactionVolume?.total || 0}</span>
          <span className="label">Transacciones</span>
        </div>
        <div className="metric">
          <span className="value">${data.transactionVolume?.totalAmount?.toLocaleString() || 0}</span>
          <span className="label">Monto Total</span>
        </div>
      </div>
      
      <div className="overview-card">
        <h3>Facturas Emitidas</h3>
        <div className="metric">
          <span className="value">{data.invoiceStats?.total || 0}</span>
          <span className="label">Facturas</span>
        </div>
        <div className="metric">
          <span className="value">{data.invoiceStats?.successRate || 0}%</span>
          <span className="label">Tasa de Éxito</span>
        </div>
      </div>
      
      <div className="overview-card">
        <h3>Errores del Sistema</h3>
        <div className="metric">
          <span className="value">{data.errorStats?.total || 0}</span>
          <span className="label">Errores</span>
        </div>
        <div className="metric">
          <span className="value">{data.errorStats?.webhookErrors || 0}</span>
          <span className="label">Errores de Webhook</span>
        </div>
      </div>
    </div>
  </div>
);

// Componente para reporte de transacciones
const TransactionsReport = ({ data }) => (
  <div className="report-section">
    <h2>Reporte de Transacciones</h2>
    <div className="transactions-chart">
      <h3>Volumen por Día</h3>
      <div className="chart-placeholder">
        <p>Gráfico de transacciones por día</p>
        <p>Datos: {JSON.stringify(data, null, 2)}</p>
      </div>
    </div>
  </div>
);

// Componente para reporte de facturas
const InvoicesReport = ({ data }) => (
  <div className="report-section">
    <h2>Reporte de Facturas</h2>
    <div className="invoices-stats">
      <div className="stat-item">
        <span className="label">Total de Facturas:</span>
        <span className="value">{data?.total || 0}</span>
      </div>
      <div className="stat-item">
        <span className="label">Facturas Exitosas:</span>
        <span className="value success">{data?.successful || 0}</span>
      </div>
      <div className="stat-item">
        <span className="label">Facturas con Error:</span>
        <span className="value error">{data?.failed || 0}</span>
      </div>
      <div className="stat-item">
        <span className="label">Tasa de Éxito:</span>
        <span className="value">{data?.successRate || 0}%</span>
      </div>
    </div>
  </div>
);

// Componente para reporte de errores
const ErrorsReport = ({ data }) => (
  <div className="report-section">
    <h2>Reporte de Errores</h2>
    <div className="errors-list">
      {data?.errors?.map((error, index) => (
        <div key={index} className="error-item">
          <div className="error-header">
            <span className="error-type">{error.type}</span>
            <span className="error-time">{error.timestamp}</span>
          </div>
          <div className="error-message">{error.message}</div>
          <div className="error-details">
            <span className="error-count">Ocurrencias: {error.count}</span>
          </div>
        </div>
      )) || <p>No hay errores registrados en el período seleccionado.</p>}
    </div>
  </div>
);

// Componente para reporte de reconciliación
const ReconciliationReport = ({ data }) => (
  <div className="report-section">
    <h2>Reporte de Reconciliación</h2>
    <div className="reconciliation-stats">
      <div className="stat-item">
        <span className="label">Transacciones Procesadas:</span>
        <span className="value">{data?.processed || 0}</span>
      </div>
      <div className="stat-item">
        <span className="label">Transacciones Huérfanas:</span>
        <span className="value warning">{data?.orphans || 0}</span>
      </div>
      <div className="stat-item">
        <span className="label">Errores de Reconciliación:</span>
        <span className="value error">{data?.errors || 0}</span>
      </div>
    </div>
  </div>
);

export default Reports;
