# Rclone Quick Start Guide

## Setup (5 minutes)

### Step 1: Install rclone binary (Development)

For development/testing with Termux:

```bash
# Install Termux from F-Droid
# Open Termux and run:
pkg install rclone

# Verify installation
rclone version
```

### Step 2: Configure a remote

```bash
# Interactive configuration
rclone config

# Follow prompts:
# n) New remote
# name> gdrive
# Storage> drive
# client_id> (press enter to use default)
# client_secret> (press enter)
# scope> 1 (full drive access)
# Follow OAuth flow...
```

### Step 3: Test the remote

```bash
# List remote
rclone lsd gdrive:

# Create test file
echo "Hello rclone" > test.txt
rclone copy test.txt gdrive:test/

# Verify
rclone ls gdrive:test/
```

## Usage in Code

### Example 1: Upload Backup to Google Drive

```kotlin
import com.obsidianbackup.cloud.rclone.backends.RcloneGoogleDriveProvider
import com.obsidianbackup.plugins.interfaces.CloudConfig

// Create provider
val provider = RcloneGoogleDriveProvider(context)

// Configure (using existing rclone config)
val config = CloudConfig(
    providerId = "rclone-gdrive",
    credentials = mapOf(
        "token" to """{"access_token":"...","refresh_token":"..."}"""
    )
)

provider.initialize(config)

// Upload backup
val backupFile = File("/sdcard/Download/backup.tar.zst")
val result = provider.uploadFile(
    localFile = backupFile,
    remotePath = "backups/2024-01-15-backup.tar.zst"
)

if (result.success) {
    println("Backup uploaded successfully!")
} else {
    println("Upload failed: ${result.error}")
}
```

### Example 2: List and Download Backups

```kotlin
// List all backups
val backups = provider.listFiles("backups/")
println("Found ${backups.size} backups:")

backups.forEach { file ->
    println("  ${file.path} - ${file.size / 1024 / 1024} MB")
}

// Download latest backup
val latestBackup = backups.maxByOrNull { it.lastModified }
if (latestBackup != null) {
    val downloadPath = File(context.cacheDir, "restored-backup.tar.zst")
    
    provider.downloadFile(
        remotePath = latestBackup.path,
        localFile = downloadPath
    )
    
    println("Downloaded to: ${downloadPath.absolutePath}")
}
```

### Example 3: Monitor Upload Progress

```kotlin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

// Launch coroutine to observe progress
launch {
    provider.observeTransferProgress().collect { progress ->
        val percent = (progress.bytesTransferred * 100 / progress.totalBytes).toInt()
        val speedMBps = progress.speedBps / 1024 / 1024
        
        println("Upload progress: $percent% ($speedMBps MB/s)")
    }
}

// Start upload
provider.uploadFile(backupFile, remotePath)
```

### Example 4: Using the Factory

```kotlin
import com.obsidianbackup.cloud.rclone.RcloneProviderFactory
import com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType

// Create Dropbox provider
val dropboxProvider = RcloneProviderFactory.create(
    context = context,
    providerType = ProviderType.DROPBOX,
    remoteName = "my_dropbox"
)

// Create S3 provider
val s3Provider = RcloneProviderFactory.create(
    context = context,
    providerType = ProviderType.S3
)

// Initialize S3
val s3Config = CloudConfig(
    providerId = "rclone-s3",
    credentials = mapOf(
        "provider_type" to "s3",
        "access_key_id" to "YOUR_ACCESS_KEY",
        "secret_access_key" to "YOUR_SECRET_KEY"
    ),
    region = "us-east-1",
    bucket = "my-backup-bucket"
)

s3Provider.initialize(s3Config)

// Use S3 provider
s3Provider.uploadFile(backupFile, "backups/backup.tar.zst")
```

### Example 5: Error Handling

```kotlin
import com.obsidianbackup.cloud.rclone.RcloneResult
import com.obsidianbackup.cloud.rclone.RcloneErrorCode

suspend fun uploadWithRetry(
    provider: RcloneCloudProvider,
    file: File,
    remotePath: String,
    maxRetries: Int = 3
): CloudResult {
    repeat(maxRetries) { attempt ->
        val result = provider.uploadFile(file, remotePath)
        
        if (result.success) {
            return result
        }
        
        // Check if error is retryable
        val error = result.error
        if (error != null && isRetryableError(error)) {
            println("Attempt ${attempt + 1} failed: $error")
            delay(2000L * (attempt + 1)) // Exponential backoff
            continue
        } else {
            // Non-retryable error
            return result
        }
    }
    
    return CloudResult(
        success = false,
        error = "Upload failed after $maxRetries attempts"
    )
}

fun isRetryableError(error: String): Boolean {
    return error.contains("network", ignoreCase = true) ||
           error.contains("timeout", ignoreCase = true) ||
           error.contains("temporary", ignoreCase = true)
}
```

### Example 6: Multi-Cloud Backup

```kotlin
// Backup to multiple clouds simultaneously
suspend fun multiCloudBackup(
    context: Context,
    backupFile: File
) = coroutineScope {
    val providers = listOf(
        RcloneProviderFactory.create(context, ProviderType.GOOGLE_DRIVE),
        RcloneProviderFactory.create(context, ProviderType.DROPBOX),
        RcloneProviderFactory.create(context, ProviderType.S3)
    )
    
    // Upload to all providers in parallel
    val results = providers.map { provider ->
        async {
            provider.uploadFile(
                localFile = backupFile,
                remotePath = "backups/${backupFile.name}"
            )
        }
    }.awaitAll()
    
    // Check results
    results.forEachIndexed { index, result ->
        if (result.success) {
            println("${providers[index].displayName}: Success")
        } else {
            println("${providers[index].displayName}: Failed - ${result.error}")
        }
    }
    
    // Return true if at least one succeeded
    results.any { it.success }
}
```

## Configuration Examples

### Google Drive with Service Account

```kotlin
val config = CloudConfig(
    providerId = "rclone-gdrive",
    credentials = mapOf(
        "provider_type" to "google_drive",
        "service_account_file" to "/path/to/service-account.json",
        "root_folder_id" to "1A2B3C4D5E6F7G8H9I0J" // Optional
    )
)
```

### Dropbox with Custom App

```kotlin
val config = CloudConfig(
    providerId = "rclone-dropbox",
    credentials = mapOf(
        "provider_type" to "dropbox",
        "token" to """{"access_token":"..."}""",
        "client_id" to "your_app_key",
        "client_secret" to "your_app_secret"
    )
)
```

### Wasabi S3 Compatible

```kotlin
val config = CloudConfig(
    providerId = "rclone-s3",
    credentials = mapOf(
        "provider_type" to "s3",
        "provider" to "Wasabi",
        "access_key_id" to "WASABI_ACCESS_KEY",
        "secret_access_key" to "WASABI_SECRET_KEY"
    ),
    region = "us-east-1",
    endpoint = "s3.wasabisys.com"
)
```

### Backblaze B2

```kotlin
val config = CloudConfig(
    providerId = "rclone-s3",
    credentials = mapOf(
        "provider_type" to "s3",
        "provider" to "B2",
        "access_key_id" to "B2_KEY_ID",
        "secret_access_key" to "B2_APPLICATION_KEY"
    ),
    region = "us-west-001",
    endpoint = "s3.us-west-001.backblazeb2.com"
)
```

## Production Deployment

### 1. Package rclone binary

Download rclone for Android:
```bash
wget https://downloads.rclone.org/v1.65.0/rclone-v1.65.0-android-21-arm64.zip
unzip rclone-v*.zip
```

Place in jniLibs:
```
app/src/main/jniLibs/
├── arm64-v8a/librclone.so
├── armeabi-v7a/librclone.so
├── x86/librclone.so
└── x86_64/librclone.so
```

### 2. Update build.gradle.kts

```kotlin
android {
    // ...
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}
```

### 3. Add to DI module

```kotlin
// di/CloudModule.kt
@Module
@InstallIn(SingletonComponent::class)
object CloudModule {
    
    @Provides
    @Singleton
    fun provideRcloneGoogleDrive(
        @ApplicationContext context: Context
    ): RcloneGoogleDriveProvider {
        return RcloneGoogleDriveProvider(context)
    }
    
    @Provides
    @Singleton
    fun provideRcloneDropbox(
        @ApplicationContext context: Context
    ): RcloneDropboxProvider {
        return RcloneDropboxProvider(context)
    }
    
    @Provides
    @Singleton
    fun provideRcloneS3(
        @ApplicationContext context: Context
    ): RcloneS3Provider {
        return RcloneS3Provider(context)
    }
}
```

### 4. Register plugins

```kotlin
// In PluginRegistry or Application class
class ObsidianBackupApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Register rclone plugins
        PluginRegistry.register(RcloneGoogleDrivePlugin(this))
        PluginRegistry.register(RcloneDropboxPlugin(this))
        PluginRegistry.register(RcloneS3Plugin(this))
    }
}
```

## Troubleshooting

### Binary not found
```kotlin
// Check if rclone is available
val executor = RcloneExecutor(context)
when (val result = executor.initialize()) {
    is RcloneResult.Success -> println("Rclone ready")
    is RcloneResult.Error -> println("Error: ${result.error.message}")
}
```

### Test configuration
```kotlin
// Test a remote configuration
val result = provider.testConnection()
when (result) {
    is Result.success -> println("Connected!")
    is Result.failure -> println("Failed: ${result.exceptionOrNull()?.message}")
}
```

### Enable debug logging
```kotlin
// Enable rclone verbose output
val result = executor.executeCommand(
    listOf("lsjson", "gdrive:", "-vv"), // -vv for very verbose
    progressCallback = { line ->
        println("[RCLONE] $line")
    }
)
```

## Performance Tips

1. **Adjust chunk size** for your network:
   ```kotlin
   additionalOptions["chunk_size"] = "8M" // Smaller for slow networks
   ```

2. **Limit bandwidth** to avoid throttling:
   ```kotlin
   executeCommand(listOf("copy", src, dst, "--bwlimit", "10M"))
   ```

3. **Use compression** for faster uploads:
   ```kotlin
   additionalOptions["compress"] = "gzip"
   ```

4. **Parallel transfers** for multiple files:
   ```kotlin
   executeCommand(listOf("copy", src, dst, "--transfers", "4"))
   ```

## Next Steps

- Read [RCLONE_INTEGRATION.md](./RCLONE_INTEGRATION.md) for architecture details
- Implement OAuth2 flow for user-friendly authentication
- Add RC server mode for better control
- Explore librclone JNI integration for native library approach
- Add more backends (OneDrive, Box, WebDAV, etc.)

## Support

- Rclone docs: https://rclone.org/docs/
- Rclone forum: https://forum.rclone.org/
- GitHub issues: https://github.com/rclone/rclone/issues
