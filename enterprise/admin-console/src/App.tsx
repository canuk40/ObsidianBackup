import { Routes, Route, Navigate } from 'react-router-dom'
import { useState } from 'react'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Devices from './pages/Devices'
import Policies from './pages/Policies'
import AuditLogs from './pages/AuditLogs'
import Reports from './pages/Reports'
import RBAC from './pages/RBAC'
import Settings from './pages/Settings'

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(
    !!localStorage.getItem('token')
  )

  if (!isAuthenticated) {
    return <Login onLogin={() => setIsAuthenticated(true)} />
  }

  return (
    <Layout onLogout={() => setIsAuthenticated(false)}>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/devices" element={<Devices />} />
        <Route path="/policies" element={<Policies />} />
        <Route path="/audit" element={<AuditLogs />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/rbac" element={<RBAC />} />
        <Route path="/settings" element={<Settings />} />
      </Routes>
    </Layout>
  )
}

export default App
