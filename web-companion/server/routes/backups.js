import express from 'express';
import { io, connectedDevices } from '../index.js';

const router = express.Router();

// Mock data (in production, fetch from Android device or database)
const mockBackups = [
  {
    id: '1',
    name: 'Full Backup - 2024-02-08',
    timestamp: new Date('2024-02-08T10:30:00').toISOString(),
    status: 'success',
    size: 1024 * 1024 * 150, // 150 MB
    fileCount: 234,
    type: 'full'
  },
  {
    id: '2',
    name: 'Incremental Backup - 2024-02-07',
    timestamp: new Date('2024-02-07T10:30:00').toISOString(),
    status: 'success',
    size: 1024 * 1024 * 45, // 45 MB
    fileCount: 67,
    type: 'incremental'
  }
];

// Get all backups
router.get('/', (req, res) => {
  const { status } = req.query;
  let filtered = mockBackups;
  
  if (status && status !== 'all') {
    filtered = mockBackups.filter(b => b.status === status);
  }
  
  res.json(filtered);
});

// Get single backup
router.get('/:id', (req, res) => {
  const backup = mockBackups.find(b => b.id === req.params.id);
  
  if (!backup) {
    return res.status(404).json({ error: 'Backup not found' });
  }
  
  res.json(backup);
});

// Trigger new backup
router.post('/trigger', (req, res) => {
  const socket = connectedDevices.get(req.user.deviceId);
  
  if (!socket) {
    return res.status(503).json({ error: 'Device not connected' });
  }
  
  // Send backup command to Android device via WebSocket
  socket.emit('backup:trigger', req.body);
  
  res.json({ 
    success: true,
    message: 'Backup triggered successfully'
  });
});

// Delete backup
router.delete('/:id', (req, res) => {
  const index = mockBackups.findIndex(b => b.id === req.params.id);
  
  if (index === -1) {
    return res.status(404).json({ error: 'Backup not found' });
  }
  
  mockBackups.splice(index, 1);
  res.json({ success: true });
});

// Restore backup
router.post('/:id/restore', (req, res) => {
  const backup = mockBackups.find(b => b.id === req.params.id);
  
  if (!backup) {
    return res.status(404).json({ error: 'Backup not found' });
  }
  
  const socket = connectedDevices.get(req.user.deviceId);
  
  if (!socket) {
    return res.status(503).json({ error: 'Device not connected' });
  }
  
  socket.emit('backup:restore', { backupId: req.params.id, ...req.body });
  
  res.json({ 
    success: true,
    message: 'Restore initiated'
  });
});

// Get backup statistics
router.get('/stats', (req, res) => {
  const totalBackups = mockBackups.length;
  const totalSize = mockBackups.reduce((sum, b) => sum + b.size, 0);
  const lastBackup = mockBackups.length > 0 
    ? mockBackups[0].timestamp 
    : null;
  const successCount = mockBackups.filter(b => b.status === 'success').length;
  const successRate = totalBackups > 0 
    ? Math.round((successCount / totalBackups) * 100) 
    : 0;
  
  res.json({
    totalBackups,
    totalSize,
    lastBackup,
    successRate,
    recentBackups: mockBackups.slice(0, 5)
  });
});

// Get backup files
router.get('/:id/files', (req, res) => {
  const { path = '/' } = req.query;
  
  // Mock file structure
  const files = [
    { name: 'Documents', type: 'directory', path: '/Documents' },
    { name: 'Pictures', type: 'directory', path: '/Pictures' },
    { name: 'config.txt', type: 'file', size: 1024, path: '/config.txt' },
    { name: 'data.json', type: 'file', size: 2048, path: '/data.json' }
  ];
  
  res.json(files);
});

export default router;
