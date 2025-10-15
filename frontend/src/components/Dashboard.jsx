import React, { useState, useEffect } from 'react';
import { api } from '../api';
import StatsGrid from './StatsGrid';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalTransactions: 0,
    totalInvoices: 0,
    totalAmount: 0,
    errorCount: 0,
    pendingTransactions: 0,
    successRate: 0
  });
  
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dateRange, setDateRange] = useState({
    start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    console.log('Dashboard montado, cargando datos...');
    loadDashboardData();
  }, [dateRange]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('Cargando dashboard con fechas:', dateRange);

      // Cargar estadísticas
      console.log('Llamando a /api/dashboard/stats...');
      const statsResponse = await api.get('/api/dashboard/stats', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end
        }
      });
      console.log('Stats recibidas:', statsResponse.data);

      // Cargar transacciones recientes
      console.log('Llamando a /api/transactions...');
      const transactionsResponse = await api.get('/api/transactions', {
        params: {
          page: 0,
          size: 10,
          sortBy: 'createdAt',
          sortDir: 'desc'
        }
      });
      console.log('Transacciones recibidas:', transactionsResponse.data);

      setStats(statsResponse.data);
      setRecentTransactions(transactionsResponse.data.content || []);
    } catch (err) {
      console.error('Error cargando dashboard:', err);
      console.error('Error completo:', err.response || err);
      setError(`Error al cargar los datos del dashboard: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (newDateRange) => {
    setDateRange(newDateRange);
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
          Cargando dashboard...
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
          <h2 style={{ color: '#c33', marginBottom: '15px' }}>Error en Dashboard</h2>
          <p style={{ color: '#666', marginBottom: '20px' }}>{error}</p>
          <button 
            onClick={loadDashboardData} 
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
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Dashboard - GetNet Facturante</h1>
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
          <button onClick={() => loadDashboardData()} className="refresh-button">
            Actualizar
          </button>
        </div>
      </div>

      <div className="dashboard-content">
        <StatsGrid 
          totalElements={stats.totalTransactions}
          pageSubtotal={stats.totalAmount}
          currency="ARS"
          paidCount={stats.totalTransactions}
          authorizedCount={stats.totalTransactions}
          formatCurrency={(amount, currency) => `$${amount.toLocaleString('es-AR')} ${currency}`}
        />
        
        <div className="dashboard-sections">
          <div className="section">
            <h2>Transacciones Recientes</h2>
            <div className="transactions-list">
              {recentTransactions.length > 0 ? (
                recentTransactions.map(transaction => (
                  <div key={transaction.id} className="transaction-item">
                    <div className="transaction-info">
                      <span className="transaction-id">{transaction.externalId}</span>
                      <span className="transaction-status">{transaction.status}</span>
                      <span className="transaction-amount">${transaction.amount}</span>
                    </div>
                  </div>
                ))
              ) : (
                <p>No hay transacciones recientes</p>
              )}
            </div>
          </div>
          
          <div className="section">
            <h2>Resumen de Actividad</h2>
            <div className="activity-summary">
              <div className="summary-item">
                <span className="label">Tasa de Éxito:</span>
                <span className={`value ${stats.successRate >= 95 ? 'success' : stats.successRate >= 80 ? 'warning' : 'error'}`}>
                  {stats.successRate.toFixed(1)}%
                </span>
              </div>
              <div className="summary-item">
                <span className="label">Transacciones Pendientes:</span>
                <span className="value warning">{stats.pendingTransactions}</span>
              </div>
              <div className="summary-item">
                <span className="label">Errores Recientes:</span>
                <span className={`value ${stats.errorCount === 0 ? 'success' : 'error'}`}>
                  {stats.errorCount}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
