# Rclone Integration for ObsidianBackup

## Overview

This implementation provides multi-cloud backup support for ObsidianBackup using **rclone**, a powerful command-line tool that supports 40+ cloud storage providers. The integration uses rclone's native binary execution pattern, compatible with Android's security model.

## Architecture

### Integration Approach

After extensive research of Android rclone integration patterns (RCX, Round Sync, RSAF), we chose the **native binary execution** approach:

```
[Android App Layer]
       ↓
[RcloneCloudProvider] ← Abstract base class
       ↓
[RcloneExecutor] ← Binary execution wrapper
       ↓
[ProcessBuilder] ← Execute rclone binary
       ↓
[rclone binary] ← Native executable (ARM/x86)
       ↓
[Cloud Storage] ← Google Drive, Dropbox, S3, etc.
```

### Key Components

1. **RcloneExecutor** (`cloud/rclone/RcloneExecutor.kt`)
   - Locates and executes rclone binary
   - Handles process management and output parsing
   - Supports progress tracking via stderr parsing
   - Maps rclone exit codes to error types

2. **RcloneConfigManager** (`cloud/rclone/RcloneConfigManager.kt`)
   - Manages `rclone.conf` file securely
   - Creates/updates/deletes remote configurations
   - Handles INI-style config format
   - Stores OAuth2 tokens and credentials

3. **RcloneCloudProvider** (`cloud/rclone/RcloneCloudProvider.kt`)
   - Abstract base class for all rclone backends
   - Implements CloudProvider interface
   - Maps operations to rclone commands:
     * `uploadFile` → `rclone copyto`
     * `downloadFile` → `rclone copyto`
     * `listFiles` → `rclone lsjson`
     * `deleteFile` → `rclone deletefile`
   - Parses JSON output from rclone
   - Tracks transfer progress

4. **Backend Implementations**
   - `RcloneGoogleDriveProvider` - Google Drive (OAuth2 + Service Accounts)
   - `RcloneDropboxProvider` - Dropbox (OAuth2)
   - `RcloneS3Provider` - AWS S3 and compatible (access key/secret)

5. **Plugin Wrappers**
   - `RcloneGoogleDrivePlugin`
   - `RcloneDropboxPlugin`
   - `RcloneS3Plugin`

## Rclone Binary Packaging

### Option 1: jniLibs (Recommended for Play Store)

Package rclone binary in `jniLibs` directory for each architecture:

```
app/src/main/jniLibs/
├── armeabi-v7a/
│   └── librclone.so
├── arm64-v8a/
│   └── librclone.so
├── x86/
│   └── librclone.so
└── x86_64/
    └── librclone.so
```

**Steps:**
1. Download rclone binaries for Android from https://rclone.org/downloads/
2. Rename `rclone` to `librclone.so` (Android requires `.so` extension)
3. Place in appropriate architecture folders
4. Binary will be extracted to `applicationInfo.nativeLibraryDir` at install time

### Option 2: Assets Extraction

Package in `assets/` and extract at runtime:

```kotlin
// Extract rclone from assets
val rcloneFile = File(context.filesDir, "rclone")
context.assets.open("rclone_arm64").use { input ->
    rcloneFile.outputStream().use { output ->
        input.copyTo(output)
    }
}
rcloneFile.setExecutable(true)
```

### Option 3: External Binary (Development/Termux)

For development or Termux users, install rclone externally:
```bash
pkg install rclone  # In Termux
```

RcloneExecutor will find it in PATH automatically.

## Rclone Commands Mapping

| Operation | Rclone Command | Description |
|-----------|----------------|-------------|
| Upload file | `rclone copyto <local> <remote>` | Copy single file to remote |
| Download file | `rclone copyto <remote> <local>` | Copy single file from remote |
| List files | `rclone lsjson <remote> --recursive` | JSON list with metadata |
| Delete file | `rclone deletefile <remote>` | Delete single file |
| Test connection | `rclone lsd <remote>: --max-depth 1` | List directories (connectivity test) |
| Get version | `rclone version --check=false` | Get rclone version |

## Configuration Format

Rclone uses INI-style configuration in `~/.config/rclone/rclone.conf`:

### Google Drive Example
```ini
[gdrive]
type = drive
client_id = <your_client_id>
client_secret = <your_client_secret>
token = {"access_token":"xyz","token_type":"Bearer","refresh_token":"xyz","expiry":"2024-12-31T23:59:59Z"}
scope = drive
```

### Dropbox Example
```ini
[dropbox]
type = dropbox
token = {"access_token":"xyz","token_type":"bearer","refresh_token":"xyz","expiry":"2024-12-31T23:59:59Z"}
```

### S3 Example
```ini
[s3]
type = s3
provider = AWS
access_key_id = <your_access_key>
secret_access_key = <your_secret>
region = us-east-1
```

## Usage

### Initialization

```kotlin
val context: Context = ...

// Create Google Drive provider
val gdriveProvider = RcloneGoogleDriveProvider(context)

// Configure with OAuth2 token
val config = CloudConfig(
    providerId = "rclone-gdrive",
    credentials = mapOf(
        "token" to """{"access_token":"...","refresh_token":"..."}""",
        "client_id" to "your-client-id",
        "client_secret" to "your-client-secret"
    )
)

// Initialize
gdriveProvider.initialize(config)

// Test connection
val testResult = gdriveProvider.testConnection()
```

### File Operations

```kotlin
// Upload file
val localFile = File("/sdcard/backup.tar.zst")
val result = gdriveProvider.uploadFile(
    localFile = localFile,
    remotePath = "backups/backup.tar.zst"
)

// Download file
gdriveProvider.downloadFile(
    remotePath = "backups/backup.tar.zst",
    localFile = File(context.cacheDir, "backup.tar.zst")
)

// List files
val files = gdriveProvider.listFiles("backups/")
files.forEach { file ->
    println("${file.path}: ${file.size} bytes")
}

// Delete file
gdriveProvider.deleteFile("backups/old-backup.tar.zst")
```

### Progress Tracking

```kotlin
// Observe transfer progress
gdriveProvider.observeTransferProgress()
    .collect { progress ->
        println("Transferred: ${progress.bytesTransferred} / ${progress.totalBytes}")
        println("Speed: ${progress.speedBps} bytes/sec")
    }
```

## Authentication

### Google Drive OAuth2

1. Create OAuth2 credentials in Google Cloud Console
2. Obtain authorization code via OAuth2 flow
3. Exchange for access/refresh tokens
4. Store token JSON in config

```kotlin
val token = """
{
    "access_token": "ya29.xxx",
    "token_type": "Bearer",
    "refresh_token": "1//xxx",
    "expiry": "2024-12-31T23:59:59.999Z"
}
"""

val config = CloudConfig(
    providerId = "rclone-gdrive",
    credentials = mapOf("token" to token)
)
```

### Dropbox OAuth2

Similar to Google Drive - obtain OAuth2 token and store in config.

### S3 Access Keys

```kotlin
val config = CloudConfig(
    providerId = "rclone-s3",
    credentials = mapOf(
        "access_key_id" to "AKIAIOSFODNN7EXAMPLE",
        "secret_access_key" to "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    ),
    region = "us-east-1",
    bucket = "my-backup-bucket"
)
```

## Error Handling

Rclone exit codes are mapped to `RcloneErrorCode`:

| Exit Code | Error Type | Retryable | Description |
|-----------|------------|-----------|-------------|
| 0 | Success | - | Operation succeeded |
| 1 | SYNTAX_ERROR | No | Invalid command syntax |
| 2 | FILE_NOT_FOUND | No | File not found |
| 3 | DIRECTORY_NOT_FOUND | No | Directory not found |
| 4 | FILE_EXISTS | No | File already exists |
| 5 | TEMPORARY_ERROR | Yes | Temporary error (network) |
| 6 | LESS_SERIOUS_ERROR | Yes | Less serious error |
| 7 | FATAL_ERROR | No | Fatal error |
| 8 | TRANSFER_EXCEEDED | No | Transfer limit exceeded |
| 9 | NO_FILES_TRANSFERRED | No | No files transferred |

```kotlin
when (val result = provider.uploadFile(file, path)) {
    is RcloneResult.Success -> {
        // Success
    }
    is RcloneResult.Error -> {
        if (result.error.retryable) {
            // Retry operation
        } else {
            // Permanent failure
        }
    }
}
```

## Security Considerations

1. **Config File Permissions**: `rclone.conf` is stored with 0600 permissions (owner read/write only)
2. **Credential Storage**: Consider encrypting OAuth tokens before storage
3. **Binary Verification**: Verify rclone binary checksum before execution
4. **Sandboxing**: All operations run under app's UID, sandboxed by Android
5. **Network Security**: Use HTTPS for all cloud operations (rclone default)

## Performance Optimization

1. **Chunk Size**: Adjust upload chunk size for network conditions
   ```kotlin
   additionalOptions["chunk_size"] = "16M"
   ```

2. **Parallel Transfers**: Use rclone's `--transfers` flag
   ```kotlin
   executeCommand(listOf("copy", src, dst, "--transfers", "4"))
   ```

3. **Bandwidth Limiting**: Control bandwidth usage
   ```kotlin
   executeCommand(listOf("copy", src, dst, "--bwlimit", "1M"))
   ```

4. **Progress Updates**: Reduce stats frequency for less overhead
   ```kotlin
   executeCommand(listOf("copy", src, dst, "--stats", "5s"))
   ```

## Testing

### Mock Binary Testing

For unit tests without rclone binary:

```kotlin
@Test
fun testUploadWithoutBinary() {
    val executor = RcloneExecutor(context)
    
    // Mock binary path
    every { executor.locateRcloneBinary() } returns "/mock/rclone"
    
    // Test operations
}
```

### Integration Testing with Termux

1. Install Termux from F-Droid
2. Install rclone: `pkg install rclone`
3. Configure remote: `rclone config`
4. Run app tests

## Supported Cloud Providers

Via rclone, ObsidianBackup now supports **40+ cloud providers**:

- **Major Clouds**: Google Drive, Dropbox, OneDrive, Box, Mega
- **S3 Compatible**: AWS S3, Wasabi, Backblaze B2, MinIO, DigitalOcean Spaces
- **Enterprise**: Azure Blob, Google Cloud Storage, Oracle Cloud
- **File Protocols**: WebDAV, SFTP, FTP, HTTP
- **Specialized**: pCloud, Yandex Disk, Koofr, Mailru, Jottacloud

See https://rclone.org/ for complete list.

## Future Enhancements

1. **RC Server Mode**: Run rclone as HTTP API server for better control
2. **Librclone JNI**: Use gomobile-compiled rclone library for deeper integration
3. **OAuth2 Helper**: Built-in OAuth2 flow for Google Drive/Dropbox
4. **Config GUI**: Visual configuration builder
5. **Sync Support**: Bidirectional sync with `rclone sync`
6. **Mount Support**: Mount cloud storage as virtual filesystem (requires root)

## Troubleshooting

### Binary Not Found

**Error**: `rclone binary not found`

**Solutions**:
1. Package binary in `jniLibs/` (see Packaging section)
2. Install rclone in Termux: `pkg install rclone`
3. Check binary permissions: `chmod +x /path/to/rclone`

### Authentication Failed

**Error**: `AUTHENTICATION_ERROR`

**Solutions**:
1. Verify OAuth2 token is valid and not expired
2. Check client_id and client_secret
3. Ensure proper scopes are requested
4. Re-authenticate via `rclone config`

### Permission Denied

**Error**: `PERMISSION_DENIED`

**Solutions**:
1. Grant Android storage permissions
2. Check cloud provider permissions
3. Verify app has network access
4. Check file/directory permissions

### Network Timeout

**Error**: `TEMPORARY_ERROR` or `TIMEOUT`

**Solutions**:
1. Increase timeout: `timeout = 300000` (5 minutes)
2. Check network connectivity
3. Reduce chunk size for slow networks
4. Retry operation (error is marked retryable)

## References

- [Rclone Official Documentation](https://rclone.org/docs/)
- [Rclone Android Integration Discussion](https://forum.rclone.org/t/official-support-for-the-android-platform/25024)
- [RCX - Rclone for Android](https://github.com/x0b/rcx)
- [Round Sync - Rclone for Android](https://github.com/newhinton/Round-Sync)
- [RSAF - Rclone Storage Access Framework](https://github.com/chenxiaolong/RSAF)
- [Librclone GoMobile](https://github.com/rclone/rclone/tree/master/librclone)

## License

This rclone integration follows ObsidianBackup's license. Rclone itself is licensed under MIT License.
