import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const authService = {
  login: async (email: string, password: string) => {
    const response = await api.post('/auth/login', { email, password })
    localStorage.setItem('token', response.data.token)
    return response.data
  },
  
  logout: () => {
    localStorage.removeItem('token')
  },
  
  initiateSAML: async (organizationId: string) => {
    const response = await api.post('/auth/saml/initiate', { organizationId })
    return response.data
  },
}

export const deviceService = {
  getAll: async () => {
    const response = await api.get('/devices')
    return response.data
  },
  
  getById: async (id: string) => {
    const response = await api.get(`/devices/${id}`)
    return response.data
  },
  
  wipe: async (deviceId: string, reason: string) => {
    const response = await api.post(`/devices/${deviceId}/wipe`, { deviceId, reason })
    return response.data
  },
  
  checkCompliance: async (deviceId: string) => {
    const response = await api.get(`/devices/${deviceId}/compliance`)
    return response.data
  },
}

export const policyService = {
  getAll: async () => {
    const response = await api.get('/policies')
    return response.data
  },
  
  create: async (policy: any) => {
    const response = await api.post('/policies', policy)
    return response.data
  },
  
  update: async (id: string, policy: any) => {
    const response = await api.put(`/policies/${id}`, policy)
    return response.data
  },
  
  delete: async (id: string) => {
    await api.delete(`/policies/${id}`)
  },
  
  enforce: async (id: string) => {
    const response = await api.post(`/policies/${id}/enforce`)
    return response.data
  },
}

export const auditService = {
  query: async (params: any) => {
    const response = await api.get('/audit', { params })
    return response.data
  },
  
  export: async (params: any) => {
    const response = await api.get('/audit/export', { params, responseType: 'blob' })
    return response.data
  },
}

export const reportService = {
  getDashboard: async () => {
    const response = await api.get('/reports/dashboard')
    return response.data
  },
  
  getBackupSuccessRates: async (days: number = 30) => {
    const response = await api.get('/reports/backup-success-rates', { params: { days } })
    return response.data
  },
  
  getStorageUsage: async () => {
    const response = await api.get('/reports/storage-usage')
    return response.data
  },
  
  getCompliance: async () => {
    const response = await api.get('/reports/compliance')
    return response.data
  },
}

export const rbacService = {
  getRoles: async () => {
    const response = await api.get('/rbac/roles')
    return response.data
  },
  
  createRole: async (role: any) => {
    const response = await api.post('/rbac/roles', role)
    return response.data
  },
  
  deleteRole: async (id: string) => {
    await api.delete(`/rbac/roles/${id}`)
  },
  
  getUserRoles: async (userId: string) => {
    const response = await api.get(`/rbac/users/${userId}/roles`)
    return response.data
  },
  
  assignRoles: async (userId: string, roleIds: string[]) => {
    const response = await api.post(`/rbac/users/${userId}/roles`, { userId, roleIds })
    return response.data
  },
  
  getPermissions: async () => {
    const response = await api.get('/rbac/permissions')
    return response.data
  },
}

export default api
