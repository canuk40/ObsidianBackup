# ObsidianBackup Web Companion - Quick Start

## 🚀 Get Started in 3 Minutes

### 1. Setup (One-time)

```bash
cd /root/workspace/ObsidianBackup/web-companion

# Run automated setup
./setup.sh

# Or manually:
npm install
cd server && npm install && cd ..
cp .env.example .env
```

### 2. Configure

Edit `.env`:
```env
PORT=8080
CLIENT_URL=http://localhost:3000
JWT_SECRET=your-secure-secret-change-this
```

### 3. Run

```bash
# Start both frontend and backend
npm run dev:all

# Access at http://localhost:3000
```

## 📱 What You Get

### Web Interface
- **Dashboard** - Real-time backup stats and device info
- **Backups** - Manage, restore, and delete backups
- **Files** - Browse and download files from backups
- **History** - Visualize backup trends with charts
- **Settings** - Configure all backup options

### Features
- 🔐 QR code pairing with Android app
- 🔄 Real-time backup progress via WebSocket
- 📁 Full file browser with search
- 📊 Interactive charts and analytics
- ⚙️ Remote settings management
- 📱 PWA - Install on ChromeOS/Desktop
- 🎨 Dark theme, responsive design

## 🔌 API Endpoints

All endpoints available at `http://localhost:8080/api/`

### Quick Reference
```
POST   /auth/pair              - Pair device
GET    /backups                - List backups
POST   /backups/trigger        - Start backup
GET    /backups/:id/files      - Browse files
GET    /history                - Get history
GET    /settings               - Get settings
PUT    /settings               - Update settings
GET    /device/info            - Device info
```

## 🌐 WebSocket Events

Connect to `ws://localhost:8080` with JWT token.

### Events
- `backup:progress` - Real-time progress updates
- `backup:complete` - Backup finished
- `backup:trigger` - Start backup (server → device)
- `backup:restore` - Restore backup (server → device)

## 📱 Android Integration

### Required Dependencies
```gradle
implementation("io.socket:socket.io-client:2.1.0")
implementation("com.google.mlkit:barcode-scanning:17.2.0")
```

### Basic Integration
```kotlin
// 1. Create WebSocket connection
val socket = IO.socket("http://192.168.1.100:8080", options)

// 2. Listen for backup trigger
socket.on("backup:trigger") { args ->
    startBackup()
}

// 3. Report progress
socket.emit("backup:progress", JSONObject().apply {
    put("percentage", 50)
    put("currentFile", "/path/to/file")
})

// 4. Report completion
socket.emit("backup:complete", JSONObject().apply {
    put("backupId", "backup_123")
    put("size", 157286400)
})
```

## 🏗️ Project Structure

```
web-companion/
├── src/
│   ├── components/     # Layout, ProtectedRoute
│   ├── pages/          # 5 main pages + login
│   ├── stores/         # Auth & WebSocket state
│   ├── services/       # API client
│   └── styles/         # Global CSS
├── server/
│   ├── routes/         # 6 API route modules
│   └── index.js        # Express + Socket.io server
├── public/             # Static assets & manifest
└── package.json        # Dependencies
```

## 🎨 Pages

1. **Login** - QR code or token pairing
2. **Dashboard** - Stats, device info, quick actions
3. **Backups** - Grid view with search/filter
4. **Files** - Navigate folders, download files
5. **History** - Charts and activity log
6. **Settings** - 4 tabs: General, Cloud, Security, Notifications

## 🔒 Security

- JWT tokens (30-day expiry)
- Secure WebSocket with auth
- CORS configured
- Token stored in localStorage
- Protected routes

## 💾 PWA Installation

### ChromeOS
1. Open Chrome
2. Visit http://localhost:3000
3. Click install icon in address bar
4. Launch from app drawer

### Desktop
1. Open in Chrome/Edge
2. Click install prompt
3. Launch like native app

## 🐛 Troubleshooting

### WebSocket Not Connecting
- Check backend is running on port 8080
- Verify firewall settings
- Check browser console for errors

### Authentication Failed
- Clear localStorage: `localStorage.clear()`
- Re-authenticate with QR or token
- Check JWT_SECRET matches

### PWA Not Installing
- Must use HTTPS (except localhost)
- Check manifest is accessible
- Verify service worker in DevTools

## 📚 Documentation

- **README.md** - This file
- **CHROMEBOOK_COMPANION.md** - Complete guide (37KB)
- **IMPLEMENTATION_SUMMARY.md** - Feature checklist

## 🚢 Production Deployment

```bash
# Build for production
npm run build

# Deploy dist/ folder to:
# - Vercel: vercel deploy
# - Netlify: netlify deploy
# - AWS S3: aws s3 sync dist/ s3://bucket
# - Firebase: firebase deploy
```

### Production Backend
```bash
cd server
PORT=8080 JWT_SECRET=secure-secret node index.js
```

## 📊 Tech Stack

**Frontend**: React 18, Vite, Zustand, Recharts, Socket.io Client  
**Backend**: Node.js, Express, Socket.io, JWT  
**Features**: PWA, WebSocket, REST API, QR codes

## 🎯 Next Steps

1. ✅ Web companion is complete
2. 🔄 Integrate with Android app:
   - Add WebSocket client
   - Implement pairing UI
   - Report backup progress
3. 🚀 Deploy to production
4. 🧪 Test end-to-end flow
5. 📱 Install as PWA on ChromeOS

## 💡 Tips

- Use Chrome DevTools Network tab to debug API calls
- Check WebSocket in DevTools Application tab
- Enable debug mode: `window.DEBUG = true`
- Monitor console for errors and logs

## 🤝 Support

See CHROMEBOOK_COMPANION.md for:
- Detailed API reference
- WebSocket protocol
- Android integration guide
- Security best practices
- Performance optimization
- Full troubleshooting guide

---

**Ready to use!** Open http://localhost:3000 and start managing your backups from desktop.
