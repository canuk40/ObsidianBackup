import express from 'express';
import jwt from 'jsonwebtoken';
import { JWT_SECRET } from '../index.js';

const router = express.Router();

// Store for pending pairings (in production, use Redis or database)
const pairingRequests = new Map();

// Pair device with QR code or token
router.post('/pair', async (req, res) => {
  const { pairingCode } = req.body;

  if (!pairingCode) {
    return res.status(400).json({ error: 'Pairing code required' });
  }

  // Check if pairing request exists (simulated)
  // In production, the Android app would create this request
  const deviceInfo = {
    deviceId: `device_${Date.now()}`,
    deviceName: 'Android Device',
    paired: true
  };

  // Generate JWT token
  const token = jwt.sign(
    { 
      userId: 'user_1',
      deviceId: deviceInfo.deviceId,
      deviceName: deviceInfo.deviceName
    },
    JWT_SECRET,
    { expiresIn: '30d' }
  );

  res.json({
    token,
    deviceId: deviceInfo.deviceId,
    deviceName: deviceInfo.deviceName
  });
});

// Verify token
router.get('/verify', (req, res) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid token' });
    }
    res.json({ valid: true, user: decoded });
  });
});

// Revoke access
router.post('/revoke', (req, res) => {
  // In production, add token to blacklist
  res.json({ success: true });
});

export default router;
