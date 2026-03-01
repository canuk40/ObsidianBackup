import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import { useWebSocketStore } from './stores/websocketStore';

// Pages
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import BackupsPage from './pages/BackupsPage';
import FileBrowserPage from './pages/FileBrowserPage';
import HistoryPage from './pages/HistoryPage';
import SettingsPage from './pages/SettingsPage';

// Components
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  const { isAuthenticated, token, initialize } = useAuthStore();
  const { connect, disconnect } = useWebSocketStore();

  useEffect(() => {
    initialize();
  }, [initialize]);

  useEffect(() => {
    if (isAuthenticated && token) {
      connect(token);
    } else {
      disconnect();
    }
  }, [isAuthenticated, token, connect, disconnect]);

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="backups" element={<BackupsPage />} />
          <Route path="files" element={<FileBrowserPage />} />
          <Route path="history" element={<HistoryPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
