import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useWebSocketStore } from '../stores/websocketStore';
import { 
  Home, Database, FolderOpen, Clock, Settings, 
  LogOut, Wifi, WifiOff, Bell, X 
} from 'lucide-react';
import './Layout.css';

export default function Layout() {
  const navigate = useNavigate();
  const { deviceName, clearAuth } = useAuthStore();
  const { isConnected, notifications, removeNotification } = useWebSocketStore();
  
  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };
  
  const navItems = [
    { path: '/dashboard', icon: Home, label: 'Dashboard' },
    { path: '/backups', icon: Database, label: 'Backups' },
    { path: '/files', icon: FolderOpen, label: 'Files' },
    { path: '/history', icon: Clock, label: 'History' },
    { path: '/settings', icon: Settings, label: 'Settings' }
  ];
  
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1 className="sidebar-title">ObsidianBackup</h1>
          <div className="device-status">
            <span className="device-name">{deviceName || 'Unknown Device'}</span>
            <div className="connection-status">
              {isConnected ? (
                <><Wifi size={14} /> Connected</>
              ) : (
                <><WifiOff size={14} /> Disconnected</>
              )}
            </div>
          </div>
        </div>
        
        <nav className="sidebar-nav">
          {navItems.map(({ path, icon: Icon, label }) => (
            <NavLink
              key={path}
              to={path}
              className={({ isActive }) => 
                `nav-item ${isActive ? 'active' : ''}`
              }
            >
              <Icon size={20} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        
        <div className="sidebar-footer">
          <button onClick={handleLogout} className="btn btn-secondary logout-btn">
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>
      
      <main className="main-content">
        <Outlet />
      </main>
      
      {notifications.length > 0 && (
        <div className="notifications">
          {notifications.map((notification) => (
            <div
              key={notification.id}
              className={`notification notification-${notification.type}`}
            >
              <div className="notification-content">
                <Bell size={18} />
                <span>{notification.message}</span>
              </div>
              <button
                onClick={() => removeNotification(notification.id)}
                className="notification-close"
              >
                <X size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
