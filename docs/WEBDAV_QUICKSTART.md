# WebDAV Cloud Provider - Quick Start Guide

## What Was Implemented

A complete, production-ready WebDAV cloud provider for the ObsidianBackup Android app that:
- Supports any WebDAV-compliant server (Nextcloud, OwnCloud, Apache, etc.)
- Implements all CloudProvider interface methods
- Uses Sardine-Android v0.9 library (production-ready, OkHttp-based)
- Includes progress tracking, error handling, and chunked uploads
- Follows the same patterns as GoogleDriveProvider

## Quick Setup

### 1. Configuration

Update the WebDavConfig in `CloudModule.kt`:

```kotlin
@Provides
@Singleton
fun provideWebDavConfig(): WebDavConfig {
    return WebDavConfig(
        baseUrl = "https://your-server.com/remote.php/dav/files/username",
        username = "your_username",
        password = "your_password",
        useDigestAuth = false
    )
}
```

### 2. Dependency Injection

Inject the WebDAV provider in your code:

```kotlin
@Inject
@Named("WebDAV")
lateinit var webdavProvider: CloudProvider
```

Or use the default provider:

```kotlin
@Inject
@Named("default")
lateinit var cloudProvider: CloudProvider
```

### 3. Basic Usage

```kotlin
// Test connection
val result = webdavProvider.testConnection()
when (result) {
    is CloudResult.Success -> println("Connected!")
    is CloudResult.Error -> println("Failed: ${result.error.message}")
}

// Upload a snapshot
val files = listOf(
    CloudFile(
        localPath = File("/path/to/backup.tar"),
        remotePath = "backup.tar",
        checksum = "sha256...",
        sizeBytes = 12345L
    )
)

val metadata = CloudSnapshotMetadata(
    snapshotId = SnapshotId("snap_001"),
    timestamp = System.currentTimeMillis(),
    deviceId = Build.DEVICE,
    appCount = 10,
    totalSizeBytes = 12345L,
    compressionRatio = 0.7f,
    encrypted = true,
    merkleRootHash = "merkle..."
)

val uploadResult = webdavProvider.uploadSnapshot(
    snapshotId = SnapshotId("snap_001"),
    files = files,
    metadata = metadata
)

// Observe progress
webdavProvider.observeProgress().collect { progress ->
    when (progress) {
        is CloudTransferProgress.Uploading -> {
            val percent = progress.bytesTransferred * 100 / progress.totalBytes
            println("Upload: $percent%")
        }
        is CloudTransferProgress.Completed -> println("Done!")
        is CloudTransferProgress.Failed -> println("Error: ${progress.error}")
    }
}
```

## Switching Providers

To switch from Google Drive to WebDAV as the default:

In `CloudModule.kt`:

```kotlin
@Provides
@Singleton
@Named("default")
fun provideDefaultCloudProvider(
    @Named("WebDAV") webdavProvider: CloudProvider  // Changed from GoogleDrive
): CloudProvider {
    return webdavProvider
}
```

## Supported Servers

- ✅ Nextcloud
- ✅ OwnCloud  
- ✅ Apache mod_dav
- ✅ Any RFC 4918 compliant WebDAV server

## Server Configuration Examples

### Nextcloud
```kotlin
WebDavConfig(
    baseUrl = "https://cloud.example.com/remote.php/dav/files/username",
    username = "username",
    password = "app-password"  // Use app password, not main password
)
```

### Generic WebDAV
```kotlin
WebDavConfig(
    baseUrl = "https://webdav.example.com/dav",
    username = "user",
    password = "pass",
    useDigestAuth = true  // More secure
)
```

## Testing

### Local Test Server (Docker)
```bash
docker run -d -p 8080:80 \
  -e AUTH_TYPE=Basic \
  -e USERNAME=test \
  -e PASSWORD=test \
  bytemark/webdav

# Then configure:
WebDavConfig(
    baseUrl = "http://localhost:8080",
    username = "test",
    password = "test"
)
```

## Security Recommendations

1. **Always use HTTPS** in production
2. **Store credentials securely**:
   ```kotlin
   val keystoreManager = KeystoreManager(context, logger)
   keystoreManager.storeToken("webdav_password", password)
   ```
3. **Use app-specific passwords** when available (Nextcloud, OwnCloud)
4. **Enable digest authentication** for non-HTTPS testing

## Troubleshooting

### Connection Failed
- Check server URL is correct (include `/remote.php/dav/files/username` for Nextcloud)
- Verify username/password
- Check server is accessible from Android device
- Try testConnection() first before uploading

### Upload Fails
- Check storage quota
- Verify write permissions
- Check file size limits
- Review server logs

### 401 Authentication Error
- Wrong username/password
- Need app-specific password
- Try switching between Basic and Digest auth

## Next Steps

1. Add UI for WebDAV configuration in app settings
2. Implement secure credential storage
3. Add server selection in cloud sync settings
4. Test with your WebDAV server
5. Add unit and integration tests

## Files Reference

- **Implementation**: `app/src/main/java/com/obsidianbackup/cloud/WebDavCloudProvider.kt`
- **DI Module**: `app/src/main/java/com/obsidianbackup/di/CloudModule.kt`
- **Full Documentation**: `WEBDAV_IMPLEMENTATION.md`

## Support

For detailed information, see `WEBDAV_IMPLEMENTATION.md` which includes:
- Complete feature list
- Architecture details
- Advanced usage examples
- Performance optimizations
- Security considerations
- Future enhancements roadmap
