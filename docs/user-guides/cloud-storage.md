# Cloud Storage Setup

Configure cloud storage providers for automatic backup synchronization.

## Supported Providers

ObsidianBackup supports three main cloud storage methods:

1. **Google Drive** - Direct API integration
2. **WebDAV** - Standard protocol, self-hosted options
3. **Rclone** - 40+ cloud providers via rclone

## Google Drive Setup

### Prerequisites
- Google account
- Google Drive app installed (optional)
- Internet connection

### Configuration Steps

1. Navigate to **Settings** → **Cloud Storage** → **Google Drive**
2. Tap **Connect to Google Drive**
3. Select your Google account
4. Grant permissions:
   - View and manage Drive files
   - Create new files
5. Choose backup folder (default: `ObsidianBackup`)
6. Configure sync settings
7. Tap **Save**

### Sync Settings

**Upload Options:**
- Automatic upload after backup
- Manual upload only
- Background sync
- Upload only on Wi-Fi

**Download Options:**
- Auto-download new backups
- Download on demand only
- Keep local copies

**Folder Structure:**
```
Google Drive/
└── ObsidianBackup/
    ├── backups/
    │   ├── 2024-01-15/
    │   └── 2024-01-16/
    ├── manifests/
    └── metadata/
```

### Advanced Settings

**Chunk Upload:**
- Enable for large files (>100 MB)
- Chunk size: 10-50 MB
- Parallel chunks: 1-5

**Retry Logic:**
- Retry count: 3
- Retry delay: exponential backoff
- Max retry delay: 60 seconds

### Troubleshooting Google Drive

**Authentication Failed:**
- Revoke access in Google Account settings
- Reconnect with fresh authentication

**Upload Fails:**
- Check Google Drive quota
- Verify network connectivity
- Check file size limits (750 GB/day)

**Sync Conflicts:**
- Use "Latest version" strategy
- Enable conflict copies
- Manual resolution option

## WebDAV Setup

### Prerequisites
- WebDAV server URL
- Username and password
- SSL certificate (if using HTTPS)

### Configuration Steps

1. Navigate to **Settings** → **Cloud Storage** → **WebDAV**
2. Tap **Add WebDAV Server**
3. Enter server details:
   - **Server URL**: `https://cloud.example.com/webdav`
   - **Username**: Your username
   - **Password**: Your password
   - **Base Path**: `/ObsidianBackup` (optional)
4. Test connection
5. Configure sync settings
6. Tap **Save**

### Supported WebDAV Servers

- **Nextcloud** (recommended)
- **ownCloud**
- **Box**
- **Apache with mod_dav**
- **nginx with dav module**
- **Synology NAS**
- **QNAP NAS**

### Server-Specific Configuration

#### Nextcloud

```
Server URL: https://cloud.example.com/remote.php/dav/files/username/
Username: username
Password: app_password (recommended)
Base Path: ObsidianBackup
```

**App Password:**
1. Nextcloud Settings → Security
2. Create new app password for ObsidianBackup
3. Use app password instead of main password

#### ownCloud

```
Server URL: https://cloud.example.com/remote.php/webdav/
Username: username
Password: password
Base Path: ObsidianBackup
```

#### Synology NAS

```
Server URL: https://nas.example.com:5006/
Username: nas_username
Password: nas_password
Base Path: /backup/ObsidianBackup
```

Enable WebDAV:
1. Control Panel → File Services
2. Enable WebDAV (HTTPS on port 5006)
3. Create shared folder for backups

### SSL/TLS Configuration

**Certificate Validation:**
- Validate certificate (recommended)
- Accept all certificates (not recommended)
- Custom certificate trust

**Self-Signed Certificates:**
1. Export server certificate
2. Import to Android trust store
3. Or disable validation (security risk)

### Advanced Settings

**Authentication:**
- Basic authentication
- Digest authentication
- Bearer token

**Connection:**
- Connection timeout: 30 seconds
- Read timeout: 60 seconds
- Follow redirects: Yes

**Headers:**
- Custom headers (advanced)
- User-Agent customization

### Troubleshooting WebDAV

**Connection Refused:**
- Verify server URL
- Check firewall settings
- Confirm WebDAV is enabled

**Authentication Failed:**
- Verify credentials
- Check authentication method
- Try app password

**SSL/TLS Errors:**
- Verify certificate validity
- Check certificate chain
- Import certificate if self-signed

**Slow Performance:**
- Increase connection timeout
- Reduce chunk size
- Check server performance

## Rclone Setup

Rclone provides access to 40+ cloud storage providers.

### Prerequisites
- Rclone installed on device (optional)
- Cloud provider account
- Provider credentials

### Installation

#### Method 1: Bundled Rclone
ObsidianBackup includes a bundled rclone binary:
- Automatic installation
- No root required
- Limited configurations

#### Method 2: External Rclone
Install rclone separately:
1. Install from F-Droid or GitHub
2. Configure provider
3. Link to ObsidianBackup

### Configuration Steps

1. Navigate to **Settings** → **Cloud Storage** → **Rclone**
2. Tap **Add Remote**
3. Choose provider:
   - Amazon S3
   - Dropbox
   - Microsoft OneDrive
   - Backblaze B2
   - And 40+ more
4. Follow provider-specific setup
5. Test connection
6. Configure sync settings

### Supported Providers

#### Popular Providers
- Amazon S3
- Dropbox
- Microsoft OneDrive
- Google Cloud Storage
- Backblaze B2
- Wasabi
- DigitalOcean Spaces

#### Storage Providers
- AWS S3
- Azure Blob Storage
- Google Cloud Storage
- Alibaba Cloud OSS
- OpenStack Swift

#### File Services
- Box
- pCloud
- Mega.nz
- Yandex Disk
- Mail.ru Cloud

### Provider-Specific Setup

#### Amazon S3

```
Provider: s3
Access Key ID: YOUR_ACCESS_KEY
Secret Access Key: YOUR_SECRET_KEY
Region: us-east-1
Endpoint: (leave blank for AWS)
```

**IAM Permissions Required:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::your-bucket/*",
        "arn:aws:s3:::your-bucket"
      ]
    }
  ]
}
```

#### Dropbox

```
Provider: dropbox
Token: (OAuth2 flow)
```

1. Generate app token in Dropbox App Console
2. Or use OAuth2 flow (recommended)
3. Grant file access permissions

#### Microsoft OneDrive

```
Provider: onedrive
Token: (OAuth2 flow)
Drive Type: personal | business
```

1. Authenticate with Microsoft account
2. Choose Personal or Business
3. Grant file access

#### Backblaze B2

```
Provider: b2
Account ID: YOUR_ACCOUNT_ID
Application Key: YOUR_APP_KEY
```

1. Create B2 bucket
2. Generate application key
3. Limit key to bucket if needed

### Rclone Configuration File

Advanced users can create rclone config:

```ini
[obsidian-remote]
type = s3
provider = AWS
env_auth = false
access_key_id = YOUR_KEY
secret_access_key = YOUR_SECRET
region = us-east-1
```

Location: `/data/data/com.obsidianbackup/files/rclone.conf`

### Advanced Settings

**Transfers:**
- Parallel transfers: 4
- Chunk size: 8 MB
- Upload cutoff: 200 MB

**Performance:**
- Buffer size: 16 MB
- Multi-threaded uploads: Yes
- Skip unchanged files: Yes

**Retry:**
- Retry count: 3
- Low-level retries: 10
- Retry delay: 1s, exponential

### Troubleshooting Rclone

**Provider Not Supported:**
- Update rclone binary
- Check provider documentation
- Use generic S3/WebDAV interface

**Authentication Failed:**
- Regenerate credentials
- Check token expiration
- Re-authenticate

**Slow Transfers:**
- Increase chunk size
- Adjust parallel transfers
- Check network speed

## Multi-Cloud Configuration

Use multiple cloud providers simultaneously:

1. Configure multiple providers
2. Set backup strategy:
   - Primary/Secondary
   - Load balancing
   - Redundant copies
3. Configure failover

### Backup Strategy

**Primary/Secondary:**
- Primary: Fast, cheap provider (Google Drive)
- Secondary: Durable provider (S3)
- Fallback on primary failure

**Redundant Copies:**
- Upload to all providers
- Verify all uploads
- Restore from fastest

**Load Balancing:**
- Distribute backups across providers
- Balance based on quota/cost
- Optimize upload speed

## Bandwidth Management

### Upload Throttling

```
Settings → Cloud Storage → Bandwidth
```

**Options:**
- Unlimited (default)
- Limit to N MB/s
- Adaptive (based on network)
- Schedule-based limits

### Metered Networks

**Configuration:**
- Skip uploads on metered
- Warn before upload
- Allow with confirmation
- Custom limit for metered

## Security Best Practices

1. **Always use HTTPS/TLS** for WebDAV
2. **Enable encryption** before cloud upload
3. **Use app passwords** instead of main passwords
4. **Rotate credentials** regularly
5. **Limit permissions** to minimum required
6. **Monitor access logs** on cloud provider
7. **Enable 2FA** on cloud accounts
8. **Test restores** periodically

## Quota Management

### Monitor Quota

```
Dashboard → Cloud Storage → Quota
```

**Display:**
- Total quota
- Used space
- Available space
- ObsidianBackup usage

### Quota Alerts

**Configure:**
- Alert at 80% usage
- Alert at 90% usage
- Critical at 95% usage
- Actions: email, notification, pause backups

## Next Steps

- [Backup Configuration](backup-configuration.md) - Configure backup settings
- [Automation Guide](automation.md) - Automate cloud sync
- [Troubleshooting](troubleshooting.md) - Common cloud issues
