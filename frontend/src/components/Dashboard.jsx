import React, { useState, useEffect } from 'react';
import { api } from '../api';
import StatsGrid from './StatsGrid';
import TransactionsTable from './TransactionsTable';
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
    loadDashboardData();
  }, [dateRange]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Cargar estadísticas
      const statsResponse = await api.get('/api/dashboard/stats', {
        params: {
          startDate: dateRange.start,
          endDate: dateRange.end
        }
      });

      // Cargar transacciones recientes
      const transactionsResponse = await api.get('/api/transactions', {
        params: {
          page: 0,
          size: 10,
          sort: 'createdAt,desc'
        }
      });

      setStats(statsResponse.data);
      setRecentTransactions(transactionsResponse.data.content || []);
    } catch (err) {
      console.error('Error cargando dashboard:', err);
      setError('Error al cargar los datos del dashboard');
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (newDateRange) => {
    setDateRange(newDateRange);
  };

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="loading-spinner"></div>
        <p>Cargando dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-error">
        <h2>Error en Dashboard</h2>
        <p>{error}</p>
        <button onClick={loadDashboardData} className="retry-button">
          Reintentar
        </button>
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
        <StatsGrid stats={stats} />
        
        <div className="dashboard-sections">
          <div className="section">
            <h2>Transacciones Recientes</h2>
            <TransactionsTable 
              transactions={recentTransactions}
              showPagination={false}
              compact={true}
            />
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
