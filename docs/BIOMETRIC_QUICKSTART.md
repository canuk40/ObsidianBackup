# Biometric Authentication - Quick Reference

## Quick Start

### 1. Add Dependencies
Already added to `app/build.gradle.kts`:
```kotlin
implementation("androidx.biometric:biometric:1.2.0-alpha05")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("androidx.credentials:credentials:1.3.0") // Android 14+
```

### 2. Initialize Components

```kotlin
class YourActivity : FragmentActivity() {
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var encryptionEngine: EncryptionEngine
    private lateinit var biometricSettings: BiometricSettings
    private lateinit var authIntegration: BiometricAuthIntegration
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        biometricAuthManager = BiometricAuthManager(this)
        encryptionEngine = EncryptionEngine()
        biometricSettings = BiometricSettings(this)
        authIntegration = BiometricAuthIntegration(this, encryptionEngine, biometricAuthManager, biometricSettings)
    }
}
```

### 3. Check Biometric Availability

```kotlin
when (val capability = biometricAuthManager.getBiometricCapability()) {
    is BiometricCapability.Available -> {
        // Biometric available (capability.hasStrongBox shows if StrongBox available)
        enableBiometric()
    }
    is BiometricCapability.NotEnrolled -> {
        // Prompt user to enroll
        showEnrollmentPrompt()
    }
    is BiometricCapability.NotAvailable -> {
        // Biometric not available: capability.error.message
        disableBiometric()
    }
}
```

### 4. Simple Authentication

```kotlin
suspend fun authenticate() {
    val result = biometricAuthManager.authenticate(
        activity = this,
        title = "Authenticate",
        subtitle = "Verify your identity",
        allowDeviceCredential = true
    )
    
    when (result) {
        is BiometricResult.Success -> {
            // Authentication successful
            proceedWithOperation()
        }
        is BiometricResult.Error -> {
            // Show error: biometricAuthManager.getErrorGuidance(result)
            showError(biometricAuthManager.getErrorGuidance(result))
        }
    }
}
```

### 5. Encrypt with Biometric

```kotlin
suspend fun encryptBackup(inputFile: File, outputFile: File) {
    val keyId = "backup_${System.currentTimeMillis()}"
    
    val result = authIntegration.performBackupWithAuth(
        activity = this,
        inputFile = inputFile,
        outputFile = outputFile,
        keyId = keyId
    )
    
    when (result) {
        is AuthResult.Success -> {
            // Backup encrypted successfully
            val metadata = result.data
        }
        is AuthResult.Failure -> {
            // Error: result.message
        }
        is AuthResult.NotEnrolled -> {
            // User needs to enroll: result.message
        }
    }
}
```

### 6. Decrypt with Biometric

```kotlin
suspend fun decryptBackup(encryptedFile: File, outputFile: File, keyAlias: String) {
    val result = authIntegration.performRestoreWithAuth(
        activity = this,
        inputFile = encryptedFile,
        outputFile = outputFile,
        keyAlias = keyAlias
    )
    
    when (result) {
        is AuthResult.Success -> {
            // Restore completed
        }
        is AuthResult.Failure -> {
            // Error: result.message
        }
        is AuthResult.NotEnrolled -> {
            // User needs to enroll
        }
    }
}
```

## Common Patterns

### Check Settings Before Operation

```kotlin
suspend fun shouldRequireBiometric(operation: SensitiveOperation): Boolean {
    return authIntegration.shouldUseBiometric(operation)
}
```

### Handle Key Invalidation

```kotlin
try {
    val cipher = encryptionEngine.initCipherForEncryption(keyAlias)
} catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
    // Biometric changed, key invalidated
    authIntegration.handleKeyInvalidation(keyAlias)
    showDialog("Biometric changed. Please set up encryption again.")
}
```

### Prompt Enrollment

```kotlin
if (authIntegration.checkEnrollmentAndPrompt(this) is EnrollmentStatus.NotEnrolled) {
    authIntegration.openEnrollmentSettings(this)
}
```

## Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| `ERROR_CANCELED` | User canceled | Allow retry |
| `ERROR_LOCKOUT` | Too many attempts | Wait 30s |
| `ERROR_LOCKOUT_PERMANENT` | Locked permanently | Use PIN/Pattern |
| `ERROR_NO_BIOMETRICS` | Not enrolled | Prompt enrollment |
| `ERROR_HW_NOT_PRESENT` | No hardware | Disable feature |
| `ERROR_TIMEOUT` | Authentication timeout | Retry |

## Settings Integration

### Enable/Disable Biometric

```kotlin
// Enable globally
biometricSettings.setBiometricEnabled(true)

// Enable for specific operations
biometricSettings.setBiometricForBackup(true)
biometricSettings.setBiometricForRestore(true)
biometricSettings.setBiometricForSettings(true)

// Observe settings
val enabled = biometricSettings.biometricEnabled.collectAsState(initial = false)
```

### Settings UI

```kotlin
@Composable
fun BiometricToggle() {
    val settings = remember { BiometricSettings(LocalContext.current) }
    val enabled by settings.biometricEnabled.collectAsState(initial = false)
    
    Switch(
        checked = enabled,
        onCheckedChange = { 
            lifecycleScope.launch {
                settings.setBiometricEnabled(it)
            }
        }
    )
}
```

## Key Generation

### Without Biometric

```kotlin
val keyAlias = encryptionEngine.generateKey(
    keyId = "my_key",
    requireBiometric = false
)
```

### With Biometric (30s timeout)

```kotlin
val keyAlias = encryptionEngine.generateKey(
    keyId = "my_key",
    requireBiometric = true,
    authValidityDuration = 30
)
```

### With StrongBox (Automatic)

StrongBox is automatically enabled when available. Check with:

```kotlin
val capability = biometricAuthManager.getBiometricCapability()
if (capability is BiometricCapability.Available && capability.hasStrongBox) {
    // StrongBox available
}
```

## Passkeys (Android 14+)

### Check Support

```kotlin
if (PasskeyManager.isPasskeySupported()) {
    // Passkeys available
}
```

### Create Passkey

```kotlin
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun createPasskey() {
    val passkeyManager = PasskeyManager(context)
    
    val result = passkeyManager.createPasskey(
        userId = UUID.randomUUID().toString(),
        userName = "user@example.com",
        userDisplayName = "User Name"
    )
    
    when (result) {
        is PasskeyResult.Success -> {
            passkeyManager.storeUserId(result.userId)
        }
        is PasskeyResult.Error -> {
            // Handle error
        }
        is PasskeyResult.Cancelled -> {
            // User cancelled
        }
    }
}
```

### Authenticate with Passkey

```kotlin
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun authenticateWithPasskey() {
    val passkeyManager = PasskeyManager(context)
    
    val result = passkeyManager.authenticateWithPasskey()
    
    when (result) {
        is PasskeyResult.Success -> {
            // Authenticated
        }
        is PasskeyResult.Error -> {
            // Error
        }
        is PasskeyResult.Cancelled -> {
            // Cancelled
        }
    }
}
```

## Testing

### Emulator Testing

```bash
# Enroll fingerprint
adb -e emu finger touch 1

# Simulate fingerprint touch
adb -e emu finger touch 1
```

### Test Checklist

- [ ] Successful authentication
- [ ] Cancel authentication
- [ ] Multiple failed attempts
- [ ] Device credential fallback
- [ ] No biometric enrolled
- [ ] Key invalidation on biometric change
- [ ] StrongBox detection
- [ ] Settings persistence
- [ ] Error messages

## Files Created

```
app/src/main/java/com/obsidianbackup/
├── crypto/
│   └── EncryptionEngine.kt (updated)
└── security/
    ├── BiometricAuthManager.kt
    ├── PasskeyManager.kt
    ├── BiometricSettings.kt
    ├── BiometricAuthIntegration.kt
    └── BiometricExampleUsage.kt
```

## Documentation

- Full documentation: `BIOMETRIC_AUTHENTICATION.md`
- Example usage: `app/src/main/java/com/obsidianbackup/security/BiometricExampleUsage.kt`

## Support

- Minimum SDK: 26 (Android 8.0)
- BiometricPrompt: API 28+
- StrongBox: API 28+ (Android 9.0)
- Passkeys: API 34+ (Android 14.0)
