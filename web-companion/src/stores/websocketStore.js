import { create } from 'zustand';
import { io } from 'socket.io-client';

export const useWebSocketStore = create((set, get) => ({
  socket: null,
  isConnected: false,
  backupProgress: null,
  notifications: [],
  
  connect: (token) => {
    // Server URL is read from the VITE_WS_URL environment variable.
    // Development: copy .env.example → .env.local and set VITE_WS_URL=ws://localhost:8080
    // Production:  set VITE_WS_URL=wss://your-server-address:8080 (TLS required)
    const wsUrl = import.meta.env.VITE_WS_URL;
    if (!wsUrl) {
      console.error(
        '[WebSocket] VITE_WS_URL is not configured. ' +
        'Copy web-companion/.env.example → .env.local and set VITE_WS_URL.'
      );
      return;
    }
    const socket = io(wsUrl, {
      auth: { token },
      transports: ['websocket']
    });
    
    socket.on('connect', () => {
      console.log('WebSocket connected');
      set({ isConnected: true });
    });
    
    socket.on('disconnect', () => {
      console.log('WebSocket disconnected');
      set({ isConnected: false, backupProgress: null });
    });
    
    socket.on('backup:progress', (progress) => {
      set({ backupProgress: progress });
    });
    
    socket.on('backup:complete', (data) => {
      set({ backupProgress: null });
      get().addNotification({
        type: 'success',
        message: 'Backup completed successfully',
        data
      });
    });
    
    socket.on('backup:error', (error) => {
      set({ backupProgress: null });
      get().addNotification({
        type: 'error',
        message: `Backup failed: ${error.message}`,
        data: error
      });
    });
    
    socket.on('notification', (notification) => {
      get().addNotification(notification);
    });
    
    set({ socket });
  },
  
  disconnect: () => {
    const { socket } = get();
    if (socket) {
      socket.disconnect();
      set({ socket: null, isConnected: false, backupProgress: null });
    }
  },
  
  addNotification: (notification) => {
    set((state) => ({
      notifications: [
        ...state.notifications,
        { ...notification, id: Date.now(), timestamp: new Date() }
      ]
    }));
    
    setTimeout(() => {
      get().removeNotification(notification.id || Date.now());
    }, 5000);
  },
  
  removeNotification: (id) => {
    set((state) => ({
      notifications: state.notifications.filter(n => n.id !== id)
    }));
  },
  
  clearNotifications: () => {
    set({ notifications: [] });
  }
}));
