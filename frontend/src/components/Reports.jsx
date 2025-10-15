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
  const [runningReconciliation, setRunningReconciliation] = useState(false);
  const [error, setError] = useState(null);
  const [dateRange, setDateRange] = useState({
    start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0]
  });
  const [selectedReport, setSelectedReport] = useState('overview');

  useEffect(() => {
    console.log('Reports montado, cargando datos...');
    loadReports();
  }, [dateRange, selectedReport]);

  const loadReports = async () => {
    try {
      setLoading(true);
      setError(null);

      // Cargar reporte consolidado
      const consolidatedResponse = await api.get('/api/reports/consolidated', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end
        }
      });

      // Cargar transacciones por día
      const dailyResponse = await api.get('/api/dashboard/transactions-by-day', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end
        }
      });

      // Cargar facturas por estado
      const invoicesStatusResponse = await api.get('/api/dashboard/invoices-by-status');

      // Cargar reporte de reconciliación
      const reconciliationResponse = await api.get('/api/reports/reconciliation', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end
        }
      });

      // Estructurar datos para los componentes
      const consolidated = consolidatedResponse.data;
      setReports({
        transactionVolume: {
          total: consolidated.totalTransactions,
          totalAmount: consolidated.totalAmount,
          daily: dailyResponse.data
        },
        invoiceStats: {
          total: consolidated.totalInvoices,
          byStatus: invoicesStatusResponse.data,
          successful: invoicesStatusResponse.data.sent || 0,
          failed: invoicesStatusResponse.data.error || 0,
          successRate: consolidated.totalInvoices > 0 
            ? ((invoicesStatusResponse.data.sent || 0) / consolidated.totalInvoices * 100).toFixed(1)
            : 0
        },
        errorStats: {
          total: consolidated.webhookErrors,
          webhookErrors: consolidated.webhookErrors,
          errors: []
        },
        reconciliationStats: {
          processed: reconciliationResponse.data.totalTransactions,
          orphans: reconciliationResponse.data.unreconciledTransactions,
          errors: reconciliationResponse.data.errorTransactions?.length || 0,
          details: reconciliationResponse.data
        },
        consolidated: consolidated
      });

    } catch (err) {
      console.error('Error cargando reportes:', err);
      console.error('Error completo:', err.response || err);
      setError(`Error al cargar los reportes: ${err.message || 'Error desconocido'}`);
    } finally {
      setLoading(false);
    }
  };

  const runReconciliation = async () => {
    try {
      setRunningReconciliation(true);
      await api.post('/api/reconciliation/run', null, {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end
        }
      });
      await loadReports();
      alert('Reconciliación ejecutada correctamente');
    } catch (err) {
      console.error('Error al ejecutar reconciliación:', err);
      alert(`Error al ejecutar reconciliación: ${err?.response?.data || err.message}`);
    } finally {
      setRunningReconciliation(false);
    }
  };

  const exportReport = async (format = 'csv') => {
    try {
      let endpoint = '/api/reports/transactions/export';
      let filename = `transacciones_${dateRange.start}_${dateRange.end}`;
      
      // Seleccionar endpoint según el tipo de reporte
      if (selectedReport === 'invoices') {
        endpoint = '/api/reports/invoices/export';
        filename = `facturas_${dateRange.start}_${dateRange.end}`;
      } else if (selectedReport === 'reconciliation') {
        endpoint = '/api/reports/transactions/export';
        filename = `reconciliacion_${dateRange.start}_${dateRange.end}`;
      }

      const response = await api.get(endpoint, {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end,
          format: format
        },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${filename}.csv`);
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
      <div style={{ 
        minHeight: '100vh', 
        display: 'flex', 
        flexDirection: 'column',
        alignItems: 'center', 
        justifyContent: 'center',
        background: 'white'
      }}>
        <div style={{
          border: '4px solid #f3f3f3',
          borderTop: '4px solid #3498db',
          borderRadius: '50%',
          width: '50px',
          height: '50px',
          animation: 'spin 1s linear infinite'
        }}></div>
        <p style={{ marginTop: '20px', fontSize: '16px', color: '#333' }}>
          Generando reportes...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ 
        minHeight: '100vh', 
        display: 'flex', 
        flexDirection: 'column',
        alignItems: 'center', 
        justifyContent: 'center',
        background: 'white',
        padding: '20px'
      }}>
        <div style={{
          background: '#fee',
          border: '2px solid #fcc',
          borderRadius: '8px',
          padding: '30px',
          maxWidth: '600px',
          textAlign: 'center'
        }}>
          <h2 style={{ color: '#c33', marginBottom: '15px' }}>Error en Reportes</h2>
          <p style={{ color: '#666', marginBottom: '20px' }}>{error}</p>
          <button 
            onClick={loadReports}
            style={{
              background: '#3498db',
              color: 'white',
              border: 'none',
              padding: '10px 20px',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '14px'
            }}
          >
            Reintentar
          </button>
        </div>
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
            {selectedReport === 'reconciliation' && (
              <button
                onClick={runReconciliation}
                className="export-button"
                disabled={runningReconciliation}
                title="Forzar reconciliación para el rango de fechas seleccionado"
              >
                {runningReconciliation ? 'Reconciliando...' : 'Forzar Reconciliación'}
              </button>
            )}
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
    <div className="transactions-summary">
      <div className="summary-card">
        <h3>Total de Transacciones</h3>
        <div className="big-value">{data?.total || 0}</div>
      </div>
      <div className="summary-card">
        <h3>Monto Total</h3>
        <div className="big-value">${(data?.totalAmount || 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })}</div>
      </div>
    </div>
    <div className="transactions-chart">
      <h3>Volumen por Día</h3>
      {data?.daily && data.daily.length > 0 ? (
        <div className="chart-data">
          <table className="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Cantidad</th>
                <th>Monto</th>
              </tr>
            </thead>
            <tbody>
              {data.daily.map((day, index) => (
                <tr key={index}>
                  <td>{day.date}</td>
                  <td>{day.count}</td>
                  <td>${(day.amount || 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p>No hay datos de transacciones para el período seleccionado.</p>
      )}
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
      <div className="stat-item">
        <span className="label">Tasa de Reconciliación:</span>
        <span className="value">{data?.details?.reconciliationRate?.toFixed(1) || 0}%</span>
      </div>
    </div>
    
    {data?.details?.orphanTransactions && data.details.orphanTransactions.length > 0 && (
      <div className="orphan-transactions">
        <h3>Transacciones Huérfanas</h3>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID Externo</th>
              <th>Monto</th>
              <th>Estado</th>
              <th>Fecha</th>
            </tr>
          </thead>
          <tbody>
            {data.details.orphanTransactions.map((tx, index) => (
              <tr key={index}>
                <td>{tx.externalId}</td>
                <td>${tx.amount.toLocaleString('es-AR', { minimumFractionDigits: 2 })}</td>
                <td><span className={`badge ${tx.status.toLowerCase()}`}>{tx.status}</span></td>
                <td>{tx.createdAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </div>
);

export default Reports;
