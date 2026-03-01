# Security Hardening Documentation
## ObsidianBackup - OWASP Mobile Top 10 Compliance

**Version:** 1.0  
**Last Updated:** February 20, 2026  
**Compliance Target:** OWASP Mobile Top 10 2024 & MASVS v2.0

> **2026-02-20 Update:** SafetyNet references in this document have been updated to reflect the Play Integrity API migration (SafetyNet was shut down May 2025). SQLCipher updated to `sqlcipher-android:4.6.1`. Health data encryption and audit logging implemented. See `FIXES_APPLIED.md` for full changelog.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [OWASP Mobile Top 10 Compliance](#owasp-mobile-top-10-compliance)
3. [Security Architecture](#security-architecture)
4. [Implementation Details](#implementation-details)
5. [Security Testing](#security-testing)
6. [Penetration Testing Checklist](#penetration-testing-checklist)
7. [Incident Response](#incident-response)
8. [Maintenance & Updates](#maintenance--updates)

---

## Executive Summary

ObsidianBackup implements comprehensive security hardening aligned with OWASP Mobile Application Security Verification Standard (MASVS) v2.0 and addresses all OWASP Mobile Top 10 risks.

### Security Posture

- **Encryption**: AES-256-GCM for data at rest, TLS 1.3 for data in transit
- **Authentication**: Biometric authentication with StrongBox hardware-backed keys
- **Root Detection**: Multi-layered detection using Play Integrity API
- **Network Security**: Certificate pinning, HTTPS-only, network security config
- **Code Protection**: R8 obfuscation, anti-tampering, secure coding practices
- **Data Protection**: Encrypted database (SQLCipher), secure storage (Android Keystore)

---

## OWASP Mobile Top 10 Compliance

### M1: Improper Platform Usage

**Risk:** Misuse of platform features or failure to use platform security controls.

**Mitigation Implemented:**

✅ **Android Keystore Integration**
- Hardware-backed encryption keys
- StrongBox support for Pixel 3+ and compatible devices
- Biometric-protected keys with user authentication requirement

✅ **Proper Permissions Handling**
- Runtime permission requests with user education
- Minimal permission principle
- Scoped storage for Android 10+

✅ **Secure Storage APIs**
- `EncryptedSharedPreferences` for sensitive data
- Android Keystore for cryptographic keys
- No sensitive data in plain text

**Implementation:**
```kotlin
// SecureStorageManager.kt
- Hardware-backed key generation
- StrongBox-backed master key
- Encrypted SharedPreferences
```

---

### M2: Insecure Data Storage

**Risk:** Sensitive data stored insecurely on device.

**Mitigation Implemented:**

✅ **Database Encryption**
- SQLCipher for Room database encryption
- AES-256 encryption
- Secure passphrase generation and storage

✅ **Encrypted SharedPreferences**
- `androidx.security:security-crypto` library
- AES256-GCM encryption scheme
- Master key in Android Keystore

✅ **Secure File Storage**
- App-private storage directories only
- File encryption for backup archives
- Secure deletion of temporary files

✅ **No Sensitive Data in Logs**
- Production logging disabled
- ProGuard strips debug code
- No PII or credentials in logs

**Implementation:**
```kotlin
// SecureDatabaseHelper.kt
- SQLCipher integration
- Encrypted database creation
- Secure passphrase management

// SecureStorageManager.kt
- Encrypted SharedPreferences
- Android Keystore integration
```

**Data Classification:**

| Data Type | Classification | Storage Method |
|-----------|---------------|----------------|
| Backup encryption keys | Critical | Android Keystore |
| OAuth tokens | Critical | Encrypted SharedPreferences |
| Database passphrase | Critical | Android Keystore |
| Backup metadata | Sensitive | Encrypted SQLCipher database |
| User preferences | Sensitive | Encrypted SharedPreferences |
| Backup archives | Sensitive | Encrypted files (AES-256-GCM) |

---

### M3: Insecure Communication

**Risk:** Data intercepted or tampered with during transmission.

**Mitigation Implemented:**

✅ **Certificate Pinning**
- Public key pinning for API endpoints
- Multiple backup pins for certificate rotation
- Pin expiration dates

✅ **Network Security Configuration**
- HTTPS-only communication
- Cleartext traffic disabled
- Certificate transparency checking

✅ **TLS Configuration**
- TLS 1.2 minimum (TLS 1.3 preferred)
- Strong cipher suites only
- No deprecated protocols (SSLv3, TLS 1.0, TLS 1.1)

✅ **Hostname Verification**
- Strict hostname verification
- No custom trust managers
- SSL error handling (never ignore)

**Implementation:**
```kotlin
// CertificatePinningManager.kt
- OkHttp certificate pinning
- Custom SSLContext configuration
- Pin validation and rotation

// network_security_config.xml
- Domain-specific pin sets
- Base configuration (HTTPS-only)
- Debug overrides for development
```

**Pinned Domains:**
- `googleapis.com` (Google Drive API)
- `api.obsidianbackup.com` (Backend API)
- Custom WebDAV endpoints (configurable)

---

### M4: Insecure Authentication

**Risk:** Weak or improperly implemented authentication.

**Mitigation Implemented:**

✅ **Biometric Authentication**
- BiometricPrompt API integration
- StrongBox-backed authentication
- Fallback to device credentials (PIN/Pattern/Password)

✅ **Crypto-based Authentication**
- Keys protected by biometric authentication
- 30-second authentication validity window
- Key invalidation on biometric changes

✅ **OAuth 2.0 for Cloud Services**
- Secure token storage
- Token refresh mechanism
- No credentials stored locally

✅ **Passkey Support (Android 14+)**
- WebAuthn/FIDO2 credentials
- Cloud-synced passkeys
- Phishing-resistant authentication

**Implementation:**
```kotlin
// BiometricAuthManager.kt
- Comprehensive biometric authentication
- StrongBox KeyMint integration
- Crypto-based key unlock

// PasskeyManager.kt
- Passkey creation and authentication
- Credential Manager API
```

**Authentication Flow:**
1. User triggers sensitive operation (backup, restore, settings)
2. BiometricPrompt displayed
3. User authenticates with biometric or device credential
4. Cipher unlocked for cryptographic operations
5. Operation proceeds with authenticated context

---

### M5: Insufficient Cryptography

**Risk:** Weak or broken cryptographic algorithms.

**Mitigation Implemented:**

✅ **Strong Encryption Algorithms**
- AES-256-GCM for symmetric encryption
- RSA-4096 for asymmetric encryption (where used)
- SHA-256 for hashing
- PBKDF2 for key derivation (if applicable)

✅ **Proper Key Management**
- Android Keystore for key storage
- Hardware-backed keys (StrongBox)
- No hardcoded keys
- Secure key generation with SecureRandom

✅ **Proper IV/Nonce Handling**
- Unique IV for each encryption operation
- Cryptographically secure random generation
- IV stored with ciphertext

✅ **No Custom Crypto**
- Standard Java Cryptography Architecture (JCA)
- Standard Android Security libraries
- No homebrew crypto implementations

**Implementation:**
```kotlin
// SecureStorageManager.kt
- AES-256-GCM encryption
- GCM authentication tag (128-bit)
- Unique IV per encryption

// Encryption Constants:
KEY_SIZE = 256
GCM_TAG_LENGTH = 128
TRANSFORMATION = "AES/GCM/NoPadding"
```

**Cryptographic Standards:**
- ✅ FIPS 140-2 compliant algorithms
- ✅ NIST recommended key sizes
- ✅ Authenticated encryption (AEAD)
- ❌ NO deprecated algorithms (MD5, SHA-1, DES, 3DES)

---

### M6: Insecure Authorization

**Risk:** Improper access control and privilege escalation.

**Mitigation Implemented:**

✅ **Principle of Least Privilege**
- Minimal permissions requested
- Runtime permission requests
- Permission usage explanation

✅ **Input Validation**
- All user inputs validated and sanitized
- SQL injection prevention (parameterized queries)
- Path traversal prevention
- Package name validation

✅ **Authorization Checks**
- Biometric authentication for sensitive operations
- Re-authentication after timeout
- No unauthorized access to backup data

✅ **Secure IPC**
- Exported components properly secured
- Intent validation
- Deep link validation

**Implementation:**
```kotlin
// SecureDatabaseHelper.kt
- Input validation and sanitization
- SQL injection prevention
- Path traversal checks
- Package name validation
- URL validation
```

---

### M7: Poor Code Quality

**Risk:** Security vulnerabilities due to coding mistakes.

**Mitigation Implemented:**

✅ **Code Obfuscation**
- R8/ProGuard enabled for release builds
- Aggressive optimization
- Package name flattening
- String encryption

✅ **Buffer Overflow Prevention**
- Kotlin null safety
- Bounds checking
- Safe array operations

✅ **Memory Management**
- Proper resource cleanup
- No memory leaks (LeakCanary in debug)
- Sensitive data cleared from memory

✅ **Error Handling**
- Comprehensive exception handling
- No sensitive data in error messages
- Secure failure modes

✅ **Secure Coding Practices**
- No hardcoded secrets
- No debug code in production
- Logging disabled in release builds

**Implementation:**
```kotlin
// proguard-rules.pro
- Comprehensive obfuscation rules
- Logging removal in release
- Code shrinking and optimization

// Security coding guidelines:
- No hardcoded credentials
- Sensitive data cleared after use
- Exception handling without leaking data
```

---

### M8: Code Tampering

**Risk:** Modification of application code or resources.

**Mitigation Implemented:**

✅ **Code Obfuscation**
- R8 optimization and obfuscation
- Class and method name obfuscation
- String encryption
- Control flow obfuscation

✅ **Root Detection**
- Multi-layered root detection
- Google Play Integrity API
- Su binary detection
- Root management app detection
- Build tags inspection
- Dangerous properties checking

✅ **Integrity Checks**
- Certificate validation
- APK signature verification
- Play Integrity MEETS_DEVICE_INTEGRITY verdict

✅ **Anti-Debugging**
- Debug detection at runtime
- Debuggable flag checking
- Debug build type exclusion in release

**Implementation:**
```kotlin
// RootDetectionManager.kt
- Play Integrity API
- Multiple detection methods
- Confidence scoring system

// Root Detection Methods:
1. Play Integrity API (primary)
2. Root management apps
3. Su binary detection
4. Build tags inspection
5. Dangerous properties
6. System writable check
7. Busybox detection
```

**Root Detection Confidence Levels:**
- **LOW**: Basic checks failed
- **MEDIUM**: Multiple checks failed
- **HIGH**: Play Integrity or critical checks failed
- **CRITICAL**: Multiple high-confidence indicators

---

### M9: Insecure Data Leakage

**Risk:** Unintended data exposure through app features.

**Mitigation Implemented:**

✅ **Secure Logging**
- Production logging disabled
- Debug logs stripped by ProGuard
- No PII in logs

✅ **Clipboard Security**
- Sensitive data not copied to clipboard
- Clipboard cleared after paste

✅ **Screenshot Prevention**
- FLAG_SECURE for sensitive screens
- Screenshots disabled for authentication flows

✅ **Keyboard Caching**
- No sensitive data in input fields
- Autocomplete disabled for passwords

✅ **Background Screenshots**
- Sensitive data hidden when app backgrounded
- Task switcher preview secured

**Implementation:**
```kotlin
// Secure Activity Configuration:
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)

// Logging Stripping (ProGuard):
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
```

---

### M10: Insufficient Cryptography

**Risk:** Improper use of WebViews leading to XSS and other attacks.

**Mitigation Implemented:**

✅ **WebView Security Configuration**
- JavaScript disabled by default
- File access disabled
- Mixed content blocked (HTTPS-only)
- Safe browsing enabled

✅ **XSS Prevention**
- HTML sanitization
- Script tag removal
- Event handler stripping
- JavaScript protocol blocking
- Content Security Policy injection

✅ **JavaScript Interface Security**
- Interface whitelist
- @JavascriptInterface annotation required
- Input validation for JS calls

✅ **URL Validation**
- Scheme whitelist (https, file, data)
- Domain whitelist
- URL sanitization

**Implementation:**
```kotlin
// WebViewSecurityManager.kt
- Secure WebView configuration
- HTML sanitization
- CSP injection
- JavaScript interface security
- URL validation

// WebView Security Settings:
- javaScriptEnabled = false (by default)
- allowFileAccess = false
- allowContentAccess = false
- mixedContentMode = MIXED_CONTENT_NEVER_ALLOW
- safeBrowsingEnabled = true
```

**Content Security Policy:**
```
default-src 'self';
script-src 'self' 'unsafe-inline';
style-src 'self' 'unsafe-inline';
img-src 'self' data: https:;
connect-src 'self' https:;
frame-src 'none';
object-src 'none';
```

---

## Security Architecture

### Defense in Depth

ObsidianBackup implements multiple layers of security:

```
┌─────────────────────────────────────────────────────┐
│               Application Layer                      │
│  • Input Validation • Authorization Checks           │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│              Security Layer                          │
│  • Biometric Auth • Root Detection • WebView Sec    │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│            Cryptography Layer                        │
│  • AES-256-GCM • Android Keystore • StrongBox       │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│              Network Layer                           │
│  • Certificate Pinning • TLS 1.3 • HTTPS-only       │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│            Platform Layer                            │
│  • Android Security • SELinux • Hardware TEE         │
└─────────────────────────────────────────────────────┘
```

### Key Security Components

| Component | Purpose | Technology |
|-----------|---------|------------|
| BiometricAuthManager | User authentication | BiometricPrompt, StrongBox |
| SecureStorageManager | Secure data storage | Android Keystore, EncryptedSharedPrefs |
| CertificatePinningManager | Network security | OkHttp, Certificate Pinning |
| RootDetectionManager | Integrity verification | Play Integrity API |
| SecureDatabaseHelper | SQL injection prevention | Parameterized queries, input validation |
| WebViewSecurityManager | XSS prevention | HTML sanitization, CSP |

---

## Implementation Details

### Certificate Pinning

**Pins Configuration:**

```xml
<!-- network_security_config.xml -->
<domain-config>
    <domain includeSubdomains="true">googleapis.com</domain>
    <pin-set expiration="2026-12-31">
        <pin digest="SHA-256">47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=</pin>
        <pin digest="SHA-256">8Rw90Ej3Ttt8RRkrg+WYDS9n7IS03bk5bjP/UXPtaY8=</pin>
    </pin-set>
</domain-config>
```

**Pin Rotation Process:**

1. Generate new certificate on server
2. Calculate SHA-256 hash of public key
3. Add new pin as backup pin (3 months before expiration)
4. Deploy app update with new pin
5. After majority of users update, rotate certificate
6. Remove old pin in next app update

**Extracting Pins (Development Only):**

```kotlin
// Use CertificatePinHelper for development
val pins = CertificatePinHelper.extractPinsFromUrl("https://example.com")
pins.forEach { println(it) }
```

### Root Detection

**Detection Methods Priority:**

1. **Play Integrity API** (Highest confidence)
   - Google Play Services integrity check
   - CTS profile match
   - Basic integrity verification

2. **Root Management Apps** (High confidence)
   - Magisk, SuperSU, KingRoot, etc.
   - Package name detection

3. **Su Binary Detection** (High confidence)
   - Common su binary paths
   - Executable permission check

4. **Build Tags** (Medium confidence)
   - test-keys in build tags
   - Indicates unofficial ROM

5. **Dangerous Properties** (Medium confidence)
   - ro.debuggable=1
   - ro.secure=0

6. **System Writable** (Medium confidence)
   - /system partition write test

7. **Busybox Detection** (Low confidence)
   - Busybox binary presence

**Usage:**

```kotlin
val rootDetection = RootDetectionManager(context, logger)
val result = rootDetection.detectRoot()

when (result.confidence) {
    DetectionConfidence.CRITICAL -> {
        // Block sensitive operations
        showSecurityWarning()
    }
    DetectionConfidence.HIGH -> {
        // Warn user
        logSecurityEvent()
    }
    else -> {
        // Proceed normally
    }
}
```

### Encrypted Database

**Setup:**

```kotlin
val secureDbHelper = SecureDatabaseHelper(context, secureStorage, logger)
val database = secureDbHelper.createEncryptedDatabase(
    BackupDatabase::class.java,
    "obsidian_backup.db"
)
```

**Benefits:**

- AES-256 encryption via SQLCipher
- Passphrase stored securely in Android Keystore
- Transparent encryption (no code changes required)
- Performance impact: <5% overhead

### Secure Storage

**Storing Sensitive Data:**

```kotlin
val secureStorage = SecureStorageManager(context, logger)

// Store OAuth token
secureStorage.putString("oauth_token", token)

// Retrieve OAuth token
val token = secureStorage.getString("oauth_token")

// Store with biometric protection
val key = secureStorage.getOrCreateSecretKey(
    "backup_encryption_key",
    requireBiometricAuth = true
)
```

### WebView Security

**Secure WebView Setup:**

```kotlin
val webViewSecurity = WebViewSecurityManager(context, logger)

// Configure WebView
webViewSecurity.configureSecureWebView(
    webView = webView,
    enableJavaScript = false,
    allowedDomains = setOf("obsidianbackup.app")
)

// Load HTML safely
webViewSecurity.loadHtmlSafely(webView, htmlContent)

// Add JavaScript interface (if needed)
webViewSecurity.addSecureJavaScriptInterface(
    webView = webView,
    obj = backupAPI,
    name = "ObsidianBackupAPI"
)
```

---

## Security Testing

### Static Analysis

**Tools:**
- ✅ Android Lint
- ✅ Detekt (Kotlin static analyzer)
- ✅ QARK (Quick Android Review Kit)
- ✅ MobSF (Mobile Security Framework)

**Run Analysis:**

```bash
# Lint
./gradlew lintRelease

# Detekt
./gradlew detekt

# MobSF (upload APK to MobSF instance)
```

### Dynamic Analysis

**Tools:**
- ✅ Frida (dynamic instrumentation)
- ✅ Objection (runtime mobile exploration)
- ✅ Drozer (security assessment framework)

**Test Scenarios:**

1. Certificate pinning bypass attempts
2. Root detection bypass attempts
3. SSL/TLS interception
4. Memory dumping
5. Runtime manipulation

### Penetration Testing

**Manual Testing:**

1. Network traffic interception (Burp Suite)
2. Certificate pinning validation
3. Root detection validation
4. SQL injection attempts
5. XSS injection attempts
6. Authentication bypass attempts
7. Authorization bypass attempts
8. Data storage inspection
9. Backup file encryption validation
10. IPC security validation

---

## Penetration Testing Checklist

### Network Security

- [ ] Certificate pinning cannot be bypassed
- [ ] TLS 1.2+ enforced (no SSLv3, TLS 1.0, TLS 1.1)
- [ ] Strong cipher suites only
- [ ] No cleartext traffic
- [ ] SSL errors properly handled (not ignored)
- [ ] Hostname verification enforced
- [ ] No mixed content (HTTP + HTTPS)
- [ ] API endpoints require authentication
- [ ] OAuth tokens properly stored and transmitted

### Data Storage Security

- [ ] No sensitive data in plain text
- [ ] Database encrypted (SQLCipher)
- [ ] SharedPreferences encrypted
- [ ] Keys stored in Android Keystore
- [ ] No hardcoded secrets in code
- [ ] No sensitive data in logs
- [ ] Backup files encrypted
- [ ] Temporary files properly deleted
- [ ] No sensitive data in external storage
- [ ] App private storage used

### Authentication & Authorization

- [ ] Biometric authentication required for sensitive operations
- [ ] Authentication timeout enforced (30 seconds)
- [ ] Device credential fallback available
- [ ] No authentication bypass possible
- [ ] Authorization checks performed server-side
- [ ] Session tokens properly managed
- [ ] Re-authentication required after timeout
- [ ] Passkey support (Android 14+)

### Input Validation

- [ ] All inputs validated and sanitized
- [ ] SQL injection attempts blocked
- [ ] XSS attempts blocked
- [ ] Path traversal attempts blocked
- [ ] URL validation enforced
- [ ] Package name validation enforced
- [ ] File path validation enforced
- [ ] Deep link validation enforced

### Code Security

- [ ] Code obfuscated (R8/ProGuard)
- [ ] Debug code stripped in release
- [ ] Logging disabled in release
- [ ] No hardcoded credentials
- [ ] No sensitive data in memory dumps
- [ ] Anti-debugging measures active
- [ ] Root detection active
- [ ] Tampering detection active

### WebView Security

- [ ] JavaScript disabled by default
- [ ] File access disabled
- [ ] Mixed content blocked
- [ ] Safe browsing enabled
- [ ] HTML sanitization performed
- [ ] CSP injected
- [ ] JavaScript interfaces whitelisted
- [ ] URL validation enforced

### Cryptography

- [ ] AES-256-GCM used for encryption
- [ ] Keys generated securely (SecureRandom)
- [ ] Keys stored in Android Keystore
- [ ] Unique IV per encryption operation
- [ ] Authenticated encryption used (AEAD)
- [ ] No deprecated algorithms (MD5, SHA-1, DES)
- [ ] Hardware-backed keys (StrongBox if available)

### Platform Security

- [ ] Minimum SDK properly set (API 26+)
- [ ] Target SDK current (API 35)
- [ ] Permissions minimized
- [ ] Exported components secured
- [ ] Intent validation performed
- [ ] Deep links validated
- [ ] Content providers secured
- [ ] Broadcast receivers secured

---

## Incident Response

### Security Incident Procedure

1. **Detection**
   - Root detection triggered
   - Play Integrity check failed
   - Certificate pinning violation
   - Unusual API activity

2. **Analysis**
   - Review logs (if available)
   - Identify attack vector
   - Assess impact

3. **Containment**
   - Block compromised devices
   - Revoke OAuth tokens
   - Rotate certificates (if necessary)

4. **Remediation**
   - Patch vulnerabilities
   - Update security measures
   - Deploy app update

5. **Communication**
   - Notify affected users (if necessary)
   - Document incident
   - Update security documentation

### Contact Information

**Security Team:** security@obsidianbackup.com  
**Bug Bounty Program:** (if applicable)

---

## Maintenance & Updates

### Security Update Schedule

- **Monthly**: Dependency updates
- **Quarterly**: Security audit
- **Annually**: Penetration testing
- **As needed**: Critical security patches

### Certificate Pin Rotation

Certificate pins should be rotated before expiration:

1. **T-90 days**: Add new backup pin
2. **T-30 days**: Deploy app update with backup pin
3. **T-0 days**: Rotate certificate on server
4. **T+30 days**: Remove old pin in next app update

### Security Dependencies

Keep the following dependencies up to date:

- `androidx.security:security-crypto`
- `com.google.android.play:integrity:1.4.0`
- `net.zetetic:android-database-sqlcipher`
- `com.squareup.okhttp3:okhttp`
- `androidx.biometric:biometric`

### Security Monitoring

Monitor the following:

- Google Play Protect alerts
- Play Integrity API updates
- OWASP Mobile Top 10 updates
- Android security bulletins
- CVE databases for dependencies

---

## References

- [OWASP Mobile Top 10 2024](https://owasp.org/www-project-mobile-top-10/)
- [OWASP MASVS](https://github.com/OWASP/owasp-masvs)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Android App Security Checklist](https://developer.android.com/training/articles/security-checklist)
- [Play Integrity API](https://developer.android.com/training/safetynet/attestation)

---

## Appendix A: Security Configuration Files

### Network Security Config

Location: `app/src/main/res/xml/network_security_config.xml`

### ProGuard Rules

Location: `app/proguard-rules.pro`

### AndroidManifest Security Attributes

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false"
    ...>
```

---

## Appendix B: Security Code Locations

| Component | File Location |
|-----------|---------------|
| Root Detection | `app/src/main/java/com/obsidianbackup/security/RootDetectionManager.kt` |
| Certificate Pinning | `app/src/main/java/com/obsidianbackup/security/CertificatePinningManager.kt` |
| Secure Storage | `app/src/main/java/com/obsidianbackup/security/SecureStorageManager.kt` |
| Secure Database | `app/src/main/java/com/obsidianbackup/security/SecureDatabaseHelper.kt` |
| WebView Security | `app/src/main/java/com/obsidianbackup/security/WebViewSecurityManager.kt` |
| Biometric Auth | `app/src/main/java/com/obsidianbackup/security/BiometricAuthManager.kt` |

---

**Document Classification:** Internal Use  
**Next Review Date:** 2025-01-01
