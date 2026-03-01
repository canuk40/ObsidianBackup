import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';
import authRoutes from './routes/auth.js';
import backupRoutes from './routes/backups.js';
import fileRoutes from './routes/files.js';
import historyRoutes from './routes/history.js';
import settingsRoutes from './routes/settings.js';
import deviceRoutes from './routes/device.js';

dotenv.config();

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: process.env.CLIENT_URL || 'http://localhost:3000',
    methods: ['GET', 'POST']
  }
});

const PORT = process.env.PORT || 8080;
const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  console.error('FATAL: JWT_SECRET environment variable is required. Generate with: openssl rand -base64 32');
  process.exit(1);
}

// Middleware
app.use(cors());
app.use(express.json());

// Authentication middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid token' });
    }
    req.user = user;
    next();
  });
};

// Store connected devices
const connectedDevices = new Map();

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/backups', authenticateToken, backupRoutes);
app.use('/api/history', authenticateToken, historyRoutes);
app.use('/api/settings', authenticateToken, settingsRoutes);
app.use('/api/device', authenticateToken, deviceRoutes);

// WebSocket authentication
io.use((socket, next) => {
  const token = socket.handshake.auth.token;
  if (!token) {
    return next(new Error('Authentication error'));
  }

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) {
      return next(new Error('Authentication error'));
    }
    socket.userId = decoded.userId;
    socket.deviceId = decoded.deviceId;
    next();
  });
});

// WebSocket connection handling
io.on('connection', (socket) => {
  console.log(`Device connected: ${socket.deviceId}`);
  connectedDevices.set(socket.deviceId, socket);

  socket.on('backup:start', (data) => {
    console.log('Backup started:', data);
    socket.emit('backup:acknowledged', { status: 'started' });
  });

  socket.on('backup:progress', (progress) => {
    socket.emit('backup:progress', progress);
  });

  socket.on('backup:complete', (data) => {
    console.log('Backup completed:', data);
    socket.emit('backup:complete', data);
  });

  socket.on('backup:error', (error) => {
    console.error('Backup error:', error);
    socket.emit('backup:error', error);
  });

  socket.on('disconnect', () => {
    console.log(`Device disconnected: ${socket.deviceId}`);
    connectedDevices.delete(socket.deviceId);
  });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy',
    connectedDevices: connectedDevices.size,
    timestamp: new Date().toISOString()
  });
});

// Export for use in routes
export { io, connectedDevices, JWT_SECRET };

httpServer.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`WebSocket server ready`);
});
