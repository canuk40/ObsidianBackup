import React, { useEffect, useState } from 'react';
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Calendar, TrendingUp, Activity } from 'lucide-react';
import { historyApi } from '../services/api';
import { format } from 'date-fns';
import './HistoryPage.css';

export default function HistoryPage() {
  const [history, setHistory] = useState([]);
  const [stats, setStats] = useState(null);
  const [period, setPeriod] = useState('30d');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadHistory();
    loadStats();
  }, [period]);

  const loadHistory = async () => {
    try {
      const data = await historyApi.getHistory({ limit: 50 });
      setHistory(data);
    } catch (error) {
      console.error('Failed to load history:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const data = await historyApi.getHistoryStats(period);
      setStats(data);
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  if (loading) {
    return <div className="page-loading">Loading history...</div>;
  }

  return (
    <div className="history-page">
      <div className="page-header">
        <h1>Backup History</h1>
        <div className="period-selector">
          {['7d', '30d', '90d', '1y'].map(p => (
            <button
              key={p}
              className={`period-btn ${period === p ? 'active' : ''}`}
              onClick={() => setPeriod(p)}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      {stats && (
        <>
          <div className="stats-cards">
            <div className="stat-card card">
              <div className="stat-icon" style={{ backgroundColor: 'rgba(99, 102, 241, 0.2)' }}>
                <Activity size={24} color="var(--color-primary)" />
              </div>
              <div className="stat-content">
                <h3>{stats.totalBackups}</h3>
                <p>Total Backups</p>
              </div>
            </div>
            <div className="stat-card card">
              <div className="stat-icon" style={{ backgroundColor: 'rgba(16, 185, 129, 0.2)' }}>
                <TrendingUp size={24} color="var(--color-success)" />
              </div>
              <div className="stat-content">
                <h3>{stats.successRate}%</h3>
                <p>Success Rate</p>
              </div>
            </div>
            <div className="stat-card card">
              <div className="stat-icon" style={{ backgroundColor: 'rgba(245, 158, 11, 0.2)' }}>
                <Calendar size={24} color="var(--color-warning)" />
              </div>
              <div className="stat-content">
                <h3>{stats.avgDuration}s</h3>
                <p>Avg Duration</p>
              </div>
            </div>
          </div>

          {stats.timeline && stats.timeline.length > 0 && (
            <div className="chart-section card">
              <h2>Backup Frequency</h2>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={stats.timeline}>
                  <defs>
                    <linearGradient id="colorBackups" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="var(--color-primary)" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="var(--color-primary)" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                  <XAxis 
                    dataKey="date" 
                    stroke="var(--color-text-muted)"
                    tick={{ fill: 'var(--color-text-muted)' }}
                  />
                  <YAxis 
                    stroke="var(--color-text-muted)"
                    tick={{ fill: 'var(--color-text-muted)' }}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'var(--color-bg-primary)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)'
                    }}
                  />
                  <Area 
                    type="monotone" 
                    dataKey="count" 
                    stroke="var(--color-primary)" 
                    fillOpacity={1}
                    fill="url(#colorBackups)" 
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}

          {stats.sizeTimeline && stats.sizeTimeline.length > 0 && (
            <div className="chart-section card">
              <h2>Backup Size Over Time</h2>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={stats.sizeTimeline}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                  <XAxis 
                    dataKey="date" 
                    stroke="var(--color-text-muted)"
                    tick={{ fill: 'var(--color-text-muted)' }}
                  />
                  <YAxis 
                    stroke="var(--color-text-muted)"
                    tick={{ fill: 'var(--color-text-muted)' }}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'var(--color-bg-primary)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)'
                    }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="size" 
                    stroke="var(--color-success)" 
                    strokeWidth={2}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </>
      )}

      <div className="history-list card">
        <h2>Recent Activity</h2>
        <div className="history-items">
          {history.length === 0 ? (
            <div className="empty-state">
              <p>No history available</p>
            </div>
          ) : (
            history.map((entry) => (
              <div key={entry.id} className="history-item">
                <div className="history-time">
                  {format(new Date(entry.timestamp), 'MMM dd, HH:mm')}
                </div>
                <div className="history-content">
                  <div className="history-header">
                    <h4>{entry.action}</h4>
                    <span className={`badge badge-${entry.status}`}>
                      {entry.status}
                    </span>
                  </div>
                  <p className="history-description">{entry.description}</p>
                  {entry.details && (
                    <div className="history-details">
                      <span>Files: {entry.details.files}</span>
                      <span>Size: {formatBytes(entry.details.size)}</span>
                      <span>Duration: {entry.details.duration}s</span>
                    </div>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
