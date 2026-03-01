# Security Quick Reference Guide
## ObsidianBackup Developer Guide

Quick reference for implementing and maintaining security features in ObsidianBackup.

---

## 🚨 Security Checklist for New Features

Before merging any new feature:

- [ ] Input validation implemented
- [ ] SQL queries parameterized (no string concatenation)
- [ ] Sensitive data encrypted
- [ ] Biometric auth added for sensitive operations
- [ ] No hardcoded secrets
- [ ] No logging of sensitive data
- [ ] WebViews configured securely
- [ ] Exported components secured
- [ ] ProGuard rules updated (if needed)
- [ ] Security tests written

---

## 🔒 Common Security Tasks

### Storing Sensitive Data

```kotlin
// ✅ CORRECT - Use SecureStorageManager
val secureStorage = SecureStorageManager(context, logger)
secureStorage.putString("api_key", apiKey)

// ❌ WRONG - Plain SharedPreferences
sharedPrefs.edit().putString("api_key", apiKey).apply()
```

### Database Queries

```kotlin
// ✅ CORRECT - Parameterized query
@Query("SELECT * FROM snapshots WHERE id = :snapshotId")
suspend fun getSnapshotById(snapshotId: String): SnapshotEntity?

// ❌ WRONG - String concatenation
db.execSQL("SELECT * FROM snapshots WHERE id = '$snapshotId'")
```

### Input Validation

```kotlin
// ✅ CORRECT - Validate input
val dbHelper = SecureDatabaseHelper(context, secureStorage, logger)
val safeInput = dbHelper.validateAndSanitizeInput(userInput, "fieldName")

// ❌ WRONG - No validation
val result = database.query(userInput)
```

### Root Detection

```kotlin
// ✅ CORRECT - Check for root before sensitive operations
val rootDetection = RootDetectionManager(context, logger)
val result = rootDetection.detectRoot()

if (result.isRooted && result.confidence >= DetectionConfidence.HIGH) {
    showWarning("Device appears to be rooted")
    // Block operation or warn user
}
```

### Certificate Pinning

```kotlin
// ✅ CORRECT - Use pinning for all network calls
val pinningManager = CertificatePinningManager(context, logger)
val okHttpClient = pinningManager.createPinnedOkHttpClient()

// Use with Retrofit
val retrofit = Retrofit.Builder()
    .client(okHttpClient)
    .build()
```

### Biometric Authentication

```kotlin
// ✅ CORRECT - Require auth for sensitive operations
val biometricAuth = BiometricAuthManager(context)
val result = biometricAuth.authenticateForOperation(
    activity = activity,
    operation = SensitiveOperation.BACKUP
)

when (result) {
    is BiometricResult.Success -> {
        // Proceed with operation
    }
    is BiometricResult.Error -> {
        // Handle error
    }
}
```

### WebView Configuration

```kotlin
// ✅ CORRECT - Secure WebView setup
val webViewSecurity = WebViewSecurityManager(context, logger)
webViewSecurity.configureSecureWebView(
    webView = webView,
    enableJavaScript = false,
    allowedDomains = setOf("obsidianbackup.app")
)

// Load content safely
webViewSecurity.loadHtmlSafely(webView, htmlContent)
```

---

## 🚫 Common Security Mistakes

### 1. Hardcoded Secrets

```kotlin
// ❌ NEVER DO THIS
const val API_KEY = "sk_live_1234567890"
const val DATABASE_PASSWORD = "mypassword123"

// ✅ DO THIS INSTEAD
// Store in secure storage or fetch from backend
val apiKey = secureStorage.getString("api_key")
```

### 2. Logging Sensitive Data

```kotlin
// ❌ NEVER DO THIS
Log.d(TAG, "User password: $password")
Log.d(TAG, "OAuth token: $token")

// ✅ DO THIS INSTEAD
Log.d(TAG, "User authenticated successfully")
// No sensitive data in logs
```

### 3. SQL Injection

```kotlin
// ❌ NEVER DO THIS
val query = "SELECT * FROM users WHERE username = '$username'"
db.execSQL(query)

// ✅ DO THIS INSTEAD
@Query("SELECT * FROM users WHERE username = :username")
suspend fun getUserByUsername(username: String): User?
```

### 4. Ignoring SSL Errors

```kotlin
// ❌ NEVER DO THIS
override fun onReceivedSslError(
    view: WebView?,
    handler: SslErrorHandler?,
    error: SslError?
) {
    handler?.proceed() // NEVER DO THIS
}

// ✅ DO THIS INSTEAD
override fun onReceivedSslError(
    view: WebView?,
    handler: SslErrorHandler?,
    error: SslError?
) {
    handler?.cancel() // Always cancel
    showError("SSL error occurred")
}
```

### 5. Storing Credentials in Plain Text

```kotlin
// ❌ NEVER DO THIS
val file = File(context.filesDir, "credentials.txt")
file.writeText("username:password")

// ✅ DO THIS INSTEAD
secureStorage.putString("username", username)
// Don't store passwords - use tokens
```

---

## 🔐 Security Module Usage

### SecureStorageManager

```kotlin
// Initialize
val secureStorage = SecureStorageManager(context, logger)

// Store data
secureStorage.putString("key", "value")
secureStorage.putInt("count", 42)
secureStorage.putBoolean("flag", true)

// Retrieve data
val value = secureStorage.getString("key")
val count = secureStorage.getInt("count")
val flag = secureStorage.getBoolean("flag")

// Generate encryption key
val key = secureStorage.getOrCreateSecretKey(
    keyAlias = "backup_key",
    requireBiometricAuth = true
)

// Encrypt/decrypt data
val encrypted = secureStorage.encryptData(data, "backup_key")
val decrypted = secureStorage.decryptData(encrypted)
```

### SecureDatabaseHelper

```kotlin
// Initialize
val dbHelper = SecureDatabaseHelper(context, secureStorage, logger)

// Create encrypted database
val database = dbHelper.createEncryptedDatabase(
    BackupDatabase::class.java,
    "obsidian_backup.db"
)

// Validate inputs
val safePackageName = dbHelper.validatePackageName(packageName)
val safeSnapshotId = dbHelper.validateSnapshotId(snapshotId)
val safeFilePath = dbHelper.validateFilePath(filePath)
val safeUrl = dbHelper.validateUrl(url)

// Sanitize search queries
val safeQuery = dbHelper.validateSearchQuery(searchQuery)
val likePattern = dbHelper.createSafeLikePattern(userInput)

// Use safe query builder
val queryBuilder = SecureDatabaseHelper.SafeQueryBuilder()
    .addWhereClause("package_name", packageName)
    .addLikeClause("app_name", "%$searchTerm%")
val (whereClause, args) = queryBuilder.build()
```

### CertificatePinningManager

```kotlin
// Initialize
val pinningManager = CertificatePinningManager(context, logger)

// Create pinned OkHttp client
val client = pinningManager.createPinnedOkHttpClient(
    additionalPins = mapOf(
        "custom.example.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        )
    )
)

// Test pinning
val testResult = pinningManager.testPinning("googleapis.com")
if (!testResult.success) {
    Log.e(TAG, "Pinning test failed: ${testResult.message}")
}

// Calculate pin for certificate
val certificate: X509Certificate = loadCertificate()
val pin = pinningManager.calculatePublicKeyHash(certificate)
println("Pin: sha256/$pin")
```

### RootDetectionManager

```kotlin
// Initialize
val rootDetection = RootDetectionManager(context, logger)

// Full detection (async)
val result = rootDetection.detectRoot()

if (result.isRooted) {
    Log.w(TAG, "Root detected: ${result.detectionMethod}")
    Log.w(TAG, "Confidence: ${result.confidence}")
    
    when (result.confidence) {
        DetectionConfidence.CRITICAL -> {
            // Block app usage
            showErrorAndExit()
        }
        DetectionConfidence.HIGH -> {
            // Warn user and limit features
            showWarning()
        }
        else -> {
            // Log for analytics
            logRootDetection()
        }
    }
}

// Quick check (synchronous, less comprehensive)
if (rootDetection.quickRootCheck()) {
    showWarning("Device may be rooted")
}
```

### WebViewSecurityManager

```kotlin
// Initialize
val webViewSec = WebViewSecurityManager(context, logger)

// Configure WebView
webViewSec.configureSecureWebView(
    webView = webView,
    enableJavaScript = false,
    allowedDomains = setOf("obsidianbackup.app", "api.obsidianbackup.com")
)

// Sanitize HTML
val safeHtml = webViewSec.sanitizeHtml(untrustedHtml)

// Load HTML safely
webViewSec.loadHtmlSafely(webView, htmlContent)

// Validate URL
val validUrl = webViewSec.validateUrl(url)
if (validUrl != null) {
    webView.loadUrl(validUrl)
}

// Execute JavaScript safely
webViewSec.evaluateJavaScriptSafely(webView, "getVersion()") { result ->
    Log.d(TAG, "Version: $result")
}

// Add JavaScript interface (if needed)
webViewSec.addSecureJavaScriptInterface(
    webView = webView,
    obj = backupAPI,
    name = "ObsidianBackupAPI"
)
```

---

## 🧪 Testing Security Features

### Unit Tests

```kotlin
@Test
fun testInputValidation() {
    val dbHelper = SecureDatabaseHelper(context, secureStorage, logger)
    
    // Test SQL injection detection
    assertThrows<SecurityException> {
        dbHelper.validateAndSanitizeInput("'; DROP TABLE users;--")
    }
    
    // Test package name validation
    val validPackage = "com.example.app"
    assertEquals(validPackage, dbHelper.validatePackageName(validPackage))
}

@Test
fun testEncryption() {
    val secureStorage = SecureStorageManager(context, logger)
    val originalData = "sensitive data".toByteArray()
    
    val encrypted = secureStorage.encryptData(originalData, "test_key")
    val decrypted = secureStorage.decryptData(encrypted)
    
    assertArrayEquals(originalData, decrypted)
}
```

### Integration Tests

```kotlin
@Test
fun testRootDetection() = runBlocking {
    val rootDetection = RootDetectionManager(context, logger)
    val result = rootDetection.detectRoot()
    
    assertNotNull(result)
    assertTrue(result.detectionMethod.isNotEmpty())
}

@Test
fun testCertificatePinning() = runBlocking {
    val pinningManager = CertificatePinningManager(context, logger)
    val result = pinningManager.testPinning("googleapis.com")
    
    assertTrue(result.success)
}
```

---

## 📝 ProGuard Rules

When adding new security classes, update `proguard-rules.pro`:

```proguard
# Keep security classes
-keep class com.obsidianbackup.security.** { *; }

# If using reflection
-keepclassmembers class com.obsidianbackup.security.MyClass {
    public <methods>;
}
```

---

## 🔄 Certificate Pin Rotation

When rotating certificate pins:

1. **3 months before expiration:**
   - Generate new certificate
   - Calculate new pin: `sha256/[hash]`
   - Add as backup pin in `network_security_config.xml`
   - Deploy app update

2. **On expiration date:**
   - Rotate certificate on server
   - Monitor for errors

3. **1 month after rotation:**
   - Remove old pin
   - Deploy app update

---

## 🚨 Security Incident Response

If you discover a security vulnerability:

1. **DO NOT** commit the fix to public repo immediately
2. **DO** email security@obsidianbackup.com
3. **DO** document the vulnerability privately
4. **DO** follow responsible disclosure process

---

## 📚 Additional Resources

- [SECURITY_HARDENING.md](SECURITY_HARDENING.md) - Full security documentation
- [SECURITY_IMPLEMENTATION_SUMMARY.md](SECURITY_IMPLEMENTATION_SUMMARY.md) - Implementation summary
- [verify_security.sh](verify_security.sh) - Automated verification script

---

## 🆘 Getting Help

**Questions?** Ask in #security channel  
**Found a bug?** Report to security team  
**Need code review?** Tag @security-team

---

**Last Updated:** 2024-02-08  
**Version:** 1.0
