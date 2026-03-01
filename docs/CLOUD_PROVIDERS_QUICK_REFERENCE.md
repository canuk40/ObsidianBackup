# Cloud Providers Quick Reference

## 🚀 Quick Start

### 1. Add Provider
```kotlin
val provider = BoxCloudProvider(context, keystoreManager, logger, "account_id")
```

### 2. Authenticate
```kotlin
val intent = provider.oauth2Provider.getAuthorizationIntent()
startActivityForResult(intent, REQUEST_CODE)
```

### 3. Upload Backup
```kotlin
provider.uploadSnapshot(snapshotId, cloudFiles, metadata)
```

## 📋 Provider Comparison

| Feature | Box | Azure | Backblaze | Alibaba | DigitalOcean | Oracle |
|---------|-----|-------|-----------|---------|--------------|--------|
| **Max File Size** | Unlimited | 190.7 TB | 10 TB | 48.8 TB | 5 TB | 10 TB |
| **Chunk Size** | 8 MB | 4 MB | 5 MB | 5 MB | 5 MB | 10 MB |
| **Free Tier** | 10 GB | 5 GB | 10 GB | None | 250 GB | 20 GB |
| **OAuth2** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Versioning** | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **CDN** | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **Enterprise** | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ |

## 🔑 OAuth2 Credentials

### Box.com
```kotlin
clientId = "YOUR_BOX_CLIENT_ID"
clientSecret = "YOUR_BOX_CLIENT_SECRET"
redirectUri = "obsidianbackup://oauth/box"
```
Get from: https://app.box.com/developers/console

### Azure Blob Storage
```kotlin
clientId = "YOUR_AZURE_CLIENT_ID"
clientSecret = "YOUR_AZURE_CLIENT_SECRET"
tenant = "YOUR_TENANT_ID"
redirectUri = "obsidianbackup://oauth/azure"
```
Get from: https://portal.azure.com → App registrations

### Backblaze B2
Credentials are stored in `local.properties` (gitignored) and accessed via `BuildConfig`:
```properties
# local.properties
b2.keyId=YOUR_KEY_ID
b2.applicationKey=YOUR_APPLICATION_KEY
```
```kotlin
// BackblazeB2Provider uses:
BuildConfig.B2_KEY_ID
BuildConfig.B2_APPLICATION_KEY
```
Get from: https://secure.backblaze.com/b2_buckets.htm → **App Keys** → **Add a New Application Key**

> ⚠️ Key ID starts with `003...`, Application Key is a 31-character string shown only once at creation.

### Alibaba Cloud OSS
```kotlin
clientId = "YOUR_ALIBABA_CLIENT_ID"
clientSecret = "YOUR_ALIBABA_CLIENT_SECRET"
redirectUri = "obsidianbackup://oauth/alibaba"
```
Get from: https://ram.console.aliyun.com

### DigitalOcean Spaces
```kotlin
accessKey = "YOUR_ACCESS_KEY"
secretKey = "YOUR_SECRET_KEY"
region = "nyc3" // or your region
```
Get from: https://cloud.digitalocean.com/account/api/tokens

### Oracle Cloud
```kotlin
clientId = "YOUR_ORACLE_CLIENT_ID"
clientSecret = "YOUR_ORACLE_CLIENT_SECRET"
redirectUri = "obsidianbackup://oauth/oracle"
```
Get from: https://cloud.oracle.com

## 📱 AndroidManifest.xml

Add inside `<application>` tag:

```xml
<activity
    android:name=".ui.cloud.OAuth2CallbackActivity"
    android:exported="true"
    android:launchMode="singleTop">
    
    <!-- Box -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" android:host="oauth" android:pathPrefix="/box" />
    </intent-filter>
    
    <!-- Azure -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" android:host="oauth" android:pathPrefix="/azure" />
    </intent-filter>
    
    <!-- Backblaze -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" android:host="oauth" android:pathPrefix="/backblaze" />
    </intent-filter>
    
    <!-- Alibaba -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" android:host="oauth" android:pathPrefix="/alibaba" />
    </intent-filter>
    
    <!-- DigitalOcean -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" android:host="oauth" android:pathPrefix="/digitalocean" />
    </intent-filter>
    
    <!-- Oracle -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" android:host="oauth" android:pathPrefix="/oracle" />
    </intent-filter>
</activity>
```

## 🔧 Common Operations

### Test Connection
```kotlin
when (val result = provider.testConnection()) {
    is CloudResult.Success -> println("Connected! Latency: ${result.data.latencyMs}ms")
    is CloudResult.Error -> println("Failed: ${result.error.message}")
}
```

### Upload Snapshot
```kotlin
val files = listOf(
    CloudFile(localFile, "path/to/file", checksum, size)
)
val metadata = CloudSnapshotMetadata(
    snapshotId, timestamp, deviceId, appCount, 
    totalSize, compressionRatio, encrypted, merkleRoot
)
provider.uploadSnapshot(snapshotId, files, metadata)
```

### Download Snapshot
```kotlin
provider.downloadSnapshot(
    snapshotId = snapshotId,
    destinationDir = File("/path/to/restore"),
    verifyIntegrity = true
)
```

### List Snapshots
```kotlin
val filter = CloudSnapshotFilter(
    afterTimestamp = System.currentTimeMillis() - 30.days,
    deviceId = getDeviceId(),
    maxResults = 50
)
provider.listSnapshots(filter)
```

### Delete Snapshot
```kotlin
provider.deleteSnapshot(snapshotId)
```

### Get Storage Quota
```kotlin
when (val result = provider.getStorageQuota()) {
    is CloudResult.Success -> {
        val quota = result.data
        println("Used: ${quota.usedBytes} / ${quota.totalBytes}")
    }
}
```

### Observe Progress
```kotlin
provider.observeProgress().collect { progress ->
    when (progress) {
        is CloudTransferProgress.Uploading -> 
            updateUI(progress.bytesTransferred, progress.totalBytes)
        is CloudTransferProgress.Completed -> 
            showSuccess()
        is CloudTransferProgress.Failed -> 
            showError(progress.error.message)
    }
}
```

## 🔐 Multi-Account Management

### Add Account
```kotlin
val provider = BoxCloudProvider(context, keystoreManager, logger, "work")
// Authenticate...
```

### List Accounts
```kotlin
val accounts = provider.oauth2Provider.listAccounts()
```

### Switch Account
```kotlin
val provider = BoxCloudProvider(context, keystoreManager, logger, "personal")
```

### Remove Account
```kotlin
provider.oauth2Provider.removeAccount("account_id")
```

## ⚡ Performance Tips

1. **Chunk Size**: Use provider-optimized sizes
2. **Parallel Uploads**: Configure thread pool
3. **Compression**: Enable for text/code files
4. **Retry Logic**: Use exponential backoff
5. **Connection Pooling**: Reuse HTTP clients
6. **Token Caching**: Avoid unnecessary refreshes

## 🐛 Error Handling

### Error Codes
- `AUTHENTICATION_FAILED` - Re-authenticate
- `NETWORK_ERROR` - Retry
- `QUOTA_EXCEEDED` - Free space or upgrade
- `FILE_NOT_FOUND` - Check snapshot ID
- `CHECKSUM_MISMATCH` - Re-upload
- `TIMEOUT` - Increase timeout or check network
- `PERMISSION_DENIED` - Check OAuth2 scopes

### Retry Example
```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    block: suspend () -> CloudResult<T>
): CloudResult<T> {
    repeat(maxAttempts - 1) {
        when (val result = block()) {
            is CloudResult.Success -> return result
            is CloudResult.Error -> {
                if (!result.error.retryable) return result
                delay((1000L * (it + 1)))
            }
        }
    }
    return block()
}
```

## 📦 Build & Deploy

### Debug Build
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

### Run Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 🆘 Troubleshooting

### OAuth2 Fails
- Check client ID/secret
- Verify redirect URI matches
- Ensure app is approved in provider console
- Check device time synchronization

### Upload Timeout
- Increase timeout values
- Check network stability
- Reduce chunk size
- Enable retry logic

### Token Refresh Fails
- Re-authenticate to get new refresh token
- Check token expiration policy
- Verify refresh token scope

### Quota Exceeded
- Delete old snapshots
- Check quota with `getStorageQuota()`
- Upgrade storage plan

## 📚 Documentation

- **Full Guide**: `ADDITIONAL_CLOUD_PROVIDERS.md`
- **Implementation Summary**: `IMPLEMENTATION_SUMMARY.md`
- **Verification Script**: `verify_cloud_providers.sh`

## 📞 Support

- GitHub Issues: https://github.com/yourusername/ObsidianBackup/issues
- Email: support@obsidianbackup.com

---

**Last Updated**: 2024
**Quick Reference Version**: 1.0
