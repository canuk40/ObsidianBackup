import express from 'express';
const router = express.Router();

router.get('/info', (req, res) => {
  res.json({
    name: 'Samsung Galaxy S21',
    model: 'SM-G991B',
    androidVersion: '13',
    storageTotal: 128 * 1024 * 1024 * 1024,
    storageUsed: 64 * 1024 * 1024 * 1024
  });
});

router.get('/status', (req, res) => {
  res.json({
    online: true,
    battery: 85,
    charging: false,
    wifiConnected: true,
    lastSeen: new Date().toISOString()
  });
});

router.get('/storage', (req, res) => {
  res.json({
    total: 128 * 1024 * 1024 * 1024,
    used: 64 * 1024 * 1024 * 1024,
    free: 64 * 1024 * 1024 * 1024,
    backups: 10 * 1024 * 1024 * 1024
  });
});

export default router;
