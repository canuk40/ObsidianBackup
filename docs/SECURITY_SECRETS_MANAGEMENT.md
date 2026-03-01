# Security & Secrets Management

This document describes how ObsidianBackup manages sensitive data, API keys, and certificates to maintain security best practices.

## Table of Contents

1. [Overview](#overview)
2. [API Keys & Secrets](#api-keys--secrets)
3. [Certificate Pinning](#certificate-pinning)
4. [Production Deployment](#production-deployment)
5. [Development Setup](#development-setup)
6. [Security Best Practices](#security-best-practices)

---

## Overview

**Core Principle**: Never commit secrets to source control.

All sensitive data (API keys, certificates, secrets) are:
- Externalized from code
- Loaded from secure configuration files
- Ignored by version control (.gitignore)
- Documented for proper setup

---

## API Keys & Secrets

### SafetyNet API Key

**Location**: `local.properties` (NOT committed to Git)

**Setup**:

1. Create/edit `local.properties` in project root:
   ```properties
   # Google SafetyNet API Key
   # Get from: https://console.cloud.google.com/apis/credentials
   safetynet.api.key=YOUR_ACTUAL_SAFETYNET_API_KEY_HERE
   ```

2. The key is automatically injected into `BuildConfig` during build:
   ```kotlin
   // Access in code
   val apiKey = BuildConfig.SAFETYNET_API_KEY
   ```

3. **NEVER** commit `local.properties` to Git (already in .gitignore)

**How it Works**:

- `app/build.gradle.kts` defines BuildConfig field:
  ```kotlin
  buildConfigField("String", "SAFETYNET_API_KEY", 
                   "\"${project.findProperty("safetynet.api.key") ?: ""}\"")
  ```
- Key is read from `local.properties` at build time
- Empty string used if key not found (dev builds)

**Getting a SafetyNet API Key**:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create/select a project
3. Enable "Android Device Verification API"
4. Create credentials (API Key)
5. Restrict key to Android apps (add package name + SHA-1)
6. Add key to `local.properties`

---

## Certificate Pinning

Certificate pinning prevents man-in-the-middle attacks by validating server certificates against known public keys.

### Current Pins (Google APIs)

**googleapis.com** pins (updated 2024):
```
Primary:      dMKp+EmLpCLB/WxGhCnWWRcS+FG6u6XdQAQjcaaK8qE=  (*.googleapis.com leaf)
Intermediate: YPtHaftLw6/0vnc2BnNKGF54xiCA28WFcccjkA4ypCM=  (WR2 intermediate)
Root/Backup:  hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=  (GTS Root R1)
```

**Locations**:
1. `app/src/main/res/xml/network_security_config.xml` - Android system-level
2. `app/src/main/java/com/obsidianbackup/security/CertificatePinningManager.kt` - OkHttp client

### Generating Certificate Pins

**For any domain** (example: your-api.com):

```bash
# Get certificate pin
openssl s_client -connect your-api.com:443 -servername your-api.com < /dev/null 2>/dev/null | \
openssl x509 -pubkey -noout | \
openssl pkey -pubin -outform der | \
openssl dgst -sha256 -binary | \
openssl enc -base64
```

**Output**: `dMKp+EmLpCLB/WxGhCnWWRcS+FG6u6XdQAQjcaaK8qE=`

**Get full certificate chain pins**:

```bash
# Save certificate chain
echo | openssl s_client -connect your-api.com:443 -showcerts 2>/dev/null > chain.pem

# Extract each certificate
csplit -f cert- chain.pem '/-----BEGIN CERTIFICATE-----/' '{*}'

# Generate pin for each certificate
for cert in cert-*; do
  if [ -s "$cert" ]; then
    openssl x509 -in "$cert" -pubkey -noout 2>/dev/null | \
    openssl pkey -pubin -outform der 2>/dev/null | \
    openssl dgst -sha256 -binary | \
    openssl enc -base64
  fi
done
```

### Adding Custom API Pins

**1. Update network_security_config.xml**:

```xml
<domain-config>
    <domain includeSubdomains="true">api.yourdomain.com</domain>
    <pin-set expiration="2026-12-31">
        <pin digest="SHA-256">YOUR_PRIMARY_PIN_HERE</pin>
        <pin digest="SHA-256">YOUR_BACKUP_PIN_HERE</pin>
    </pin-set>
</domain-config>
```

**2. Update CertificatePinningManager.kt**:

```kotlin
private val API_PINS = mapOf(
    "api.yourdomain.com" to listOf(
        "sha256/YOUR_PRIMARY_PIN_HERE",
        "sha256/YOUR_BACKUP_PIN_HERE"
    )
)
```

### Pin Rotation Process

**When to rotate**:
- Certificate expires (check `expiration` in XML)
- CA rotates intermediate/root certificates
- Security incident

**How to rotate**:

1. Generate new pins (see above)
2. Add NEW pins alongside OLD pins (both active)
3. Deploy app update
4. Wait 90+ days for user adoption
5. Remove OLD pins from code
6. Deploy another update

**Example** (during rotation):
```xml
<pin-set expiration="2026-12-31">
    <!-- Old pins (will be removed after 90 days) -->
    <pin digest="SHA-256">OLD_PRIMARY_PIN</pin>
    <pin digest="SHA-256">OLD_BACKUP_PIN</pin>
    
    <!-- New pins (active now) -->
    <pin digest="SHA-256">NEW_PRIMARY_PIN</pin>
    <pin digest="SHA-256">NEW_BACKUP_PIN</pin>
</pin-set>
```

---

## Production Deployment

### CI/CD Setup (GitHub Actions, Jenkins, etc.)

**Set secrets as environment variables**:

```yaml
# GitHub Actions example
env:
  SAFETYNET_API_KEY: ${{ secrets.SAFETYNET_API_KEY }}

# Before build, create local.properties
- name: Setup secrets
  run: |
    echo "safetynet.api.key=${{ secrets.SAFETYNET_API_KEY }}" > local.properties
```

**OR inject via gradle.properties**:

```bash
# In CI/CD
export ORG_GRADLE_PROJECT_safetynet.api.key=$SAFETYNET_API_KEY
./gradlew assembleRelease
```

### Backend-Driven Key Management (Recommended for Production)

Instead of embedding keys at build time, fetch from backend:

```kotlin
class SecureKeyManager @Inject constructor(
    private val apiClient: ApiClient,
    private val secureStorage: SecureStorageManager
) {
    suspend fun getSafetyNetApiKey(): String {
        // Check cache
        val cached = secureStorage.get("safetynet_key")
        if (cached != null) return cached
        
        // Fetch from backend
        val key = apiClient.fetchApiKey("safetynet")
        
        // Cache securely
        secureStorage.put("safetynet_key", key)
        return key
    }
}
```

---

## Development Setup

### Quick Start

1. **Clone repository**
   ```bash
   git clone https://github.com/yourusername/ObsidianBackup.git
   cd ObsidianBackup
   ```

2. **Create local.properties** (copy template)
   ```bash
   cp local.properties.template local.properties
   ```

3. **Add your API keys** (edit local.properties)
   ```properties
   safetynet.api.key=YOUR_KEY_HERE
   ```

4. **Build**
   ```bash
   ./gradlew assembleDebug
   ```

### Local Configuration Files

**local.properties** (Git ignored):
```properties
# Google SafetyNet API Key
safetynet.api.key=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXX

# Add other secrets as needed
# firebase.api.key=...
# stripe.publishable.key=...
```

**Verify .gitignore**:
```bash
# Check that local.properties is ignored
git check-ignore local.properties
# Should output: local.properties
```

---

## Security Best Practices

### ✅ DO

- **Externalize secrets** - Use BuildConfig, backend APIs, or Android Keystore
- **Use .gitignore** - Never commit secrets to Git
- **Pin certificates** - Enable certificate pinning for all production APIs
- **Rotate regularly** - Update pins before expiration
- **Use multi-layer security**:
  - Certificate pinning (transport layer)
  - API key authentication (application layer)
  - End-to-end encryption (data layer)
- **Test in debug builds** - Use debug overrides for testing
- **Document everything** - Keep this file updated

### ❌ DON'T

- **Never hardcode secrets** - No API keys in source code
- **Never commit secrets** - Check Git history if you accidentally commit
- **Don't use production keys in debug** - Use separate debug API keys
- **Don't ignore pin expiration** - Monitor certificate expiration dates
- **Don't remove old pins immediately** - Wait 90+ days after rotation

### Threat Model

**Protections**:
- ✅ Man-in-the-middle attacks → Certificate pinning
- ✅ Hardcoded secrets → BuildConfig externalization
- ✅ Root detection bypass → SafetyNet attestation
- ✅ Network traffic inspection → TLS 1.2+, pinning
- ✅ API abuse → Key rotation, rate limiting

**Limitations**:
- ❌ Rooted device attacks → Detect but can't prevent
- ❌ Repackaged APKs → Code obfuscation helps but not foolproof
- ❌ Runtime memory inspection → Use SecureMemory utilities

---

## Troubleshooting

### Build Error: "SAFETYNET_API_KEY not found"

**Cause**: `local.properties` missing or key not set

**Fix**:
```bash
echo "safetynet.api.key=YOUR_KEY" >> local.properties
./gradlew clean assembleDebug
```

### Certificate Pinning Failure in Production

**Symptoms**: `SSLPeerUnverifiedException` in logs

**Causes**:
1. Certificate rotated (pins outdated)
2. Wrong pins configured
3. Network intercepting traffic (corporate proxy, VPN)

**Debug**:
```kotlin
// Enable logging in CertificatePinningManager
val testResult = certificatePinningManager.testPinning("googleapis.com")
logger.d("Pin test: $testResult")
```

**Fix**:
1. Regenerate pins (see "Generating Certificate Pins")
2. Update XML and Kotlin code
3. Test with `testPinning()` method
4. Deploy app update

### SafetyNet Attestation Fails

**Common issues**:
- Invalid API key → Check Google Cloud Console
- Key restrictions too strict → Add package name + SHA-1
- Rooted device → Expected behavior (detected correctly)
- API not enabled → Enable "Android Device Verification API"

---

## Tools & Scripts

### Certificate Pin Checker

```bash
# Check current pins for a domain
./scripts/check-pins.sh googleapis.com
```

### Pin Update Script

```bash
# Update pins for Google APIs
./scripts/update-google-pins.sh
```

### Local Properties Template Generator

```bash
# Generate template with instructions
./scripts/generate-local-properties.sh
```

---

## References

- [OWASP MASVS](https://github.com/OWASP/owasp-masvs) - Mobile Application Security Verification Standard
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)
- [Certificate Pinning Guide](https://owasp.org/www-community/controls/Certificate_and_Public_Key_Pinning)
- [SafetyNet API Docs](https://developer.android.com/training/safetynet/attestation)

---

## Contact

For security concerns or questions:
- File an issue: https://github.com/yourusername/ObsidianBackup/issues
- Email: security@obsidianbackup.com
- Security disclosure: SECURITY.md

---

**Last Updated**: December 2024  
**Pin Update Schedule**: Verify Google API pins quarterly
