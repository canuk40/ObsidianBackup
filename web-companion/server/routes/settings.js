import express from 'express';
const router = express.Router();

let mockSettings = {
  backupFrequency: 'daily',
  autoDeleteOldBackups: false,
  retentionDays: 30,
  compressionLevel: 6,
  wifiOnly: true,
  cloudProvider: 'local',
  enableEncryption: false,
  notifyOnComplete: true,
  notifyOnFailure: true,
  dailySummary: false
};

router.get('/', (req, res) => {
  res.json(mockSettings);
});

router.put('/', (req, res) => {
  mockSettings = { ...mockSettings, ...req.body };
  res.json(mockSettings);
});

router.get('/cloud-providers', (req, res) => {
  res.json([
    { id: 'local', name: 'Local Storage', enabled: true },
    { id: 'webdav', name: 'WebDAV', enabled: true },
    { id: 'rclone', name: 'Rclone', enabled: true }
  ]);
});

router.post('/cloud-providers/:id/test', (req, res) => {
  res.json({ success: true, message: 'Connection successful' });
});

export default router;
