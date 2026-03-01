import React, { useEffect, useState } from 'react';
import { FolderOpen, File, Download, ChevronRight, Home, Search } from 'lucide-react';
import { filesApi, backupApi } from '../services/api';
import './FileBrowserPage.css';

export default function FileBrowserPage() {
  const [backups, setBackups] = useState([]);
  const [selectedBackup, setSelectedBackup] = useState(null);
  const [currentPath, setCurrentPath] = useState('/');
  const [files, setFiles] = useState([]);
  const [breadcrumbs, setBreadcrumbs] = useState(['/']);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    loadBackups();
  }, []);

  useEffect(() => {
    if (selectedBackup) {
      loadFiles(currentPath);
    }
  }, [selectedBackup, currentPath]);

  const loadBackups = async () => {
    try {
      const data = await backupApi.getBackups({ status: 'success' });
      setBackups(data);
      if (data.length > 0) {
        setSelectedBackup(data[0].id);
      }
    } catch (error) {
      console.error('Failed to load backups:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadFiles = async (path) => {
    if (!selectedBackup) return;
    
    try {
      const data = await filesApi.listFiles(selectedBackup, path);
      setFiles(data);
      updateBreadcrumbs(path);
    } catch (error) {
      console.error('Failed to load files:', error);
    }
  };

  const updateBreadcrumbs = (path) => {
    if (path === '/') {
      setBreadcrumbs(['/']);
    } else {
      const parts = path.split('/').filter(Boolean);
      const crumbs = ['/'];
      let current = '';
      parts.forEach(part => {
        current += '/' + part;
        crumbs.push(current);
      });
      setBreadcrumbs(crumbs);
    }
  };

  const handleFolderClick = (folder) => {
    const newPath = currentPath === '/' 
      ? '/' + folder.name 
      : currentPath + '/' + folder.name;
    setCurrentPath(newPath);
  };

  const handleBreadcrumbClick = (index) => {
    setCurrentPath(breadcrumbs[index]);
  };

  const handleDownload = async (file) => {
    try {
      const blob = await filesApi.downloadFile(selectedBackup, file.path);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.name;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      alert('Failed to download file');
    }
  };

  const handleSearch = async () => {
    if (!searchQuery || !selectedBackup) return;
    
    try {
      const results = await filesApi.searchFiles(selectedBackup, searchQuery);
      setFiles(results);
    } catch (error) {
      alert('Search failed');
    }
  };

  if (loading) {
    return <div className="page-loading">Loading...</div>;
  }

  if (backups.length === 0) {
    return (
      <div className="empty-state card">
        <FolderOpen size={48} color="var(--color-text-muted)" />
        <p>No backups available</p>
      </div>
    );
  }

  return (
    <div className="file-browser-page">
      <div className="page-header">
        <h1>File Browser</h1>
        <select
          value={selectedBackup || ''}
          onChange={(e) => {
            setSelectedBackup(e.target.value);
            setCurrentPath('/');
          }}
          className="backup-selector"
        >
          {backups.map(backup => (
            <option key={backup.id} value={backup.id}>
              {backup.name} - {new Date(backup.timestamp).toLocaleString()}
            </option>
          ))}
        </select>
      </div>

      <div className="file-browser-toolbar card">
        <div className="breadcrumbs">
          <Home 
            size={18} 
            onClick={() => setCurrentPath('/')}
            style={{ cursor: 'pointer' }}
          />
          {breadcrumbs.map((crumb, index) => (
            <React.Fragment key={index}>
              {index > 0 && <ChevronRight size={16} />}
              {index > 0 && (
                <span
                  className="breadcrumb"
                  onClick={() => handleBreadcrumbClick(index)}
                >
                  {crumb.split('/').filter(Boolean).pop()}
                </span>
              )}
            </React.Fragment>
          ))}
        </div>
        <div className="search-box">
          <input
            type="text"
            placeholder="Search files..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="input"
          />
          <button onClick={handleSearch} className="btn btn-primary">
            <Search size={16} />
          </button>
        </div>
      </div>

      <div className="file-list card">
        {files.length === 0 ? (
          <div className="empty-folder">
            <p>This folder is empty</p>
          </div>
        ) : (
          <div className="file-items">
            {files.map((item, index) => (
              <div
                key={index}
                className={`file-item ${item.type === 'directory' ? 'folder' : 'file'}`}
                onClick={() => item.type === 'directory' && handleFolderClick(item)}
              >
                <div className="file-item-info">
                  {item.type === 'directory' ? (
                    <FolderOpen size={20} color="var(--color-warning)" />
                  ) : (
                    <File size={20} color="var(--color-info)" />
                  )}
                  <span className="file-name">{item.name}</span>
                </div>
                <div className="file-item-meta">
                  {item.type === 'file' && (
                    <>
                      <span className="file-size">{formatBytes(item.size)}</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDownload(item);
                        }}
                        className="btn btn-sm btn-secondary"
                      >
                        <Download size={14} />
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
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
