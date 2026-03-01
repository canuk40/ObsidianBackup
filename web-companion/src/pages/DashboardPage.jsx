import React, { useEffect, useState } from 'react';
import { Database, HardDrive, Clock, TrendingUp, Play } from 'lucide-react';
import { backupApi, deviceApi } from '../services/api';
import { useWebSocketStore } from '../stores/websocketStore';
import { formatDistance } from 'date-fns';
import './DashboardPage.css';

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [deviceInfo, setDeviceInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const { backupProgress } = useWebSocketStore();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const [statsData, deviceData] = await Promise.all([
        backupApi.getBackupStats(),
        deviceApi.getDeviceInfo()
      ]);
      setStats(statsData);
      setDeviceInfo(deviceData);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleTriggerBackup = async () => {
    try {
      await backupApi.triggerBackup();
    } catch (error) {
      console.error('Failed to trigger backup:', error);
    }
  };

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="animate-spin" style={{ fontSize: '2rem' }}>⏳</div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <button onClick={handleTriggerBackup} className="btn btn-primary">
          <Play size={18} />
          Trigger Backup
        </button>
      </div>

      {backupProgress && (
        <div className="backup-progress-card card">
          <h3>Backup in Progress</h3>
          <div className="progress-info">
            <div className="progress-bar">
              <div
                className="progress-fill"
                style={{ width: `${backupProgress.percentage}%` }}
              />
            </div>
            <div className="progress-details">
              <span>{backupProgress.currentFile}</span>
              <span>{backupProgress.percentage}%</span>
            </div>
            <div className="progress-stats">
              <span>{backupProgress.processedFiles} / {backupProgress.totalFiles} files</span>
              <span>{formatBytes(backupProgress.processedBytes)} / {formatBytes(backupProgress.totalBytes)}</span>
            </div>
          </div>
        </div>
      )}

      <div className="stats-grid">
        <div className="stat-card card">
          <div className="stat-icon" style={{ backgroundColor: 'rgba(99, 102, 241, 0.2)' }}>
            <Database size={24} color="var(--color-primary)" />
          </div>
          <div className="stat-content">
            <h3>{stats?.totalBackups || 0}</h3>
            <p>Total Backups</p>
          </div>
        </div>

        <div className="stat-card card">
          <div className="stat-icon" style={{ backgroundColor: 'rgba(16, 185, 129, 0.2)' }}>
            <HardDrive size={24} color="var(--color-success)" />
          </div>
          <div className="stat-content">
            <h3>{formatBytes(stats?.totalSize || 0)}</h3>
            <p>Total Size</p>
          </div>
        </div>

        <div className="stat-card card">
          <div className="stat-icon" style={{ backgroundColor: 'rgba(245, 158, 11, 0.2)' }}>
            <Clock size={24} color="var(--color-warning)" />
          </div>
          <div className="stat-content">
            <h3>
              {stats?.lastBackup 
                ? formatDistance(new Date(stats.lastBackup), new Date(), { addSuffix: true })
                : 'Never'}
            </h3>
            <p>Last Backup</p>
          </div>
        </div>

        <div className="stat-card card">
          <div className="stat-icon" style={{ backgroundColor: 'rgba(139, 92, 246, 0.2)' }}>
            <TrendingUp size={24} color="var(--color-secondary)" />
          </div>
          <div className="stat-content">
            <h3>{stats?.successRate || 0}%</h3>
            <p>Success Rate</p>
          </div>
        </div>
      </div>

      {deviceInfo && (
        <div className="device-info-section">
          <h2>Device Information</h2>
          <div className="card">
            <div className="device-info-grid">
              <div className="info-item">
                <span className="info-label">Device Name</span>
                <span className="info-value">{deviceInfo.name}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Model</span>
                <span className="info-value">{deviceInfo.model}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Android Version</span>
                <span className="info-value">{deviceInfo.androidVersion}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Storage Used</span>
                <span className="info-value">
                  {formatBytes(deviceInfo.storageUsed)} / {formatBytes(deviceInfo.storageTotal)}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {stats?.recentBackups && stats.recentBackups.length > 0 && (
        <div className="recent-backups-section">
          <h2>Recent Backups</h2>
          <div className="card">
            <div className="backups-list">
              {stats.recentBackups.map((backup) => (
                <div key={backup.id} className="backup-item">
                  <div className="backup-info">
                    <h4>{backup.name}</h4>
                    <p className="backup-date">
                      {formatDistance(new Date(backup.timestamp), new Date(), { addSuffix: true })}
                    </p>
                  </div>
                  <div className="backup-meta">
                    <span className={`badge badge-${backup.status}`}>
                      {backup.status}
                    </span>
                    <span className="backup-size">{formatBytes(backup.size)}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
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
