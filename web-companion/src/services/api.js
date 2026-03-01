import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('obsidianbackup-auth');
  if (token) {
    const auth = JSON.parse(token);
    config.headers.Authorization = `Bearer ${auth.state.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('obsidianbackup-auth');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  pairDevice: async (pairingCode) => {
    const response = await api.post('/auth/pair', { pairingCode });
    return response.data;
  },
  
  verifyToken: async () => {
    const response = await api.get('/auth/verify');
    return response.data;
  },
  
  revokeAccess: async () => {
    const response = await api.post('/auth/revoke');
    return response.data;
  }
};

export const backupApi = {
  getBackups: async (filters = {}) => {
    const response = await api.get('/backups', { params: filters });
    return response.data;
  },
  
  getBackup: async (backupId) => {
    const response = await api.get(`/backups/${backupId}`);
    return response.data;
  },
  
  triggerBackup: async (options = {}) => {
    const response = await api.post('/backups/trigger', options);
    return response.data;
  },
  
  deleteBackup: async (backupId) => {
    const response = await api.delete(`/backups/${backupId}`);
    return response.data;
  },
  
  restoreBackup: async (backupId, options = {}) => {
    const response = await api.post(`/backups/${backupId}/restore`, options);
    return response.data;
  },
  
  getBackupStats: async () => {
    const response = await api.get('/backups/stats');
    return response.data;
  }
};

export const filesApi = {
  listFiles: async (backupId, path = '/') => {
    const response = await api.get(`/backups/${backupId}/files`, {
      params: { path }
    });
    return response.data;
  },
  
  downloadFile: async (backupId, filePath) => {
    const response = await api.get(`/backups/${backupId}/files/download`, {
      params: { path: filePath },
      responseType: 'blob'
    });
    return response.data;
  },
  
  searchFiles: async (backupId, query) => {
    const response = await api.get(`/backups/${backupId}/files/search`, {
      params: { q: query }
    });
    return response.data;
  }
};

export const historyApi = {
  getHistory: async (filters = {}) => {
    const response = await api.get('/history', { params: filters });
    return response.data;
  },
  
  getHistoryEntry: async (entryId) => {
    const response = await api.get(`/history/${entryId}`);
    return response.data;
  },
  
  getHistoryStats: async (period = '30d') => {
    const response = await api.get('/history/stats', {
      params: { period }
    });
    return response.data;
  }
};

export const settingsApi = {
  getSettings: async () => {
    const response = await api.get('/settings');
    return response.data;
  },
  
  updateSettings: async (settings) => {
    const response = await api.put('/settings', settings);
    return response.data;
  },
  
  getCloudProviders: async () => {
    const response = await api.get('/settings/cloud-providers');
    return response.data;
  },
  
  testConnection: async (providerId) => {
    const response = await api.post(`/settings/cloud-providers/${providerId}/test`);
    return response.data;
  }
};

export const deviceApi = {
  getDeviceInfo: async () => {
    const response = await api.get('/device/info');
    return response.data;
  },
  
  getDeviceStatus: async () => {
    const response = await api.get('/device/status');
    return response.data;
  },
  
  getStorageInfo: async () => {
    const response = await api.get('/device/storage');
    return response.data;
  }
};

export default api;
