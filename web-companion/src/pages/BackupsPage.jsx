import React, { useEffect, useState } from 'react';
import { Database, Download, Trash2, RefreshCw, Search } from 'lucide-react';
import { backupApi } from '../services/api';
import { format } from 'date-fns';
import './BackupsPage.css';

export default function BackupsPage() {
  const [backups, setBackups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');

  useEffect(() => {
    loadBackups();
  }, []);

  const loadBackups = async () => {
    try {
      const data = await backupApi.getBackups();
      setBackups(data);
    } catch (error) {
      console.error('Failed to load backups:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRestore = async (backupId) => {
    if (!confirm('Are you sure you want to restore this backup?')) return;
    
    try {
      await backupApi.restoreBackup(backupId);
      alert('Restore initiated successfully');
    } catch (error) {
      alert('Failed to restore backup');
    }
  };

  const handleDelete = async (backupId) => {
    if (!confirm('Are you sure you want to delete this backup?')) return;
    
    try {
      await backupApi.deleteBackup(backupId);
      setBackups(backups.filter(b => b.id !== backupId));
    } catch (error) {
      alert('Failed to delete backup');
    }
  };

  const filteredBackups = backups.filter(backup => {
    const matchesSearch = backup.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = filterStatus === 'all' || backup.status === filterStatus;
    return matchesSearch && matchesFilter;
  });

  if (loading) {
    return <div className="page-loading">Loading backups...</div>;
  }

  return (
    <div className="backups-page">
      <div className="page-header">
        <h1>Backups</h1>
        <button onClick={loadBackups} className="btn btn-secondary">
          <RefreshCw size={18} />
          Refresh
        </button>
      </div>

      <div className="backups-filters card">
        <div className="search-box">
          <Search size={18} />
          <input
            type="text"
            placeholder="Search backups..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input"
          />
        </div>
        <div className="filter-buttons">
          {['all', 'success', 'failed', 'pending'].map(status => (
            <button
              key={status}
              className={`filter-btn ${filterStatus === status ? 'active' : ''}`}
              onClick={() => setFilterStatus(status)}
            >
              {status.charAt(0).toUpperCase() + status.slice(1)}
            </button>
          ))}
        </div>
      </div>

      <div className="backups-grid">
        {filteredBackups.length === 0 ? (
          <div className="empty-state card">
            <Database size={48} color="var(--color-text-muted)" />
            <p>No backups found</p>
          </div>
        ) : (
          filteredBackups.map((backup) => (
            <div key={backup.id} className="backup-card card">
              <div className="backup-card-header">
                <h3>{backup.name}</h3>
                <span className={`badge badge-${backup.status}`}>
                  {backup.status}
                </span>
              </div>
              <div className="backup-card-body">
                <div className="backup-info-row">
                  <span>Date:</span>
                  <span>{format(new Date(backup.timestamp), 'PPp')}</span>
                </div>
                <div className="backup-info-row">
                  <span>Size:</span>
                  <span>{formatBytes(backup.size)}</span>
                </div>
                <div className="backup-info-row">
                  <span>Files:</span>
                  <span>{backup.fileCount} files</span>
                </div>
                <div className="backup-info-row">
                  <span>Type:</span>
                  <span>{backup.type}</span>
                </div>
              </div>
              <div className="backup-card-actions">
                <button
                  onClick={() => handleRestore(backup.id)}
                  className="btn btn-primary"
                  disabled={backup.status !== 'success'}
                >
                  <Download size={16} />
                  Restore
                </button>
                <button
                  onClick={() => handleDelete(backup.id)}
                  className="btn btn-danger"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))
        )}
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
