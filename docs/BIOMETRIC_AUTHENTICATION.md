# Biometric Authentication Implementation

## Overview

ObsidianBackup now supports biometric authentication for sensitive operations using Android's BiometricPrompt API, StrongBox KeyMint hardware security, and Passkey integration for Android 14+.

## Features

### 🔐 Core Capabilities
- **Biometric Authentication**: Fingerprint, Face, Iris recognition
- **StrongBox KeyMint**: Hardware-backed key storage (Android 9+)
- **Passkey Support**: FIDO2/WebAuthn authentication (Android 14+)
- **Device Credentials Fallback**: PIN/Pattern/Password as backup
- **Crypto-based Authentication**: Keys bound to biometric authentication
- **Enrollment Detection**: Prompts users to set up biometrics

### 🛡️ Security Features
- **Key Invalidation**: Keys invalidated when biometrics change
- **Auth Timeout**: 30-second validity window after authentication
- **Strong Authentication**: Class 3 biometric requirements
- **User Authentication Required**: Keys cannot be used without auth
- **Lockout Protection**: Handles temporary and permanent lockouts

## Architecture

### Components

#### 1. BiometricAuthManager
**Location**: `app/src/main/java/com/obsidianbackup/security/BiometricAuthManager.kt`

Central authentication handler with the following responsibilities:
- Check biometric capability and enrollment status
- Show biometric prompt with customizable UI
- Handle authentication callbacks
- Provide error guidance
- Support crypto-based authentication

**Key Methods**:
```kotlin
// Check if biometric is available
fun getBiometricCapability(): BiometricCapability

// Authenticate user
suspend fun authenticate(
    activity: FragmentActivity,
    title: String,
    subtitle: String? = null,
    description: String? = null,
    allowDeviceCredential: Boolean = true,
    cipher: Cipher? = null
): BiometricResult

// Authenticate for specific operations
suspend fun authenticateForOperation(
    activity: FragmentActivity,
    operation: SensitiveOperation,
    cipher: Cipher? = null
): BiometricResult
```

#### 2. PasskeyManager (Android 14+)
**Location**: `app/src/main/java/com/obsidianbackup/security/PasskeyManager.kt`

Manages FIDO2/WebAuthn passkey authentication:
- Create and register passkeys
- Authenticate with existing passkeys
- Platform authenticator (device-bound)
- Integration with Android CredentialManager

**Key Methods**:
```kotlin
// Create new passkey
suspend fun createPasskey(
    userId: String,
    userName: String,
    userDisplayName: String = userName
): PasskeyResult

// Authenticate with passkey
suspend fun authenticateWithPasskey(): PasskeyResult

// Check if passkey is supported
fun isPasskeySupported(): Boolean
```

#### 3. EncryptionEngine (Updated)
**Location**: `app/src/main/java/com/obsidianbackup/crypto/EncryptionEngine.kt`

Enhanced with biometric authentication support:
- Generate keys with biometric requirement
- StrongBox backing when available
- Cipher initialization for biometric prompt
- Authentication validity duration

**Key Updates**:
```kotlin
// Generate key with biometric requirement
fun generateKey(
    keyId: String,
    requireBiometric: Boolean = false,
    authValidityDuration: Int = 30
): String

// Initialize cipher for encryption (can throw UserNotAuthenticatedException)
fun initCipherForEncryption(keyAlias: String): Cipher

// Initialize cipher for decryption
fun initCipherForDecryption(keyAlias: String, iv: ByteArray): Cipher

// Check if key requires authentication
fun keyRequiresAuthentication(keyAlias: String): Boolean
```

#### 4. BiometricSettings
**Location**: `app/src/main/java/com/obsidianbackup/security/BiometricSettings.kt`

Manages user preferences for biometric authentication:
- Enable/disable biometric globally
- Per-operation biometric requirements
- Passkey preference
- DataStore-backed reactive settings

## Usage Examples

### Example 1: Authenticate for Backup

```kotlin
// In your Activity or Fragment
class BackupActivity : FragmentActivity() {
    
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var encryptionEngine: EncryptionEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        biometricAuthManager = BiometricAuthManager(this)
        encryptionEngine = EncryptionEngine()
    }
    
    suspend fun performBackup() {
        // Check biometric capability
        when (val capability = biometricAuthManager.getBiometricCapability()) {
            is BiometricCapability.Available -> {
                // Biometric available, proceed with authentication
                val keyAlias = "backup_key_${System.currentTimeMillis()}"
                
                // Generate key with biometric requirement
                encryptionEngine.generateKey(
                    keyId = keyAlias,
                    requireBiometric = true,
                    authValidityDuration = 30
                )
                
                // Initialize cipher for encryption
                val cipher = try {
                    encryptionEngine.initCipherForEncryption(keyAlias)
                } catch (e: UserNotAuthenticatedException) {
                    // User needs to authenticate
                    null
                }
                
                // Show biometric prompt
                val result = biometricAuthManager.authenticateForOperation(
                    activity = this,
                    operation = SensitiveOperation.BACKUP,
                    cipher = cipher
                )
                
                when (result) {
                    is BiometricResult.Success -> {
                        // Authentication successful
                        // Use result.cipher for encryption
                        val authenticatedCipher = result.cipher
                        encryptBackupData(authenticatedCipher, keyAlias)
                    }
                    is BiometricResult.Error -> {
                        // Show error to user
                        val guidance = biometricAuthManager.getErrorGuidance(result)
                        showError(guidance)
                    }
                }
            }
            is BiometricCapability.NotEnrolled -> {
                // Prompt user to enroll biometric
                showEnrollmentPrompt()
            }
            is BiometricCapability.NotAvailable -> {
                // Biometric not available, use alternative auth
                showAlternativeAuth()
            }
        }
    }
    
    private suspend fun encryptBackupData(cipher: Cipher?, keyAlias: String) {
        val inputFile = File("/path/to/backup")
        val outputFile = File("/path/to/encrypted/backup")
        
        val metadata = encryptionEngine.encryptFile(
            inputFile = inputFile,
            outputFile = outputFile,
            keyAlias = keyAlias,
            cipher = cipher
        )
        
        // Save metadata
        saveBackupMetadata(metadata)
    }
}
```

### Example 2: Authenticate for Restore

```kotlin
suspend fun performRestore(backupFile: File, keyAlias: String) {
    // Read IV from encrypted file
    val iv = backupFile.inputStream().use { input ->
        ByteArray(12).apply { input.read(this) }
    }
    
    // Initialize cipher for decryption
    val cipher = try {
        encryptionEngine.initCipherForDecryption(keyAlias, iv)
    } catch (e: UserNotAuthenticatedException) {
        null
    }
    
    // Authenticate user
    val result = biometricAuthManager.authenticateForOperation(
        activity = this,
        operation = SensitiveOperation.RESTORE,
        cipher = cipher
    )
    
    when (result) {
        is BiometricResult.Success -> {
            // Decrypt with authenticated cipher
            val decryptResult = encryptionEngine.decryptFile(
                inputFile = backupFile,
                outputFile = File("/path/to/restored/data"),
                keyAlias = keyAlias,
                cipher = result.cipher
            )
            
            decryptResult.fold(
                onSuccess = { showSuccess("Restore completed") },
                onFailure = { showError(it.message ?: "Restore failed") }
            )
        }
        is BiometricResult.Error -> {
            val guidance = biometricAuthManager.getErrorGuidance(result)
            showError(guidance)
        }
    }
}
```

### Example 3: Settings Integration

```kotlin
// In SettingsScreen.kt
@Composable
fun BiometricSettingsSection() {
    val context = LocalContext.current
    val settings = remember { BiometricSettings(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    // Observe settings
    val biometricEnabled by settings.biometricEnabled.collectAsState(initial = false)
    val biometricForBackup by settings.biometricForBackup.collectAsState(initial = false)
    val biometricForRestore by settings.biometricForRestore.collectAsState(initial = true)
    
    // Check capability
    val capability = biometricAuthManager.getBiometricCapability()
    
    Column {
        Text("Security", style = MaterialTheme.typography.titleLarge)
        
        // Master toggle
        SwitchPreference(
            title = "Biometric Authentication",
            summary = when (capability) {
                is BiometricCapability.Available -> 
                    "Protect sensitive operations with biometric"
                is BiometricCapability.NotEnrolled -> 
                    "No biometric enrolled. Tap to enroll."
                is BiometricCapability.NotAvailable -> 
                    "Biometric not available: ${capability.error.message}"
            },
            checked = biometricEnabled && capability is BiometricCapability.Available,
            enabled = capability is BiometricCapability.Available,
            onCheckedChange = { enabled ->
                lifecycleScope.launch {
                    settings.setBiometricEnabled(enabled)
                }
            }
        )
        
        if (biometricEnabled) {
            // Per-operation toggles
            SwitchPreference(
                title = "Require for Backup",
                summary = "Authenticate before creating backups",
                checked = biometricForBackup,
                onCheckedChange = { enabled ->
                    lifecycleScope.launch {
                        settings.setBiometricForBackup(enabled)
                    }
                }
            )
            
            SwitchPreference(
                title = "Require for Restore",
                summary = "Authenticate before restoring backups",
                checked = biometricForRestore,
                onCheckedChange = { enabled ->
                    lifecycleScope.launch {
                        settings.setBiometricForRestore(enabled)
                    }
                }
            )
        }
        
        // Enrollment prompt
        if (capability is BiometricCapability.NotEnrolled) {
            Button(
                onClick = { openBiometricEnrollmentSettings() }
            ) {
                Text("Set Up Biometric")
            }
        }
    }
}

fun openBiometricEnrollmentSettings() {
    val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
    startActivity(intent)
}
```

### Example 4: Passkey Authentication (Android 14+)

```kotlin
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun setupPasskey() {
    if (!PasskeyManager.isPasskeySupported()) {
        showError("Passkeys not supported on this device")
        return
    }
    
    val passkeyManager = PasskeyManager(context)
    
    // Generate user ID
    val userId = UUID.randomUUID().toString()
    
    // Create passkey
    val result = passkeyManager.createPasskey(
        userId = userId,
        userName = "user@example.com",
        userDisplayName = "User Name"
    )
    
    when (result) {
        is PasskeyResult.Success -> {
            // Store user ID
            passkeyManager.storeUserId(userId)
            showSuccess("Passkey created successfully")
        }
        is PasskeyResult.Error -> {
            showError(result.message)
        }
        is PasskeyResult.Cancelled -> {
            showInfo("Passkey creation cancelled")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun authenticateWithPasskey() {
    val passkeyManager = PasskeyManager(context)
    
    if (!passkeyManager.isPasskeyRegistered()) {
        showError("No passkey registered. Please create one first.")
        return
    }
    
    val result = passkeyManager.authenticateWithPasskey()
    
    when (result) {
        is PasskeyResult.Success -> {
            showSuccess("Authentication successful")
            proceedWithOperation()
        }
        is PasskeyResult.Error -> {
            showError(result.message)
        }
        is PasskeyResult.Cancelled -> {
            showInfo("Authentication cancelled")
        }
    }
}
```

## Error Handling

### Common Errors

| Error Code | Description | Guidance |
|------------|-------------|----------|
| `ERROR_CANCELED` | User canceled authentication | Allow retry |
| `ERROR_USER_CANCELED` | User explicitly canceled | Allow retry |
| `ERROR_LOCKOUT` | Too many failed attempts | Wait 30 seconds |
| `ERROR_LOCKOUT_PERMANENT` | Biometric locked | Use device credential |
| `ERROR_NO_BIOMETRICS` | No biometric enrolled | Prompt enrollment |
| `ERROR_HW_NOT_PRESENT` | No biometric hardware | Disable feature |
| `ERROR_HW_UNAVAILABLE` | Hardware temporarily unavailable | Retry later |
| `ERROR_TIMEOUT` | Authentication timeout | Retry |
| `UserNotAuthenticatedException` | Key requires authentication | Show biometric prompt |
| `KeyPermanentlyInvalidatedException` | Biometric changed | Regenerate key |

### Error Handling Pattern

```kotlin
try {
    val cipher = encryptionEngine.initCipherForEncryption(keyAlias)
    // Proceed with authentication
} catch (e: UserNotAuthenticatedException) {
    // Show biometric prompt
    val result = biometricAuthManager.authenticate(...)
    // Handle result
} catch (e: KeyPermanentlyInvalidatedException) {
    // Biometric has changed, key is invalidated
    // Regenerate key or prompt user
    showDialog("Biometric changed. Please set up encryption again.")
    encryptionEngine.deleteKey(keyAlias)
    // Generate new key
} catch (e: Exception) {
    // Handle other errors
    showError(e.message ?: "Unknown error")
}
```

## Security Considerations

### 1. StrongBox KeyMint
- Hardware security module (HSM) for key storage
- Tamper-resistant secure element
- Only available on supported devices (Pixel 3+, Samsung S9+, etc.)
- Automatically enabled when available

### 2. Authentication Validity Duration
- Default: 30 seconds
- Keys locked after timeout
- Configurable per-key
- Balance between security and UX

### 3. Key Invalidation
- Keys invalidated when biometric enrollment changes
- Prevents unauthorized access with old biometric
- Requires user to re-setup encryption

### 4. Fallback Authentication
- Device credential (PIN/Pattern/Password) as fallback
- Ensures access even if biometric fails
- Can be disabled for high-security scenarios

### 5. Passkey Security (Android 14+)
- FIDO2/WebAuthn standard
- Phishing-resistant
- Device-bound (platform authenticator)
- No password transmission

## Testing

### Manual Testing Checklist

- [ ] Biometric enrollment detection
- [ ] Successful authentication with fingerprint
- [ ] Successful authentication with face unlock
- [ ] Cancel authentication
- [ ] Multiple failed attempts (lockout)
- [ ] Device credential fallback
- [ ] Key invalidation on biometric change
- [ ] StrongBox availability check
- [ ] Passkey creation (Android 14+)
- [ ] Passkey authentication (Android 14+)
- [ ] Settings persistence
- [ ] Error guidance messages

### Test Devices

**Recommended Test Devices**:
- Pixel 4+ (fingerprint + face unlock)
- Samsung Galaxy S21+ (fingerprint + face unlock)
- OnePlus 8+ (fingerprint)
- Android 14 emulator (for passkeys)

### Testing Without Hardware

```kotlin
// Use emulator with virtual biometric
// In emulator, use adb commands:

// Enroll fingerprint
adb -e emu finger touch 1

// Simulate fingerprint touch
adb -e emu finger touch 1
```

## Dependencies

Added to `app/build.gradle.kts`:

```kotlin
// Biometric Authentication
implementation("androidx.biometric:biometric:1.2.0-alpha05")

// DataStore for settings
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Credentials Manager for Passkeys (Android 14+)
implementation("androidx.credentials:credentials:1.3.0")
implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
```

## Permissions

No additional permissions required. Biometric authentication uses built-in Android capabilities.

## API Requirements

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **BiometricPrompt**: API 28+ (fallback to FingerprintManager on API 23-27)
- **StrongBox**: API 28+ (Android 9.0)
- **Passkeys**: API 34+ (Android 14.0)

## Migration Guide

### Migrating Existing Keys

If you have existing encryption keys without biometric requirement:

```kotlin
suspend fun migrateKeyToBiometric(oldKeyAlias: String) {
    // 1. Decrypt existing data with old key
    val decryptedData = decryptWithOldKey(oldKeyAlias)
    
    // 2. Generate new key with biometric requirement
    val newKeyAlias = encryptionEngine.generateKey(
        keyId = "biometric_${System.currentTimeMillis()}",
        requireBiometric = true
    )
    
    // 3. Encrypt data with new key
    encryptWithNewKey(decryptedData, newKeyAlias)
    
    // 4. Delete old key
    encryptionEngine.deleteKey(oldKeyAlias)
}
```

## Best Practices

1. **Always check capability before enabling**: Don't enable biometric if hardware is not available
2. **Provide fallback**: Always allow device credential as fallback
3. **Handle key invalidation gracefully**: Prompt user to re-setup when biometric changes
4. **Use appropriate timeouts**: 30s is good balance, adjust based on use case
5. **Clear error messages**: Use `getErrorGuidance()` for user-friendly messages
6. **Test on real devices**: Emulator biometric simulation is limited
7. **Consider StrongBox availability**: Not all devices support it
8. **Android 14+ passkeys**: Offer as premium feature for newer devices

## Future Enhancements

- [ ] Biometric strength selection (weak/strong)
- [ ] Custom authentication timeout per operation
- [ ] Multi-factor authentication (biometric + PIN)
- [ ] Biometric analytics (success rate, error tracking)
- [ ] Cloud-synced passkeys (cross-device)
- [ ] Biometric re-authentication prompts
- [ ] Admin policies for enterprise deployments

## Troubleshooting

### Issue: "Biometric not available"
**Solution**: Check device has biometric hardware and at least one biometric enrolled

### Issue: "Key permanently invalidated"
**Solution**: User changed biometric enrollment. Delete old key and create new one.

### Issue: "StrongBox not available"
**Solution**: Device doesn't support StrongBox. Fallback to regular KeyStore (still secure).

### Issue: "Passkey creation fails"
**Solution**: Ensure Android 14+ and Google Play Services updated.

### Issue: "Authentication always fails"
**Solution**: Check authentication timeout hasn't expired. Re-initialize cipher.

## References

- [Android BiometricPrompt Documentation](https://developer.android.com/reference/androidx/biometric/BiometricPrompt)
- [Android Keystore System](https://developer.android.com/privacy-and-security/keystore)
- [StrongBox KeyMint](https://source.android.com/docs/security/features/keystore)
- [Android Credential Manager](https://developer.android.com/training/sign-in/passkeys)
- [FIDO2/WebAuthn](https://fidoalliance.org/fido2/)

---

**Implementation Date**: 2024
**Author**: ObsidianBackup Team
**Version**: 1.0.0
