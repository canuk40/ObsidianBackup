# ObsidianBackup Chromebook/Desktop Companion - Complete Guide

## Overview

The ObsidianBackup Chromebook/Desktop Companion is a full-featured progressive web application (PWA) that provides desktop access to your Android backup system. It enables remote backup management, file browsing, real-time progress monitoring, and comprehensive settings control from any computer or Chromebook.

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Installation & Setup](#installation--setup)
4. [Authentication](#authentication)
5. [Web Interface](#web-interface)
6. [REST API Reference](#rest-api-reference)
7. [WebSocket Communication](#websocket-communication)
8. [Android Integration](#android-integration)
9. [PWA Installation](#pwa-installation)
10. [Development Guide](#development-guide)
11. [Security](#security)
12. [Troubleshooting](#troubleshooting)

## Features

### ✅ Implemented Features

1. **Modern Web Interface**
   - Built with React 18 and Vite
   - Responsive design for all screen sizes
   - Dark theme optimized for long usage
   - Material Design-inspired UI components

2. **REST API**
   - Complete RESTful API for device communication
   - JWT-based authentication
   - Comprehensive backup management endpoints
   - Settings synchronization
   - Device status monitoring

3. **WebSocket Real-time Communication**
   - Live backup progress updates
   - Real-time notifications
   - Bidirectional communication with Android device
   - Automatic reconnection handling

4. **Desktop File Browser**
   - Browse backup contents with intuitive interface
   - Breadcrumb navigation
   - File search functionality
   - Individual file downloads
   - Folder structure visualization

5. **Remote Backup Trigger**
   - Start backups from desktop/Chromebook
   - Monitor progress in real-time
   - Success/failure notifications
   - Backup configuration options

6. **Backup History Visualization**
   - Interactive charts with Recharts
   - Timeline visualization
   - Success rate analytics
   - Size trending over time
   - Detailed activity log

7. **Settings Management**
   - Complete settings interface
   - Backup frequency configuration
   - Cloud provider setup
   - Encryption settings
   - Notification preferences

8. **Authentication System**
   - QR code pairing
   - Token-based authentication
   - Secure JWT tokens (30-day expiry)
   - Device management

9. **Responsive Design**
   - Mobile-first approach
   - Tablet optimization
   - Desktop large-screen layouts
   - ChromeOS-optimized

10. **PWA Support**
    - Installable on ChromeOS
    - Offline capability (basic)
    - App-like experience
    - Service worker integration

## Architecture

### Frontend Stack

```
React 18.2.0          - UI Framework
React Router 6.20     - Client-side routing
Vite 5.0              - Build tool & dev server
Zustand 4.4           - State management
Axios 1.6             - HTTP client
Socket.io Client 4.6  - WebSocket client
Recharts 2.10         - Data visualization
Lucide React 0.292    - Icon library
QRCode.react 3.1      - QR code generation
date-fns 2.30         - Date formatting
```

### Backend Stack

```
Node.js + Express 4.18  - REST API server
Socket.io 4.6           - WebSocket server
jsonwebtoken 9.0        - JWT authentication
CORS 2.8                - Cross-origin support
dotenv 16.3             - Environment config
```

### Project Structure

```
web-companion/
├── src/
│   ├── components/
│   │   ├── Layout.jsx              # Main app layout with sidebar
│   │   ├── Layout.css
│   │   └── ProtectedRoute.jsx      # Route protection
│   │
│   ├── pages/
│   │   ├── LoginPage.jsx           # Authentication page
│   │   ├── LoginPage.css
│   │   ├── DashboardPage.jsx       # Main dashboard
│   │   ├── DashboardPage.css
│   │   ├── BackupsPage.jsx         # Backup management
│   │   ├── BackupsPage.css
│   │   ├── FileBrowserPage.jsx     # File browsing
│   │   ├── FileBrowserPage.css
│   │   ├── HistoryPage.jsx         # History & analytics
│   │   ├── HistoryPage.css
│   │   ├── SettingsPage.jsx        # Settings management
│   │   └── SettingsPage.css
│   │
│   ├── stores/
│   │   ├── authStore.js            # Authentication state
│   │   └── websocketStore.js       # WebSocket state
│   │
│   ├── services/
│   │   └── api.js                  # API service layer
│   │
│   ├── styles/
│   │   └── index.css               # Global styles
│   │
│   ├── App.jsx                     # Root component
│   └── main.jsx                    # Entry point
│
├── server/
│   ├── routes/
│   │   ├── auth.js                 # Auth endpoints
│   │   ├── backups.js              # Backup endpoints
│   │   ├── files.js                # File endpoints
│   │   ├── history.js              # History endpoints
│   │   ├── settings.js             # Settings endpoints
│   │   └── device.js               # Device endpoints
│   │
│   ├── index.js                    # Server entry point
│   └── package.json
│
├── public/                         # Static assets
├── index.html                      # HTML template
├── vite.config.js                  # Vite configuration
├── package.json                    # Frontend dependencies
├── .env.example                    # Environment template
└── README.md                       # Quick start guide
```

## Installation & Setup

### Prerequisites

- Node.js 18+ and npm
- ObsidianBackup Android app installed
- Modern web browser (Chrome, Firefox, Safari, Edge)

### Step 1: Install Dependencies

```bash
cd /root/workspace/ObsidianBackup/web-companion

# Install frontend dependencies
npm install

# Install backend dependencies
cd server
npm install
cd ..
```

### Step 2: Configure Environment

```bash
cp .env.example .env
```

Edit `.env`:
```env
PORT=8080
CLIENT_URL=http://localhost:3000
JWT_SECRET=change-this-to-a-secure-random-string
ANDROID_HOST=192.168.1.100  # Your Android device IP
ANDROID_PORT=8081
```

### Step 3: Start Development Servers

```bash
# Start both frontend and backend
npm run dev:all

# Or start separately:
npm run dev        # Frontend (port 3000)
npm run server     # Backend (port 8080)
```

### Step 4: Access the Application

Open your browser to: http://localhost:3000

### Production Deployment

```bash
# Build frontend
npm run build

# Start production server
cd server
npm start
```

Serve the `dist/` folder with any static file server or deploy to:
- Vercel
- Netlify
- AWS S3 + CloudFront
- Firebase Hosting
- GitHub Pages

## Authentication

### QR Code Pairing

The most user-friendly authentication method.

#### Web Interface:
1. Navigate to http://localhost:3000
2. Select "QR Code" tab
3. A QR code and pairing code are displayed
4. Wait for Android app to scan

#### Android App (to be implemented):
```kotlin
// In your Android app settings screen
class WebCompanionSettingsActivity : AppCompatActivity() {
    
    private fun pairWithQRCode() {
        // Start QR scanner
        val scanner = QRCodeScanner()
        scanner.startScanning { qrData ->
            val pairingData = Json.decodeFromString<PairingData>(qrData)
            
            // Send pairing request to server
            val token = apiService.pair(pairingData.code)
            
            // Save token for future use
            preferences.edit()
                .putString("companion_token", token)
                .apply()
            
            // Start WebSocket connection
            connectWebSocket(token)
        }
    }
    
    data class PairingData(
        val type: String,
        val code: String,
        val timestamp: Long
    )
}
```

### Token-Based Pairing

For situations where QR scanning is not available.

#### Android App Token Generation:
```kotlin
class WebCompanionManager(private val context: Context) {
    
    fun generatePairingToken(): String {
        // Generate secure random token
        val token = UUID.randomUUID().toString()
        
        // Store temporarily (5 minutes expiry)
        TokenStore.storePairingToken(token, expiryMinutes = 5)
        
        return token
    }
    
    fun validatePairingToken(token: String): Boolean {
        return TokenStore.validateAndConsume(token)
    }
}
```

#### Web Interface:
1. Navigate to http://localhost:3000
2. Select "Token" tab
3. Enter token from Android app
4. Click "Connect"

### JWT Token Storage

Tokens are stored in browser localStorage with Zustand persistence:

```javascript
// Automatic storage
{
  "state": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "deviceId": "device_1234567890",
    "deviceName": "Samsung Galaxy S21",
    "isAuthenticated": true
  },
  "version": 0
}
```

Token includes:
- User ID
- Device ID
- Device name
- 30-day expiration

## Web Interface

### Dashboard

**Purpose**: Overview of backup system status and quick actions.

**Features**:
- Current backup statistics (total, size, last backup, success rate)
- Active backup progress monitoring
- Device information display
- Recent backups list
- Quick backup trigger button

**Components**:
- Stat cards with icons
- Real-time progress bar during backups
- Device info grid
- Recent activity list

### Backups Page

**Purpose**: Manage all backups with filtering and actions.

**Features**:
- Grid view of all backups
- Search functionality
- Status filtering (all, success, failed, pending)
- Individual backup details
- Restore capability
- Delete functionality

**Actions**:
- Restore backup → Sends restore command via WebSocket
- Delete backup → Removes from system
- View details → Shows full backup information

### File Browser

**Purpose**: Navigate and download files from backups.

**Features**:
- Backup selection dropdown
- Breadcrumb navigation
- Folder/file icons
- File search
- Individual file download
- Directory traversal

**Navigation**:
```
/ (root)
├── Documents/
│   ├── file1.txt
│   └── file2.pdf
├── Pictures/
│   ├── photo1.jpg
│   └── photo2.png
└── config.json
```

### History Page

**Purpose**: Visualize backup history and trends.

**Features**:
- Period selector (7d, 30d, 90d, 1y)
- Statistics cards
- Backup frequency chart (area chart)
- Backup size over time (line chart)
- Detailed activity log
- Success/failure tracking

**Charts**:
- Recharts responsive charts
- Custom styling for dark theme
- Interactive tooltips
- Date-based x-axis

### Settings Page

**Purpose**: Configure all backup and app settings.

**Tabs**:

1. **General Settings**
   - Backup frequency (manual, hourly, daily, weekly)
   - Auto-delete old backups
   - Retention days
   - Compression level (0-9)
   - WiFi-only mode

2. **Cloud Storage**
   - Provider selection (Local, WebDAV, Rclone)
   - WebDAV configuration (URL, username, password)
   - Connection testing

3. **Security**
   - Enable encryption
   - Encryption method (AES-128, AES-256)
   - Encryption password
   - Biometric requirement

4. **Notifications**
   - Backup completion notifications
   - Failure notifications
   - Daily summary

## REST API Reference

### Base URL

```
http://localhost:8080/api
```

### Authentication Header

All authenticated endpoints require:
```
Authorization: Bearer <jwt_token>
```

### Endpoints

#### Authentication

##### POST /auth/pair
Pair device with pairing code or token.

**Request**:
```json
{
  "pairingCode": "ABC123XY"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "deviceId": "device_1234567890",
  "deviceName": "Samsung Galaxy S21"
}
```

##### GET /auth/verify
Verify authentication token validity.

**Response**:
```json
{
  "valid": true,
  "user": {
    "userId": "user_1",
    "deviceId": "device_1234567890",
    "deviceName": "Samsung Galaxy S21"
  }
}
```

##### POST /auth/revoke
Revoke current authentication token.

**Response**:
```json
{
  "success": true
}
```

#### Backups

##### GET /backups
List all backups with optional filtering.

**Query Parameters**:
- `status` (optional): Filter by status (success, failed, pending)

**Response**:
```json
[
  {
    "id": "1",
    "name": "Full Backup - 2024-02-08",
    "timestamp": "2024-02-08T10:30:00.000Z",
    "status": "success",
    "size": 157286400,
    "fileCount": 234,
    "type": "full"
  }
]
```

##### GET /backups/:id
Get details of specific backup.

**Response**:
```json
{
  "id": "1",
  "name": "Full Backup - 2024-02-08",
  "timestamp": "2024-02-08T10:30:00.000Z",
  "status": "success",
  "size": 157286400,
  "fileCount": 234,
  "type": "full",
  "details": {
    "duration": 45,
    "compression": "gzip",
    "encrypted": false
  }
}
```

##### POST /backups/trigger
Trigger new backup on Android device.

**Request**:
```json
{
  "type": "full",
  "compression": true,
  "encryption": false
}
```

**Response**:
```json
{
  "success": true,
  "message": "Backup triggered successfully"
}
```

**Error**: 503 if device not connected

##### DELETE /backups/:id
Delete backup.

**Response**:
```json
{
  "success": true
}
```

##### POST /backups/:id/restore
Restore backup to device.

**Request**:
```json
{
  "location": "/sdcard/restore",
  "overwrite": false
}
```

**Response**:
```json
{
  "success": true,
  "message": "Restore initiated"
}
```

##### GET /backups/stats
Get backup statistics.

**Response**:
```json
{
  "totalBackups": 15,
  "totalSize": 2147483648,
  "lastBackup": "2024-02-08T10:30:00.000Z",
  "successRate": 95,
  "recentBackups": [...]
}
```

##### GET /backups/:id/files
List files in backup.

**Query Parameters**:
- `path` (optional): Path to list (default: "/")

**Response**:
```json
[
  {
    "name": "Documents",
    "type": "directory",
    "path": "/Documents"
  },
  {
    "name": "config.txt",
    "type": "file",
    "size": 1024,
    "path": "/config.txt"
  }
]
```

##### GET /backups/:id/files/download
Download file from backup.

**Query Parameters**:
- `path` (required): File path to download

**Response**: Binary file data

##### GET /backups/:id/files/search
Search files in backup.

**Query Parameters**:
- `q` (required): Search query

**Response**:
```json
[
  {
    "name": "document.pdf",
    "type": "file",
    "size": 2048,
    "path": "/Documents/document.pdf",
    "matchScore": 0.95
  }
]
```

#### History

##### GET /history
Get backup history.

**Query Parameters**:
- `limit` (optional): Max entries to return (default: 50)

**Response**:
```json
[
  {
    "id": "1",
    "action": "Full Backup",
    "status": "success",
    "timestamp": "2024-02-08T10:30:00.000Z",
    "description": "Completed full backup of all data",
    "details": {
      "files": 234,
      "size": 157286400,
      "duration": 45
    }
  }
]
```

##### GET /history/:id
Get specific history entry.

##### GET /history/stats
Get history statistics.

**Query Parameters**:
- `period` (optional): Time period (7d, 30d, 90d, 1y)

**Response**:
```json
{
  "totalBackups": 50,
  "successRate": 95,
  "avgDuration": 28,
  "timeline": [
    { "date": "2024-02-01", "count": 3 },
    { "date": "2024-02-02", "count": 2 }
  ],
  "sizeTimeline": [
    { "date": "2024-02-01", "size": 150 },
    { "date": "2024-02-02", "size": 145 }
  ]
}
```

#### Settings

##### GET /settings
Get all settings.

**Response**:
```json
{
  "backupFrequency": "daily",
  "autoDeleteOldBackups": false,
  "retentionDays": 30,
  "compressionLevel": 6,
  "wifiOnly": true,
  "cloudProvider": "local",
  "enableEncryption": false,
  "notifyOnComplete": true,
  "notifyOnFailure": true,
  "dailySummary": false
}
```

##### PUT /settings
Update settings.

**Request**: Partial or full settings object

**Response**: Updated settings object

##### GET /settings/cloud-providers
List available cloud providers.

**Response**:
```json
[
  {
    "id": "local",
    "name": "Local Storage",
    "enabled": true
  },
  {
    "id": "webdav",
    "name": "WebDAV",
    "enabled": true
  }
]
```

##### POST /settings/cloud-providers/:id/test
Test cloud provider connection.

**Response**:
```json
{
  "success": true,
  "message": "Connection successful"
}
```

#### Device

##### GET /device/info
Get device information.

**Response**:
```json
{
  "name": "Samsung Galaxy S21",
  "model": "SM-G991B",
  "androidVersion": "13",
  "storageTotal": 137438953472,
  "storageUsed": 68719476736
}
```

##### GET /device/status
Get device status.

**Response**:
```json
{
  "online": true,
  "battery": 85,
  "charging": false,
  "wifiConnected": true,
  "lastSeen": "2024-02-08T12:00:00.000Z"
}
```

##### GET /device/storage
Get storage information.

**Response**:
```json
{
  "total": 137438953472,
  "used": 68719476736,
  "free": 68719476736,
  "backups": 10737418240
}
```

## WebSocket Communication

### Connection

```javascript
import { io } from 'socket.io-client';

const socket = io('ws://localhost:8080', {
  auth: {
    token: 'your-jwt-token'
  },
  transports: ['websocket']
});
```

### Events

#### Client → Server

##### backup:start
Notify server that backup is starting.

```javascript
socket.emit('backup:start', {
  type: 'full',
  timestamp: new Date().toISOString()
});
```

##### backup:progress
Report backup progress.

```javascript
socket.emit('backup:progress', {
  percentage: 45,
  currentFile: '/data/app/com.example/file.apk',
  processedFiles: 100,
  totalFiles: 234,
  processedBytes: 67108864,
  totalBytes: 157286400
});
```

#### Server → Client

##### backup:trigger
Server requests device to start backup.

```javascript
socket.on('backup:trigger', (options) => {
  // Start backup on Android device
  startBackup(options);
});
```

##### backup:restore
Server requests device to restore backup.

```javascript
socket.on('backup:restore', (data) => {
  // Restore backup on Android device
  restoreBackup(data.backupId, data);
});
```

##### backup:progress
Receive backup progress updates.

```javascript
socket.on('backup:progress', (progress) => {
  updateProgressBar(progress.percentage);
  updateCurrentFile(progress.currentFile);
});
```

##### backup:complete
Backup completed successfully.

```javascript
socket.on('backup:complete', (data) => {
  showNotification('Backup completed successfully');
  refreshBackupList();
});
```

##### backup:error
Backup failed with error.

```javascript
socket.on('backup:error', (error) => {
  showNotification(`Backup failed: ${error.message}`, 'error');
});
```

##### notification
General notification.

```javascript
socket.on('notification', (notification) => {
  showNotification(notification.message, notification.type);
});
```

### Android Implementation

```kotlin
// In your Android app
class WebSocketManager(private val token: String) {
    private var socket: Socket? = null
    
    fun connect() {
        val options = IO.Options().apply {
            auth = mapOf("token" to token)
            transports = arrayOf("websocket")
        }
        
        socket = IO.socket("http://192.168.1.100:8080", options)
        
        // Listen for backup trigger
        socket?.on("backup:trigger") { args ->
            val options = args[0] as JSONObject
            triggerBackup(options)
        }
        
        // Listen for restore trigger
        socket?.on("backup:restore") { args ->
            val data = args[0] as JSONObject
            restoreBackup(data)
        }
        
        socket?.connect()
    }
    
    fun reportProgress(progress: BackupProgress) {
        socket?.emit("backup:progress", JSONObject().apply {
            put("percentage", progress.percentage)
            put("currentFile", progress.currentFile)
            put("processedFiles", progress.processedFiles)
            put("totalFiles", progress.totalFiles)
            put("processedBytes", progress.processedBytes)
            put("totalBytes", progress.totalBytes)
        })
    }
    
    fun reportComplete(result: BackupResult) {
        socket?.emit("backup:complete", JSONObject().apply {
            put("backupId", result.id)
            put("size", result.size)
            put("duration", result.duration)
            put("fileCount", result.fileCount)
        })
    }
    
    fun reportError(error: String) {
        socket?.emit("backup:error", JSONObject().apply {
            put("message", error)
            put("timestamp", System.currentTimeMillis())
        })
    }
    
    fun disconnect() {
        socket?.disconnect()
    }
}
```

## Android Integration

### Add Web Companion Module

Create new module in Android app:

```kotlin
// WebCompanionModule.kt
package com.titanbackup.webcompanion

import android.content.Context
import android.content.SharedPreferences
import io.socket.client.Socket
import kotlinx.coroutines.*

class WebCompanionModule(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "web_companion",
        Context.MODE_PRIVATE
    )
    private var webSocket: WebSocketManager? = null
    
    fun isPaired(): Boolean {
        return prefs.contains("companion_token")
    }
    
    fun getToken(): String? {
        return prefs.getString("companion_token", null)
    }
    
    fun saveToken(token: String) {
        prefs.edit()
            .putString("companion_token", token)
            .apply()
            
        // Start WebSocket connection
        connectWebSocket(token)
    }
    
    fun unpair() {
        webSocket?.disconnect()
        webSocket = null
        
        prefs.edit()
            .remove("companion_token")
            .apply()
    }
    
    private fun connectWebSocket(token: String) {
        webSocket = WebSocketManager(token).apply {
            connect()
        }
    }
    
    fun reportBackupProgress(progress: BackupProgress) {
        webSocket?.reportProgress(progress)
    }
    
    fun reportBackupComplete(result: BackupResult) {
        webSocket?.reportComplete(result)
    }
    
    fun reportBackupError(error: String) {
        webSocket?.reportError(error)
    }
}

// In your backup process
class BackupEngine {
    private val webCompanion = WebCompanionModule(context)
    
    suspend fun performBackup() {
        try {
            // Notify start
            webCompanion.reportBackupProgress(
                BackupProgress(0, "Starting backup...", 0, totalFiles, 0, totalBytes)
            )
            
            // Process files
            files.forEachIndexed { index, file ->
                processFile(file)
                
                // Report progress
                val progress = ((index + 1) * 100) / totalFiles
                webCompanion.reportBackupProgress(
                    BackupProgress(
                        progress,
                        file.path,
                        index + 1,
                        totalFiles,
                        processedBytes,
                        totalBytes
                    )
                )
            }
            
            // Report completion
            webCompanion.reportBackupComplete(
                BackupResult(
                    id = backupId,
                    size = totalBytes,
                    duration = elapsedSeconds,
                    fileCount = files.size
                )
            )
        } catch (e: Exception) {
            webCompanion.reportBackupError(e.message ?: "Unknown error")
        }
    }
}
```

### Add Settings UI

```kotlin
// WebCompanionSettingsActivity.kt
class WebCompanionSettingsActivity : AppCompatActivity() {
    private lateinit var webCompanion: WebCompanionModule
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_companion_settings)
        
        webCompanion = WebCompanionModule(this)
        
        updateUI()
        
        findViewById<Button>(R.id.btn_pair_qr).setOnClickListener {
            startQRScanning()
        }
        
        findViewById<Button>(R.id.btn_pair_token).setOnClickListener {
            showTokenDialog()
        }
        
        findViewById<Button>(R.id.btn_unpair).setOnClickListener {
            unpairDevice()
        }
    }
    
    private fun startQRScanning() {
        val scanner = QRCodeScanner()
        scanner.startScanning { qrData ->
            val pairingData = Json.decodeFromString<PairingData>(qrData)
            pairDevice(pairingData.code)
        }
    }
    
    private fun showTokenDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Enter Pairing Token")
            .setView(input)
            .setPositiveButton("Pair") { _, _ ->
                pairDevice(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun pairDevice(code: String) {
        lifecycleScope.launch {
            try {
                val result = apiService.pair(code)
                webCompanion.saveToken(result.token)
                updateUI()
                Toast.makeText(
                    this@WebCompanionSettingsActivity,
                    "Successfully paired!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@WebCompanionSettingsActivity,
                    "Pairing failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun unpairDevice() {
        AlertDialog.Builder(this)
            .setTitle("Unpair Device?")
            .setMessage("This will disconnect the web companion.")
            .setPositiveButton("Unpair") { _, _ ->
                webCompanion.unpair()
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateUI() {
        val isPaired = webCompanion.isPaired()
        findViewById<View>(R.id.layout_paired).visibility =
            if (isPaired) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layout_unpaired).visibility =
            if (isPaired) View.GONE else View.VISIBLE
    }
}
```

### Add Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" /> <!-- For QR scanning -->

<application>
    <!-- ... -->
    
    <activity
        android:name=".webcompanion.WebCompanionSettingsActivity"
        android:label="Web Companion"
        android:exported="false" />
</application>
```

### Add Dependencies

```gradle
// app/build.gradle.kts
dependencies {
    // WebSocket
    implementation("io.socket:socket.io-client:2.1.0")
    
    // QR Code scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // HTTP client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
```

## PWA Installation

### ChromeOS Installation

1. **Open Chrome Browser**
   - Navigate to the web companion URL
   - Example: http://192.168.1.100:3000

2. **Install Application**
   - Click the install icon (⊕) in the address bar
   - Or use Chrome menu → "Install ObsidianBackup Companion"

3. **Launch Application**
   - Find "ObsidianBackup Companion" in your app drawer
   - Pin to shelf for quick access
   - Works like a native app

4. **Offline Capability**
   - Basic offline functionality available
   - Service worker caches static assets
   - Requires initial online connection

### Desktop Installation

#### Chrome/Edge (Windows/Mac/Linux)

1. Visit the web companion in Chrome or Edge
2. Look for install prompt or address bar icon
3. Click "Install" to add to your applications
4. Launch from:
   - Start Menu (Windows)
   - Applications folder (Mac)
   - App menu (Linux)

#### Firefox (Limited PWA Support)

Firefox has limited PWA support. Consider:
- Bookmark for quick access
- Use "Pin Tab" feature
- Enable notifications for updates

### Service Worker Configuration

The PWA uses Workbox for caching:

```javascript
// vite.config.js
VitePWA({
  registerType: 'autoUpdate',
  workbox: {
    globPatterns: ['**/*.{js,css,html,ico,png,svg}'],
    runtimeCaching: [
      {
        urlPattern: /^https:\/\/api\..*/i,
        handler: 'NetworkFirst',
        options: {
          cacheName: 'api-cache',
          expiration: {
            maxEntries: 10,
            maxAgeSeconds: 300
          }
        }
      }
    ]
  }
})
```

## Development Guide

### Running Development Environment

```bash
# Terminal 1: Start backend server
cd server
npm run dev

# Terminal 2: Start frontend
npm run dev

# Or use concurrently:
npm run dev:all
```

### Adding New Feature

#### Example: Add Backup Scheduling

1. **Create API Endpoint**

```javascript
// server/routes/schedule.js
import express from 'express';
const router = express.Router();

router.get('/', (req, res) => {
  res.json({ schedules: [] });
});

router.post('/', (req, res) => {
  const schedule = req.body;
  // Save schedule
  res.json({ success: true, schedule });
});

export default router;
```

2. **Update Server**

```javascript
// server/index.js
import scheduleRoutes from './routes/schedule.js';

app.use('/api/schedule', authenticateToken, scheduleRoutes);
```

3. **Create API Service**

```javascript
// src/services/api.js
export const scheduleApi = {
  getSchedules: async () => {
    const response = await api.get('/schedule');
    return response.data;
  },
  
  createSchedule: async (schedule) => {
    const response = await api.post('/schedule', schedule);
    return response.data;
  }
};
```

4. **Create UI Component**

```jsx
// src/pages/SchedulePage.jsx
import React, { useState, useEffect } from 'react';
import { scheduleApi } from '../services/api';

export default function SchedulePage() {
  const [schedules, setSchedules] = useState([]);
  
  useEffect(() => {
    loadSchedules();
  }, []);
  
  const loadSchedules = async () => {
    const data = await scheduleApi.getSchedules();
    setSchedules(data.schedules);
  };
  
  return (
    <div className="schedule-page">
      <h1>Backup Schedules</h1>
      {/* Schedule UI */}
    </div>
  );
}
```

5. **Add Route**

```jsx
// src/App.jsx
import SchedulePage from './pages/SchedulePage';

<Route path="schedule" element={<SchedulePage />} />
```

### Code Style

- Use functional components with hooks
- Follow ESLint configuration
- Use CSS modules or styled-components for component styles
- Keep components small and focused
- Use TypeScript for type safety (optional)

### Testing

```bash
# Install testing dependencies
npm install --save-dev @testing-library/react @testing-library/jest-dom vitest

# Run tests
npm test
```

Example test:
```javascript
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import DashboardPage from './DashboardPage';

describe('DashboardPage', () => {
  it('renders dashboard title', () => {
    render(<DashboardPage />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });
});
```

## Security

### Production Checklist

- [ ] Change JWT_SECRET to secure random string
- [ ] Enable HTTPS in production
- [ ] Configure proper CORS origins
- [ ] Implement rate limiting
- [ ] Add input validation
- [ ] Sanitize user inputs
- [ ] Enable CSP headers
- [ ] Use secure WebSocket (wss://)
- [ ] Implement token refresh mechanism
- [ ] Add audit logging
- [ ] Enable 2FA (optional)
- [ ] Regular security audits

### JWT Token Security

```javascript
// Secure JWT configuration
const JWT_CONFIG = {
  algorithm: 'HS256',
  expiresIn: '30d',
  issuer: 'obsidianbackup',
  audience: 'obsidianbackup-companion'
};

// Generate token
const token = jwt.sign(payload, JWT_SECRET, JWT_CONFIG);

// Verify token
jwt.verify(token, JWT_SECRET, {
  algorithms: ['HS256'],
  issuer: 'obsidianbackup',
  audience: 'obsidianbackup-companion'
});
```

### HTTPS Configuration

For production with HTTPS:

```javascript
// server/index.js
import https from 'https';
import fs from 'fs';

const httpsOptions = {
  key: fs.readFileSync('path/to/private-key.pem'),
  cert: fs.readFileSync('path/to/certificate.pem')
};

const httpsServer = https.createServer(httpsOptions, app);
const io = new Server(httpsServer, {
  cors: {
    origin: 'https://yourdomain.com',
    methods: ['GET', 'POST']
  }
});

httpsServer.listen(443);
```

### Rate Limiting

```javascript
// server/index.js
import rateLimit from 'express-rate-limit';

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

app.use('/api/', limiter);
```

## Troubleshooting

### Common Issues

#### 1. WebSocket Connection Failed

**Symptoms**: "WebSocket disconnected" shown in UI

**Solutions**:
- Check if backend server is running
- Verify CORS configuration
- Check firewall settings
- Ensure correct WebSocket URL
- Check browser console for errors

```bash
# Test WebSocket connection
curl -i -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Host: localhost:8080" \
  http://localhost:8080/socket.io/
```

#### 2. Authentication Failed

**Symptoms**: 401 or 403 errors

**Solutions**:
- Check JWT token validity
- Verify JWT_SECRET matches between sessions
- Check token expiration
- Clear localStorage and re-authenticate

```javascript
// Debug token in browser console
const auth = JSON.parse(localStorage.getItem('obsidianbackup-auth'));
console.log('Token:', auth.state.token);
console.log('Expires:', jwt_decode(auth.state.token).exp);
```

#### 3. Android Device Not Connecting

**Symptoms**: 503 errors when triggering backups

**Solutions**:
- Ensure Android app is running
- Check network connectivity
- Verify same network (WiFi)
- Check Android app WebSocket implementation
- Verify token is valid

#### 4. PWA Not Installing

**Symptoms**: No install prompt appears

**Solutions**:
- Must be served over HTTPS (except localhost)
- Check manifest.webmanifest is accessible
- Verify service worker registration
- Check browser PWA support
- Look for errors in DevTools Application tab

#### 5. Charts Not Displaying

**Symptoms**: Empty chart areas

**Solutions**:
- Check Recharts is installed
- Verify data format matches chart expectations
- Check responsive container dimensions
- Look for console errors
- Ensure parent has height

### Debug Mode

Enable debug logging:

```javascript
// Add to src/main.jsx
if (import.meta.env.DEV) {
  window.DEBUG = true;
}

// Use in code
if (window.DEBUG) {
  console.log('Debug info:', data);
}
```

### Browser DevTools

**Network Tab**:
- Monitor API requests
- Check WebSocket connection
- Verify response data

**Console Tab**:
- Check for JavaScript errors
- View debug logs
- Test API calls

**Application Tab**:
- Check localStorage
- Verify service worker
- View manifest
- Check cache storage

## Performance Optimization

### Frontend

1. **Code Splitting**
```javascript
// Lazy load pages
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const BackupsPage = lazy(() => import('./pages/BackupsPage'));

<Suspense fallback={<Loading />}>
  <Routes>
    <Route path="dashboard" element={<DashboardPage />} />
  </Routes>
</Suspense>
```

2. **Memoization**
```javascript
import { useMemo, useCallback } from 'react';

const filteredBackups = useMemo(() => {
  return backups.filter(b => b.status === filter);
}, [backups, filter]);

const handleClick = useCallback(() => {
  // Handler logic
}, [dependencies]);
```

3. **Virtual Scrolling**
For large file lists, use react-window:
```javascript
import { FixedSizeList } from 'react-window';

<FixedSizeList
  height={600}
  itemCount={files.length}
  itemSize={50}
>
  {FileRow}
</FixedSizeList>
```

### Backend

1. **Connection Pooling**
2. **Caching** (Redis recommended)
3. **Compression** (gzip)
4. **Database Indexing**
5. **Pagination**

## Future Enhancements

### Planned Features

1. **Multi-device Support**
   - Manage multiple Android devices
   - Switch between devices
   - Consolidated dashboard

2. **Backup Comparison**
   - Compare two backups
   - Show differences
   - Selective restore

3. **Cloud Storage Integration**
   - Direct cloud uploads from web
   - Cloud-to-cloud transfers
   - Cloud storage management

4. **Advanced Analytics**
   - Backup trends
   - Storage predictions
   - Performance metrics

5. **Collaboration Features**
   - Share backups
   - Team management
   - Access control

6. **Mobile Web App**
   - Responsive mobile layout
   - Touch-optimized UI
   - Mobile PWA

7. **Backup Verification**
   - Integrity checking
   - Automated testing
   - Corruption detection

8. **Scheduling**
   - Cron-based scheduling
   - Conditional backups
   - Backup chains

## Support & Contributing

### Getting Help

- Check this documentation first
- Review GitHub issues
- Check browser console for errors
- Enable debug mode for detailed logging

### Reporting Issues

Include:
1. Browser and version
2. Operating system
3. Steps to reproduce
4. Expected vs actual behavior
5. Console errors
6. Network tab screenshots

### Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## License

See main project LICENSE file.

---

**Documentation Version**: 1.0.0  
**Last Updated**: February 8, 2024  
**Maintainer**: ObsidianBackup Team
