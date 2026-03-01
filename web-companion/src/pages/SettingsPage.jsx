import React, { useEffect, useState } from 'react';
import { Save, Settings as SettingsIcon, Cloud, Shield, Bell } from 'lucide-react';
import { settingsApi } from '../services/api';
import './SettingsPage.css';

export default function SettingsPage() {
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState('general');

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const data = await settingsApi.getSettings();
      setSettings(data);
    } catch (error) {
      console.error('Failed to load settings:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await settingsApi.updateSettings(settings);
      alert('Settings saved successfully');
    } catch (error) {
      alert('Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  const updateSetting = (key, value) => {
    setSettings(prev => ({ ...prev, [key]: value }));
  };

  if (loading) {
    return <div className="page-loading">Loading settings...</div>;
  }

  return (
    <div className="settings-page">
      <div className="page-header">
        <h1>Settings</h1>
        <button onClick={handleSave} disabled={saving} className="btn btn-primary">
          <Save size={18} />
          {saving ? 'Saving...' : 'Save Changes'}
        </button>
      </div>

      <div className="settings-container">
        <div className="settings-tabs">
          <button
            className={`tab-btn ${activeTab === 'general' ? 'active' : ''}`}
            onClick={() => setActiveTab('general')}
          >
            <SettingsIcon size={18} />
            General
          </button>
          <button
            className={`tab-btn ${activeTab === 'cloud' ? 'active' : ''}`}
            onClick={() => setActiveTab('cloud')}
          >
            <Cloud size={18} />
            Cloud Storage
          </button>
          <button
            className={`tab-btn ${activeTab === 'security' ? 'active' : ''}`}
            onClick={() => setActiveTab('security')}
          >
            <Shield size={18} />
            Security
          </button>
          <button
            className={`tab-btn ${activeTab === 'notifications' ? 'active' : ''}`}
            onClick={() => setActiveTab('notifications')}
          >
            <Bell size={18} />
            Notifications
          </button>
        </div>

        <div className="settings-content card">
          {activeTab === 'general' && (
            <div className="settings-section">
              <h2>General Settings</h2>
              
              <div className="setting-group">
                <label>Backup Frequency</label>
                <select
                  value={settings.backupFrequency || 'daily'}
                  onChange={(e) => updateSetting('backupFrequency', e.target.value)}
                  className="input"
                >
                  <option value="manual">Manual Only</option>
                  <option value="hourly">Hourly</option>
                  <option value="daily">Daily</option>
                  <option value="weekly">Weekly</option>
                </select>
              </div>

              <div className="setting-group">
                <label>Auto-Delete Old Backups</label>
                <div className="checkbox-group">
                  <input
                    type="checkbox"
                    checked={settings.autoDeleteOldBackups || false}
                    onChange={(e) => updateSetting('autoDeleteOldBackups', e.target.checked)}
                  />
                  <span>Delete backups older than</span>
                  <input
                    type="number"
                    value={settings.retentionDays || 30}
                    onChange={(e) => updateSetting('retentionDays', parseInt(e.target.value))}
                    className="input"
                    style={{ width: '80px' }}
                    disabled={!settings.autoDeleteOldBackups}
                  />
                  <span>days</span>
                </div>
              </div>

              <div className="setting-group">
                <label>Compression Level</label>
                <input
                  type="range"
                  min="0"
                  max="9"
                  value={settings.compressionLevel || 6}
                  onChange={(e) => updateSetting('compressionLevel', parseInt(e.target.value))}
                />
                <span className="range-value">{settings.compressionLevel || 6}</span>
              </div>

              <div className="setting-group">
                <label>Backup on WiFi Only</label>
                <input
                  type="checkbox"
                  checked={settings.wifiOnly || false}
                  onChange={(e) => updateSetting('wifiOnly', e.target.checked)}
                />
              </div>
            </div>
          )}

          {activeTab === 'cloud' && (
            <div className="settings-section">
              <h2>Cloud Storage</h2>
              
              <div className="setting-group">
                <label>Cloud Provider</label>
                <select
                  value={settings.cloudProvider || 'local'}
                  onChange={(e) => updateSetting('cloudProvider', e.target.value)}
                  className="input"
                >
                  <option value="local">Local Storage</option>
                  <option value="webdav">WebDAV</option>
                  <option value="rclone">Rclone</option>
                </select>
              </div>

              {settings.cloudProvider === 'webdav' && (
                <>
                  <div className="setting-group">
                    <label>WebDAV URL</label>
                    <input
                      type="url"
                      value={settings.webdavUrl || ''}
                      onChange={(e) => updateSetting('webdavUrl', e.target.value)}
                      placeholder="https://example.com/webdav"
                      className="input"
                    />
                  </div>
                  <div className="setting-group">
                    <label>Username</label>
                    <input
                      type="text"
                      value={settings.webdavUsername || ''}
                      onChange={(e) => updateSetting('webdavUsername', e.target.value)}
                      className="input"
                    />
                  </div>
                  <div className="setting-group">
                    <label>Password</label>
                    <input
                      type="password"
                      value={settings.webdavPassword || ''}
                      onChange={(e) => updateSetting('webdavPassword', e.target.value)}
                      className="input"
                    />
                  </div>
                </>
              )}
            </div>
          )}

          {activeTab === 'security' && (
            <div className="settings-section">
              <h2>Security Settings</h2>
              
              <div className="setting-group">
                <label>Enable Encryption</label>
                <input
                  type="checkbox"
                  checked={settings.enableEncryption || false}
                  onChange={(e) => updateSetting('enableEncryption', e.target.checked)}
                />
              </div>

              {settings.enableEncryption && (
                <>
                  <div className="setting-group">
                    <label>Encryption Method</label>
                    <select
                      value={settings.encryptionMethod || 'aes256'}
                      onChange={(e) => updateSetting('encryptionMethod', e.target.value)}
                      className="input"
                    >
                      <option value="aes128">AES-128</option>
                      <option value="aes256">AES-256</option>
                    </select>
                  </div>
                  <div className="setting-group">
                    <label>Encryption Password</label>
                    <input
                      type="password"
                      value={settings.encryptionPassword || ''}
                      onChange={(e) => updateSetting('encryptionPassword', e.target.value)}
                      className="input"
                    />
                  </div>
                </>
              )}

              <div className="setting-group">
                <label>Require Biometric Authentication</label>
                <input
                  type="checkbox"
                  checked={settings.requireBiometric || false}
                  onChange={(e) => updateSetting('requireBiometric', e.target.checked)}
                />
              </div>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div className="settings-section">
              <h2>Notification Settings</h2>
              
              <div className="setting-group">
                <label>Backup Completion Notifications</label>
                <input
                  type="checkbox"
                  checked={settings.notifyOnComplete || false}
                  onChange={(e) => updateSetting('notifyOnComplete', e.target.checked)}
                />
              </div>

              <div className="setting-group">
                <label>Backup Failure Notifications</label>
                <input
                  type="checkbox"
                  checked={settings.notifyOnFailure || false}
                  onChange={(e) => updateSetting('notifyOnFailure', e.target.checked)}
                />
              </div>

              <div className="setting-group">
                <label>Daily Summary</label>
                <input
                  type="checkbox"
                  checked={settings.dailySummary || false}
                  onChange={(e) => updateSetting('dailySummary', e.target.checked)}
                />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
