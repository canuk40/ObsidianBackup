# Additional Cloud Storage Providers for ObsidianBackup

## Overview

This document describes the implementation of 6 additional enterprise-grade cloud storage providers for ObsidianBackup, extending the existing Google Drive and WebDAV support with comprehensive OAuth2 authentication and multi-account management.

## Implemented Providers

### 1. **Box.com** 
- **File**: `BoxCloudProvider.kt`
- **OAuth2 Endpoint**: `https://account.box.com/api/oauth2/authorize`
- **API Base**: `https://api.box.com/2.0`
- **Features**: Enterprise security, large file uploads, folder management
- **Chunk Size**: 8 MB
- **Max File Size**: Unlimited
- **Special Features**: Metadata support, shared links, retention policies

### 2. **Azure Blob Storage**
- **File**: `AzureBlobProvider.kt`
- **OAuth2 Endpoint**: `https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize`
- **API Base**: `https://{account}.blob.core.windows.net`
- **Features**: Microsoft integration, geo-redundancy, versioning
- **Chunk Size**: 4 MB
- **Max File Size**: 190.7 TB per blob
- **Special Features**: Tiered storage (hot/cool/archive), immutable storage, lifecycle management

### 3. **Backblaze B2**
- **File**: `BackblazeB2Provider.kt`
- **OAuth2 Endpoint**: Application Key based
- **API Base**: `https://api.backblazeb2.com`
- **Features**: Cost-effective, S3-compatible, simple pricing
- **Chunk Size**: 5 MB (minimum for multipart)
- **Max File Size**: 10 TB per file
- **Special Features**: Lifecycle rules, file versioning, bandwidth alliances

### 4. **Alibaba Cloud OSS**
- **File**: `AlibabaOSSProvider.kt`
- **OAuth2 Endpoint**: RAM (Resource Access Management)
- **API Base**: `https://{bucket}.oss-{region}.aliyuncs.com`
- **Features**: China market leader, low latency in Asia, CDN integration
- **Chunk Size**: 5 MB
- **Max File Size**: 48.8 TB per object
- **Special Features**: Image processing, video transcoding, data transfer acceleration

### 5. **DigitalOcean Spaces**
- **File**: `DigitalOceanSpacesProvider.kt`
- **OAuth2 Endpoint**: S3-compatible authentication
- **API Base**: `https://{region}.digitaloceanspaces.com`
- **Features**: Developer-friendly, predictable pricing, S3-compatible API
- **Chunk Size**: 5 MB
- **Max File Size**: 5 TB per object
- **Special Features**: Built-in CDN, spaces management, lifecycle policies

### 6. **Oracle Cloud Object Storage**
- **File**: `OracleCloudProvider.kt`
- **OAuth2 Endpoint**: OCI OAuth2
- **API Base**: `https://objectstorage.{region}.oraclecloud.com`
- **Features**: Enterprise-grade, free tier, global regions
- **Chunk Size**: 10 MB
- **Max File Size**: 10 TB per object
- **Special Features**: Object versioning, retention rules, replication policies

## Architecture

### Core Components

#### 1. OAuth2Provider Base Class
**Location**: `com.obsidianbackup.cloud.oauth.OAuth2Provider`

Abstract base class providing:
- Token management (storage, retrieval, refresh)
- Authorization URL generation
- Code-to-token exchange
- Multi-account support
- Secure token storage via KeystoreManager

```kotlin
abstract class OAuth2Provider(
    protected val context: Context,
    protected val keystoreManager: KeystoreManager,
    protected val logger: ObsidianLogger
) {
    abstract val providerId: String
    abstract val displayName: String
    abstract val authorizationEndpoint: String
    abstract val tokenEndpoint: String
    abstract val clientId: String
    abstract val clientSecret: String
    abstract val scopes: List<String>
    abstract val redirectUri: String
}
```

#### 2. CloudProvider Interface
**Location**: `com.obsidianbackup.cloud.CloudProvider`

All providers implement this interface:
- `testConnection()` - Verify connectivity and credentials
- `uploadSnapshot()` - Upload complete backup snapshot
- `downloadSnapshot()` - Download and restore snapshot
- `listSnapshots()` - List available backups with filters
- `deleteSnapshot()` - Remove backup from cloud
- `getStorageQuota()` - Get storage usage information
- `observeProgress()` - Real-time transfer progress
- `syncCatalog()` - Sync backup catalog metadata
- `uploadFile()` / `downloadFile()` - Single file operations

#### 3. Multi-Account Support

Each provider supports multiple accounts via the `accountId` parameter:

```kotlin
class BoxCloudProvider(
    context: Context,
    keystoreManager: KeystoreManager,
    logger: ObsidianLogger,
    private val accountId: String = "default"
) : CloudProvider
```

Accounts are managed through:
- `listAccounts()` - Get all configured accounts
- `removeAccount(accountId)` - Remove account and credentials
- `isAuthenticated(accountId)` - Check authentication status

### Configuration UI

**Location**: `com.obsidianbackup.ui.cloud.CloudProviderConfigScreen`

Jetpack Compose-based unified configuration interface featuring:
- Provider selection dialog
- Account management per provider
- Connection testing
- Storage quota visualization
- Multi-account switching
- Provider-specific settings

```kotlin
@Composable
fun CloudProviderConfigScreen(
    viewModel: CloudConfigViewModel,
    onNavigateBack: () -> Unit
)
```

**Features**:
- Material3 design
- Real-time status updates
- Progress indicators
- Error handling with snackbars
- Empty state guidance
- Provider icons and descriptions

## OAuth2 Flow

### Standard Flow

1. **User initiates authentication**
   ```kotlin
   val authUrl = oauth2Provider.buildAuthorizationUrl()
   startActivity(Intent(Intent.ACTION_VIEW, authUrl))
   ```

2. **User authorizes in browser**
   - Redirects to provider's authorization page
   - User grants permissions
   - Provider redirects to app with authorization code

3. **App receives callback**
   ```kotlin
   // In activity handling obsidianbackup://oauth/{provider}
   val code = intent.data?.getQueryParameter("code")
   val accountId = "user@example.com" // or generate unique ID
   ```

4. **Exchange code for token**
   ```kotlin
   when (val result = oauth2Provider.exchangeCodeForToken(code, accountId)) {
       is OAuth2Result.Success -> {
           // Token stored securely
           // Provider ready to use
       }
       is OAuth2Result.Error -> {
           // Handle error
       }
   }
   ```

5. **Token refresh (automatic)**
   ```kotlin
   // Providers automatically refresh expired tokens
   val tokenResult = oauth2Provider.getValidToken(accountId)
   ```

## Provider-Specific Implementation Details

### Box.com

**OAuth2 Configuration**:
```kotlin
class BoxOAuth2Provider : OAuth2Provider() {
    override val authorizationEndpoint = "https://account.box.com/api/oauth2/authorize"
    override val tokenEndpoint = "https://api.box.com/oauth2/token"
    override val scopes = listOf("root_readwrite")
    override val redirectUri = "obsidianbackup://oauth/box"
}
```

**Key Features**:
- Folder-based organization (each snapshot = folder)
- Chunked uploads for large files
- Search API for finding snapshots
- Metadata storage in folder descriptions
- Enterprise security features

**API Calls**:
- Create folder: `POST /folders`
- Upload file: `POST /files/content` (multipart)
- Download file: `GET /files/{id}/content`
- Search: `GET /search?query={query}`
- Delete folder: `DELETE /folders/{id}?recursive=true`

### Azure Blob Storage

**OAuth2 Configuration**:
```kotlin
class AzureOAuth2Provider : OAuth2Provider() {
    override val authorizationEndpoint = 
        "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize"
    override val tokenEndpoint = 
        "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token"
    override val scopes = listOf("https://storage.azure.com/user_impersonation")
}
```

**Key Features**:
- Container-based storage (snapshots as blobs)
- Block blob uploads with staging
- Blob metadata and tags
- Lease-based locking
- Versioning support

**API Calls**:
- Create container: `PUT /{container}?restype=container`
- Upload block: `PUT /{container}/{blob}?comp=block&blockid={id}`
- Commit blocks: `PUT /{container}/{blob}?comp=blocklist`
- Download blob: `GET /{container}/{blob}`
- Delete blob: `DELETE /{container}/{blob}`

### Backblaze B2

**Authentication**:
Uses application keys instead of traditional OAuth2:
```kotlin
// Authorize account
POST https://api.backblazeb2.com/b2api/v2/b2_authorize_account
Authorization: Basic {base64(keyId:applicationKey)}
```

**Key Features**:
- Bucket-based organization
- Large file uploads via parts
- File info metadata storage
- Lifecycle rules for cost optimization
- S3-compatible API available

**API Calls**:
- List buckets: `POST /b2_list_buckets`
- Upload file: `POST /b2_upload_file` (small files)
- Start large file: `POST /b2_start_large_file`
- Upload part: `POST /b2_upload_part`
- Finish large file: `POST /b2_finish_large_file`
- Download: `GET /file/{bucketName}/{fileName}`

### Alibaba Cloud OSS

**OAuth2 Configuration**:
```kotlin
class AlibabaOAuth2Provider : OAuth2Provider() {
    override val authorizationEndpoint = 
        "https://signin.aliyun.com/oauth2/v1/authorize"
    override val tokenEndpoint = 
        "https://oauth.aliyun.com/v1/token"
    override val scopes = listOf("oss:*")
}
```

**Key Features**:
- Bucket-based storage
- Multipart upload for large files
- Object metadata support
- CDN acceleration
- Image/video processing

**API Calls**:
- Put object: `PUT /{bucket}/{object}`
- Initiate multipart: `POST /{object}?uploads`
- Upload part: `PUT /{object}?partNumber={n}&uploadId={id}`
- Complete multipart: `POST /{object}?uploadId={id}`
- Get object: `GET /{bucket}/{object}`
- Delete object: `DELETE /{bucket}/{object}`

### DigitalOcean Spaces

**Authentication**:
S3-compatible with Access Key/Secret Key:
```kotlin
// AWS Signature Version 4 authentication
Authorization: AWS4-HMAC-SHA256 Credential=...
```

**Key Features**:
- S3-compatible API
- Built-in CDN (SpacesCDN)
- Simple pricing model
- CORS configuration
- Lifecycle policies

**API Calls**:
- Create bucket: `PUT /{bucket}`
- Upload object: `PUT /{bucket}/{key}`
- Multipart upload: Standard S3 multipart flow
- Download object: `GET /{bucket}/{key}`
- Delete object: `DELETE /{bucket}/{key}`
- List objects: `GET /{bucket}?list-type=2`

### Oracle Cloud Object Storage

**OAuth2 Configuration**:
```kotlin
class OracleOAuth2Provider : OAuth2Provider() {
    override val authorizationEndpoint = 
        "https://login.{region}.oraclecloud.com/v1/oauth2/authorize"
    override val tokenEndpoint = 
        "https://login.{region}.oraclecloud.com/v1/oauth2/token"
    override val scopes = listOf("urn:opc:idm:__myscopes__")
}
```

**Key Features**:
- Namespace-based organization
- Bucket storage with versioning
- Multipart uploads
- Private/public access
- Cross-region replication

**API Calls**:
- Create bucket: `POST /n/{namespace}/b/`
- Put object: `PUT /n/{namespace}/b/{bucket}/o/{object}`
- Multipart upload: `POST /n/{namespace}/b/{bucket}/u/`
- Upload part: `PUT /n/{namespace}/b/{bucket}/u/{uploadId}/{partNum}`
- Commit: `POST /n/{namespace}/b/{bucket}/u/{uploadId}`
- Get object: `GET /n/{namespace}/b/{bucket}/o/{object}`

## Usage Examples

### Adding a Provider

```kotlin
// 1. Create provider instance
val boxProvider = BoxCloudProvider(
    context = applicationContext,
    keystoreManager = keystoreManager,
    logger = logger,
    accountId = "work_account"
)

// 2. Initiate OAuth2 flow
val intent = boxProvider.oauth2Provider.getAuthorizationIntent()
startActivityForResult(intent, REQUEST_CODE_BOX_AUTH)

// 3. Handle callback (in activity)
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_BOX_AUTH && resultCode == RESULT_OK) {
        val code = data?.data?.getQueryParameter("code")
        code?.let {
            lifecycleScope.launch {
                when (val result = boxProvider.oauth2Provider.exchangeCodeForToken(it, "work_account")) {
                    is OAuth2Result.Success -> {
                        Toast.makeText(this, "Box connected!", Toast.LENGTH_SHORT).show()
                    }
                    is OAuth2Result.Error -> {
                        Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
```

### Testing Connection

```kotlin
suspend fun testProviderConnection(provider: CloudProvider) {
    when (val result = provider.testConnection()) {
        is CloudResult.Success -> {
            val info = result.data
            println("Connected! Latency: ${info.latencyMs}ms")
            println("Server: ${info.serverVersion}")
        }
        is CloudResult.Error -> {
            println("Connection failed: ${result.error.message}")
        }
    }
}
```

### Uploading a Snapshot

```kotlin
suspend fun uploadBackup(
    provider: CloudProvider,
    snapshotId: SnapshotId,
    files: List<File>
) {
    // Prepare cloud files with checksums
    val cloudFiles = files.map { file ->
        CloudFile(
            localPath = file,
            remotePath = "snapshots/${snapshotId.value}/${file.name}",
            checksum = calculateChecksum(file),
            sizeBytes = file.length()
        )
    }

    // Create metadata
    val metadata = CloudSnapshotMetadata(
        snapshotId = snapshotId,
        timestamp = System.currentTimeMillis(),
        deviceId = getDeviceId(),
        appCount = getInstalledApps().size,
        totalSizeBytes = files.sumOf { it.length() },
        compressionRatio = 0.7f,
        encrypted = true,
        merkleRootHash = calculateMerkleRoot(files)
    )

    // Observe progress
    launch {
        provider.observeProgress().collect { progress ->
            when (progress) {
                is CloudTransferProgress.Uploading -> {
                    updateUI(progress.bytesTransferred, progress.totalBytes)
                }
                is CloudTransferProgress.Completed -> {
                    showSuccess("Upload complete!")
                }
                is CloudTransferProgress.Failed -> {
                    showError("Upload failed: ${progress.error.message}")
                }
            }
        }
    }

    // Upload
    when (val result = provider.uploadSnapshot(snapshotId, cloudFiles, metadata)) {
        is CloudResult.Success -> {
            val summary = result.data
            println("Uploaded ${summary.filesUploaded} files")
            println("Total: ${formatBytes(summary.bytesUploaded)}")
            println("Speed: ${formatBytes(summary.averageSpeed)}/s")
        }
        is CloudResult.Error -> {
            println("Upload failed: ${result.error.message}")
            if (result.error.retryable) {
                // Retry logic
            }
        }
    }
}
```

### Downloading a Snapshot

```kotlin
suspend fun downloadBackup(
    provider: CloudProvider,
    snapshotId: SnapshotId,
    destinationDir: File
) {
    when (val result = provider.downloadSnapshot(
        snapshotId = snapshotId,
        destinationDir = destinationDir,
        verifyIntegrity = true
    )) {
        is CloudResult.Success -> {
            val summary = result.data
            println("Downloaded ${summary.filesDownloaded} files")
            println("Verification: ${summary.verificationResult}")
        }
        is CloudResult.Error -> {
            println("Download failed: ${result.error.message}")
        }
    }
}
```

### Listing Snapshots

```kotlin
suspend fun listBackups(provider: CloudProvider, deviceId: String) {
    val filter = CloudSnapshotFilter(
        deviceId = deviceId,
        afterTimestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000), // Last 30 days
        maxResults = 50
    )

    when (val result = provider.listSnapshots(filter)) {
        is CloudResult.Success -> {
            result.data.forEach { snapshot ->
                println("Snapshot: ${snapshot.snapshotId.value}")
                println("Date: ${formatDate(snapshot.timestamp)}")
                println("Size: ${formatBytes(snapshot.sizeBytes)}")
                println("Files: ${snapshot.fileCount}")
            }
        }
        is CloudResult.Error -> {
            println("List failed: ${result.error.message}")
        }
    }
}
```

### Multi-Account Management

```kotlin
// Add second account for Box
val boxProvider2 = BoxCloudProvider(
    context = applicationContext,
    keystoreManager = keystoreManager,
    logger = logger,
    accountId = "personal_account"
)

// List all Box accounts
val accounts = boxProvider.oauth2Provider.listAccounts()
println("Box accounts: $accounts")

// Switch between accounts
suspend fun useAccount(accountId: String, provider: CloudProvider) {
    if (provider.oauth2Provider.isAuthenticated(accountId)) {
        // Use this account for operations
        testProviderConnection(provider)
    } else {
        // Need to authenticate
        initiateOAuth2Flow(accountId)
    }
}

// Remove account
suspend fun removeAccount(accountId: String, provider: CloudProvider) {
    provider.oauth2Provider.removeAccount(accountId)
    println("Account $accountId removed")
}
```

### Storage Quota Management

```kotlin
suspend fun checkStorageQuota(provider: CloudProvider) {
    when (val result = provider.getStorageQuota()) {
        is CloudResult.Success -> {
            val quota = result.data
            val usedPercent = (quota.usedBytes.toFloat() / quota.totalBytes * 100).toInt()
            
            println("Storage Usage:")
            println("Total: ${formatBytes(quota.totalBytes)}")
            println("Used: ${formatBytes(quota.usedBytes)} ($usedPercent%)")
            println("Available: ${formatBytes(quota.availableBytes)}")
            
            if (usedPercent > 90) {
                showWarning("Storage nearly full! Consider upgrading or cleaning up old backups.")
            }
        }
        is CloudResult.Error -> {
            println("Failed to get quota: ${result.error.message}")
        }
    }
}
```

## Configuration

### AndroidManifest.xml

Add intent filters for OAuth2 callbacks:

```xml
<activity
    android:name=".ui.cloud.OAuth2CallbackActivity"
    android:exported="true"
    android:launchMode="singleTop">
    
    <!-- Box.com -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="obsidianbackup"
            android:host="oauth"
            android:pathPrefix="/box" />
    </intent-filter>
    
    <!-- Azure -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="obsidianbackup"
            android:host="oauth"
            android:pathPrefix="/azure" />
    </intent-filter>
    
    <!-- Backblaze B2 -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="obsidianbackup"
            android:host="oauth"
            android:pathPrefix="/backblaze" />
    </intent-filter>
    
    <!-- Alibaba Cloud OSS -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="obsidianbackup"
            android:host="oauth"
            android:pathPrefix="/alibaba" />
    </intent-filter>
    
    <!-- DigitalOcean Spaces -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="obsidianbackup"
            android:host="oauth"
            android:pathPrefix="/digitalocean" />
    </intent-filter>
    
    <!-- Oracle Cloud -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="obsidianbackup"
            android:host="oauth"
            android:pathPrefix="/oracle" />
    </intent-filter>
</activity>
```

### OAuth2 Credentials

Each provider requires OAuth2 credentials. Update the following in each provider:

**Box.com**:
1. Create app at https://app.box.com/developers/console
2. Update `clientId` and `clientSecret` in `BoxOAuth2Provider`
3. Set redirect URI: `obsidianbackup://oauth/box`

**Azure**:
1. Register app at https://portal.azure.com → App registrations
2. Update `clientId`, `clientSecret`, and `tenant` in `AzureOAuth2Provider`
3. Set redirect URI: `obsidianbackup://oauth/azure`

**Backblaze B2**:
1. Create application key at https://secure.backblaze.com/b2_buckets.htm
2. Update `applicationKeyId` and `applicationKey` in `BackblazeB2Provider`

**Alibaba Cloud OSS**:
1. Create RAM user at https://ram.console.aliyun.com
2. Create OAuth2 app and get credentials
3. Update `clientId` and `clientSecret` in `AlibabaOAuth2Provider`

**DigitalOcean Spaces**:
1. Generate Spaces access keys at https://cloud.digitalocean.com/account/api/tokens
2. Update `accessKey` and `secretKey` in `DigitalOceanSpacesProvider`

**Oracle Cloud**:
1. Create OAuth2 app at https://cloud.oracle.com
2. Update `clientId` and `clientSecret` in `OracleOAuth2Provider`

### Dependency Versions

The following dependencies are added to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Box SDK
    implementation("com.box:box-android-sdk:5.1.0")
    
    // Azure Storage SDK
    implementation("com.azure:azure-storage-blob:12.23.0")
    implementation("com.azure:azure-identity:1.10.0")
    
    // Backblaze B2 (S3-compatible)
    implementation("software.amazon.awssdk:s3:2.20.0")
    
    // Alibaba Cloud OSS SDK
    implementation("com.aliyun.dpa:oss-android-sdk:2.9.13")
    
    // DigitalOcean Spaces (S3-compatible)
    implementation("com.amazonaws:aws-android-sdk-s3:2.73.0")
    
    // Oracle Cloud Infrastructure SDK
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage:3.27.0")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.27.0")
}
```

## Security Considerations

### Token Storage
- All OAuth2 tokens stored using Android KeyStore
- Encrypted at rest using AES-256-GCM
- Biometric protection available for token access
- Automatic token cleanup on account removal

### Network Security
- TLS 1.3 enforced for all connections
- Certificate pinning for critical endpoints
- Request signing for authenticated APIs
- Timeout protection (30s connect, 300s read/write)

### Data Protection
- Optional end-to-end encryption of backups
- SHA-256 checksums for integrity verification
- Merkle tree validation for snapshot consistency
- No local storage of unencrypted credentials

### Privacy
- No telemetry sent to cloud providers beyond API calls
- User data never leaves device unencrypted (if encryption enabled)
- Account data isolated per provider and accountId
- Compliance with GDPR, CCPA, and regional regulations

## Performance Optimizations

### Chunked Uploads
Each provider optimized for its chunk size limits:
- **Box**: 8 MB chunks
- **Azure**: 4 MB blocks (100 MB max per PUT)
- **Backblaze**: 5 MB minimum for multipart
- **Alibaba**: 5 MB recommended
- **DigitalOcean**: 5 MB (S3 standard)
- **Oracle**: 10 MB for optimal performance

### Parallel Transfers
- Multiple files uploaded/downloaded concurrently (configurable)
- Thread pool size: min(CPU cores * 2, 8)
- Backpressure handling for memory management
- Automatic retry with exponential backoff

### Caching
- Token caching in memory (5-minute expiry buffer)
- Metadata caching for snapshot lists (configurable TTL)
- Connection pooling for HTTP clients
- DNS caching for faster lookups

### Network Optimization
- Gzip compression for metadata
- Binary format for large files (no encoding overhead)
- Keep-alive connections
- Adaptive bitrate based on network conditions

## Error Handling

### Error Codes
```kotlin
enum class ErrorCode {
    AUTHENTICATION_FAILED,  // OAuth2/credentials invalid
    NETWORK_ERROR,          // Connection issues
    QUOTA_EXCEEDED,         // Storage limit reached
    FILE_NOT_FOUND,         // Snapshot/file doesn't exist
    CHECKSUM_MISMATCH,      // Data corruption detected
    TIMEOUT,                // Operation took too long
    PERMISSION_DENIED,      // Access forbidden
    UNKNOWN                 // Unexpected error
}
```

### Retry Logic
```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> CloudResult<T>
): CloudResult<T> {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        when (val result = block()) {
            is CloudResult.Success -> return result
            is CloudResult.Error -> {
                if (!result.error.retryable) return result
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }
    return block() // Final attempt
}
```

### User-Facing Error Messages
```kotlin
fun CloudError.toUserMessage(): String {
    return when (code) {
        ErrorCode.AUTHENTICATION_FAILED -> 
            "Authentication failed. Please re-connect your account."
        ErrorCode.NETWORK_ERROR -> 
            "Network error. Please check your connection and try again."
        ErrorCode.QUOTA_EXCEEDED -> 
            "Storage quota exceeded. Please free up space or upgrade your plan."
        ErrorCode.FILE_NOT_FOUND -> 
            "Backup not found. It may have been deleted."
        ErrorCode.CHECKSUM_MISMATCH -> 
            "Data corruption detected. Please re-upload the backup."
        ErrorCode.TIMEOUT -> 
            "Operation timed out. Please try again."
        ErrorCode.PERMISSION_DENIED -> 
            "Access denied. Please check your permissions."
        ErrorCode.UNKNOWN -> 
            "An unexpected error occurred: $message"
    }
}
```

## Testing

### Unit Tests
```kotlin
@Test
fun `test OAuth2 token refresh`() = runTest {
    val provider = BoxOAuth2Provider(context, keystoreManager, logger)
    
    // Mock expired token
    val expiredToken = OAuth2Token(
        accessToken = "old_token",
        refreshToken = "refresh_token",
        expiresIn = 3600,
        tokenType = "Bearer",
        scope = "root_readwrite",
        issuedAt = System.currentTimeMillis() - (2 * 3600 * 1000)
    )
    
    // Store expired token
    provider.storeToken("test", expiredToken)
    
    // Should trigger refresh
    val result = provider.getValidToken("test")
    
    assertTrue(result is OAuth2Result.Success)
    assertNotEquals("old_token", (result as OAuth2Result.Success).data)
}
```

### Integration Tests
```kotlin
@Test
fun `test Box upload and download`() = runTest {
    val provider = BoxCloudProvider(context, keystoreManager, logger, "test")
    
    // Test connection
    val connResult = provider.testConnection()
    assertTrue(connResult is CloudResult.Success)
    
    // Create test snapshot
    val snapshotId = SnapshotId("test_${System.currentTimeMillis()}")
    val testFile = createTestFile()
    val cloudFile = CloudFile(
        localPath = testFile,
        remotePath = "test/${testFile.name}",
        checksum = calculateChecksum(testFile),
        sizeBytes = testFile.length()
    )
    
    // Upload
    val uploadResult = provider.uploadSnapshot(
        snapshotId,
        listOf(cloudFile),
        createTestMetadata(snapshotId)
    )
    assertTrue(uploadResult is CloudResult.Success)
    
    // List and verify
    val listResult = provider.listSnapshots()
    assertTrue(listResult is CloudResult.Success)
    val snapshots = (listResult as CloudResult.Success).data
    assertTrue(snapshots.any { it.snapshotId == snapshotId })
    
    // Download
    val downloadDir = createTempDir()
    val downloadResult = provider.downloadSnapshot(snapshotId, downloadDir, true)
    assertTrue(downloadResult is CloudResult.Success)
    
    // Verify file
    val downloadedFile = File(downloadDir, testFile.name)
    assertTrue(downloadedFile.exists())
    assertEquals(testFile.readText(), downloadedFile.readText())
    
    // Cleanup
    provider.deleteSnapshot(snapshotId)
}
```

## Migration Guide

### From Google Drive

```kotlin
// Old code
val driveProvider = GoogleDriveProvider(context, oauthManager, logger)

// New code - choose provider
val provider: CloudProvider = when (userChoice) {
    "box" -> BoxCloudProvider(context, keystoreManager, logger)
    "azure" -> AzureBlobProvider(context, keystoreManager, logger)
    "backblaze" -> BackblazeB2Provider(context, keystoreManager, logger)
    "alibaba" -> AlibabaOSSProvider(context, keystoreManager, logger)
    "digitalocean" -> DigitalOceanSpacesProvider(context, keystoreManager, logger)
    "oracle" -> OracleCloudProvider(context, keystoreManager, logger)
    else -> GoogleDriveProvider(context, oauthManager, logger)
}

// API calls remain the same - CloudProvider interface unchanged
provider.uploadSnapshot(snapshotId, files, metadata)
```

### Migrating Existing Backups

```kotlin
suspend fun migrateBackups(
    sourceProvider: CloudProvider,
    targetProvider: CloudProvider
) {
    // List all snapshots from source
    val snapshotsResult = sourceProvider.listSnapshots()
    val snapshots = (snapshotsResult as? CloudResult.Success)?.data ?: return
    
    // Migrate each snapshot
    snapshots.forEach { snapshot ->
        println("Migrating ${snapshot.snapshotId.value}...")
        
        // Download from source
        val tempDir = createTempDir()
        when (sourceProvider.downloadSnapshot(snapshot.snapshotId, tempDir)) {
            is CloudResult.Success -> {
                // Upload to target
                val files = tempDir.listFiles()?.map { file ->
                    CloudFile(
                        localPath = file,
                        remotePath = "snapshots/${snapshot.snapshotId.value}/${file.name}",
                        checksum = calculateChecksum(file),
                        sizeBytes = file.length()
                    )
                } ?: emptyList()
                
                targetProvider.uploadSnapshot(
                    snapshot.snapshotId,
                    files,
                    snapshot.metadata
                )
                
                // Cleanup temp files
                tempDir.deleteRecursively()
                
                println("✓ Migrated ${snapshot.snapshotId.value}")
            }
            is CloudResult.Error -> {
                println("✗ Failed to migrate ${snapshot.snapshotId.value}")
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. OAuth2 Authentication Fails
**Symptoms**: "Authentication failed" error, unable to complete OAuth2 flow

**Solutions**:
- Verify client ID and secret are correct
- Check redirect URI matches exactly (including scheme)
- Ensure app is approved/enabled in provider's console
- Clear app data and retry
- Check device time is synchronized

#### 2. Upload/Download Timeouts
**Symptoms**: Operations fail with timeout error

**Solutions**:
- Increase timeout values in provider
- Check network connection stability
- Reduce chunk size for slower connections
- Enable retry logic with exponential backoff

#### 3. Token Refresh Fails
**Symptoms**: "No refresh token available" or refresh returns error

**Solutions**:
- Ensure `access_type=offline` in authorization URL
- Re-authenticate to get new refresh token
- Check provider's token expiration policy
- Verify refresh token scope matches original

#### 4. Quota Exceeded
**Symptoms**: "Storage quota exceeded" error

**Solutions**:
- Check storage quota with `getStorageQuota()`
- Delete old snapshots
- Upgrade storage plan
- Enable compression/deduplication

#### 5. Checksum Mismatch
**Symptoms**: Download fails verification, "Data corruption detected"

**Solutions**:
- Retry download
- Check network stability
- Verify file wasn't modified in cloud
- Re-upload if source file available

### Debug Logging

Enable debug logging for troubleshooting:

```kotlin
// In provider implementation
private val logger = ObsidianLogger.create("BoxCloudProvider").apply {
    setLevel(LogLevel.DEBUG)
}

// Log all API calls
logger.d(TAG, "Uploading file: ${file.name}, size: ${file.length()}")
logger.d(TAG, "API Response: $responseBody")
logger.d(TAG, "Token valid: ${token.isExpired()}")
```

### Provider Status Page

Monitor provider status:
- **Box**: https://status.box.com/
- **Azure**: https://status.azure.com/
- **Backblaze**: https://www.backblazestatus.com/
- **Alibaba**: https://status.alibabacloud.com/
- **DigitalOcean**: https://status.digitalocean.com/
- **Oracle**: https://ocistatus.oraclecloud.com/

## Future Enhancements

### Planned Features

1. **Provider Sync**
   - Sync backups across multiple providers
   - Automatic failover if primary provider unavailable
   - Load balancing for large backups

2. **Incremental Backups**
   - Delta encoding for changed files only
   - Block-level deduplication
   - Reduced bandwidth usage

3. **Backup Verification**
   - Periodic integrity checks
   - Automated repair of corrupted backups
   - Health score per snapshot

4. **Advanced Encryption**
   - Client-side encryption before upload
   - Key management with hardware security module
   - Compliance with zero-knowledge architecture

5. **Cost Optimization**
   - Automatic tiering (hot/cool/archive)
   - Lifecycle policies
   - Cost analysis per provider

6. **Team Features**
   - Shared backups across team
   - Role-based access control
   - Audit logging

## Contributing

### Adding New Providers

To add a new cloud storage provider:

1. **Create Provider Class**
   ```kotlin
   class NewProvider(
       context: Context,
       keystoreManager: KeystoreManager,
       logger: ObsidianLogger,
       private val accountId: String = "default"
   ) : CloudProvider {
       // Implement all CloudProvider methods
   }
   ```

2. **Create OAuth2 Provider**
   ```kotlin
   class NewOAuth2Provider(
       context: Context,
       keystoreManager: KeystoreManager,
       logger: ObsidianLogger
   ) : OAuth2Provider(context, keystoreManager, logger) {
       // Set provider-specific OAuth2 configuration
   }
   ```

3. **Add to ProviderType Enum**
   ```kotlin
   enum class ProviderType {
       // ...
       NEW_PROVIDER("new", "New Provider", "Description", Icons.Default.Cloud)
   }
   ```

4. **Update AndroidManifest.xml**
   - Add intent filter for OAuth2 callback

5. **Write Tests**
   - Unit tests for OAuth2 flow
   - Integration tests for CRUD operations

6. **Document**
   - Add to this README
   - Include setup instructions
   - Document any provider-specific features

## Support

For issues or questions:
- GitHub Issues: https://github.com/yourusername/ObsidianBackup/issues
- Documentation: https://docs.obsidianbackup.com
- Email: support@obsidianbackup.com

## License

This implementation is part of ObsidianBackup and follows the project's license.

---

**Last Updated**: 2024
**Version**: 1.0.0
**Maintainers**: ObsidianBackup Team
