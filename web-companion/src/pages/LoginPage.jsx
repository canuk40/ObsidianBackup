import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { useAuthStore } from '../stores/authStore';
import { authApi } from '../services/api';
import { Smartphone, Key, Loader } from 'lucide-react';
import './LoginPage.css';

export default function LoginPage() {
  const navigate = useNavigate();
  const { setAuth, isAuthenticated } = useAuthStore();
  const [mode, setMode] = useState('qr'); // 'qr' or 'token'
  const [pairingCode, setPairingCode] = useState('');
  const [tokenInput, setTokenInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [qrData, setQrData] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (mode === 'qr') {
      generatePairingCode();
    }
  }, [mode]);

  const generatePairingCode = () => {
    const code = Math.random().toString(36).substring(2, 10).toUpperCase();
    setPairingCode(code);
    setQrData(JSON.stringify({
      type: 'obsidianbackup-pairing',
      code,
      timestamp: Date.now()
    }));
  };

  const handleQrPairing = async () => {
    setLoading(true);
    setError('');
    
    try {
      const response = await authApi.pairDevice(pairingCode);
      setAuth(response.token, response.deviceId, response.deviceName);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Pairing failed');
      generatePairingCode();
    } finally {
      setLoading(false);
    }
  };

  const handleTokenPairing = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      const response = await authApi.pairDevice(tokenInput);
      setAuth(response.token, response.deviceId, response.deviceName);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid token');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-header">
          <h1>ObsidianBackup Companion</h1>
          <p>Connect to your Android device</p>
        </div>

        <div className="login-mode-selector">
          <button
            className={`mode-btn ${mode === 'qr' ? 'active' : ''}`}
            onClick={() => setMode('qr')}
          >
            <Smartphone size={20} />
            QR Code
          </button>
          <button
            className={`mode-btn ${mode === 'token' ? 'active' : ''}`}
            onClick={() => setMode('token')}
          >
            <Key size={20} />
            Token
          </button>
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {mode === 'qr' ? (
          <div className="qr-section">
            <div className="qr-code-container">
              <QRCodeSVG
                value={qrData}
                size={240}
                level="H"
                includeMargin
              />
            </div>
            <div className="pairing-code">
              <span>Pairing Code:</span>
              <strong>{pairingCode}</strong>
            </div>
            <p className="instruction">
              Scan this QR code or enter the pairing code in your ObsidianBackup Android app
            </p>
            <button
              onClick={handleQrPairing}
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? (
                <><Loader className="animate-spin" size={18} /> Waiting...</>
              ) : (
                'Check Connection'
              )}
            </button>
            <button
              onClick={generatePairingCode}
              disabled={loading}
              className="btn btn-secondary"
              style={{ marginTop: '0.5rem' }}
            >
              Refresh Code
            </button>
          </div>
        ) : (
          <div className="token-section">
            <form onSubmit={handleTokenPairing}>
              <div className="form-group">
                <label htmlFor="token">Access Token</label>
                <input
                  id="token"
                  type="text"
                  value={tokenInput}
                  onChange={(e) => setTokenInput(e.target.value)}
                  placeholder="Enter your access token"
                  className="input"
                  required
                />
              </div>
              <p className="instruction">
                Enter the access token from your ObsidianBackup app settings
              </p>
              <button
                type="submit"
                disabled={loading || !tokenInput}
                className="btn btn-primary"
              >
                {loading ? (
                  <><Loader className="animate-spin" size={18} /> Connecting...</>
                ) : (
                  'Connect'
                )}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
