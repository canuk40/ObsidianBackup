# Biometric Security

Secure your backups with biometric authentication.

## Overview

ObsidianBackup supports multiple biometric authentication methods:

- **Fingerprint**: Fingerprint sensor authentication
- **Face Recognition**: Face unlock (Android 10+)
- **Passkey**: FIDO2 passkeys (Android 14+)
- **Device Credential**: PIN, pattern, or password fallback

## Prerequisites

- Android 8.0+ (API 26)
- Device with biometric hardware
- Biometric credentials enrolled in device settings
- Android 10+ for face authentication
- Android 14+ for passkey support

## Setup

### Enabling Biometric Authentication

1. Navigate to **Settings** → **Security**
2. Tap **Biometric Authentication**
3. Toggle **Enable Biometric Auth**
4. Choose authentication methods:
   - Fingerprint
   - Face
   - Passkey (if supported)
5. Configure authentication scope:
   - App launch
   - Backup operations
   - Restore operations
   - Settings access
6. Test authentication

### First-Time Configuration

**Authentication Flow:**
1. Enable biometric authentication
2. System prompts for biometric enrollment
3. Verify your identity (fingerprint, face, or passkey)
4. Set fallback method (PIN/pattern/password)
5. Configure grace period (optional)

## Authentication Methods

### Fingerprint Authentication

**Setup:**
1. Enroll fingerprints in device Settings → Security → Fingerprint
2. Enable in ObsidianBackup → Settings → Security → Fingerprint
3. Test authentication

**Features:**
- Fast authentication (~0.5 seconds)
- Works with screen off
- Multiple fingerprints supported
- Hardware-backed security

**Limitations:**
- Requires fingerprint sensor
- May not work with wet fingers
- Can fail with significant finger injuries

### Face Authentication

**Setup:**
1. Enroll face in device Settings → Security → Face Unlock
2. Enable in ObsidianBackup → Settings → Security → Face
3. Test authentication

**Features:**
- Convenient hands-free authentication
- Works in various lighting conditions
- Adaptive learning

**Security Classes:**
- **Class 3** (Strong): Secure face authentication (recommended)
- **Class 2** (Weak): Basic face detection (not recommended for sensitive data)

**Limitations:**
- Requires Android 10+
- May not work in darkness
- Can be affected by face coverings

### Passkey Authentication

**Setup:**
1. Enable passkeys in device Settings → Security → Passkeys
2. Create passkey for ObsidianBackup
3. Enable in app settings

**Features:**
- FIDO2 compliant
- Hardware-backed security
- No password needed
- Sync across devices (with Google)

**Requirements:**
- Android 14+
- Google account (for sync)
- Compatible device

**Limitations:**
- Newer feature, limited device support
- Requires Google Play Services

## Security Configuration

### Authentication Scope

Configure what requires biometric authentication:

```
Settings → Security → Authentication Scope
```

**Options:**
- **App Launch**: Authenticate when opening app
- **Backup Operations**: Authenticate before backup
- **Restore Operations**: Authenticate before restore
- **Settings Access**: Authenticate to change settings
- **Cloud Sync**: Authenticate before cloud operations
- **Plugin Installation**: Authenticate before installing plugins

### Grace Period

Allow temporary access without re-authentication:

```
Settings → Security → Grace Period
```

**Options:**
- None (always require auth)
- 1 minute
- 5 minutes
- 15 minutes
- 30 minutes
- Until app closes

**Use Cases:**
- Multiple backups in succession
- Configuration changes
- Testing and debugging

### Fallback Methods

Configure fallback when biometric fails:

```
Settings → Security → Fallback
```

**Options:**
- **Device Credential**: PIN, pattern, or password
- **App Password**: Separate password for app
- **None**: No fallback (lock out if biometric fails)

**Recommendation:** Always enable device credential fallback

## Backup Encryption with Biometric Keys

### Hardware-Backed Encryption

Use biometric authentication to secure encryption keys:

1. Enable biometric authentication
2. Enable backup encryption
3. Choose "Biometric-Protected Key"
4. Keys are stored in Android Keystore
5. Keys only accessible after biometric auth

**Benefits:**
- Keys never leave secure hardware
- Protection against key extraction
- Automatic key invalidation on biometric change

**Process:**
```
Backup → Encrypt with key → Store encrypted backup
Restore → Authenticate → Decrypt with key → Restore
```

### Key Storage Options

**Android Keystore (Recommended):**
- Hardware-backed security
- Biometric-protected
- Automatic key rotation
- TEE (Trusted Execution Environment) or StrongBox

**Encrypted Preferences:**
- Software encryption
- Password-protected
- Portable across devices
- Less secure than Keystore

**Custom Key:**
- User-provided key
- Full control
- User responsible for security

## Advanced Security Features

### Biometric Prompt Configuration

Customize authentication prompts:

```kotlin
{
  "title": "Authenticate to Backup",
  "subtitle": "Verify your identity",
  "description": "Use biometric to secure backup",
  "negative_button": "Cancel",
  "confirmation_required": true,
  "device_credential_allowed": true
}
```

### Authentication Strength

Configure required authentication strength:

**Strong (Class 3):**
- Fingerprint
- Iris
- Strong face authentication
- Hardware-backed

**Weak (Class 2):**
- Basic face detection
- Software-based
- Not recommended for sensitive data

**Setting:**
```
Settings → Security → Authentication Strength → Strong
```

### Timeout and Lockout

Configure security policies:

**Timeout Settings:**
- Failed attempts before lockout: 5
- Lockout duration: 30 seconds
- Max lockout duration: 5 minutes
- Permanent lockout after: Never / 10 attempts

**Auto-Lock:**
- Lock after screen off: Yes
- Lock after inactivity: 5 minutes
- Lock on app switch: No

## Integration with System Features

### Android Keystore Integration

ObsidianBackup integrates with Android Keystore:

**Features:**
- Hardware-backed key storage
- Biometric-protected keys
- Key invalidation on security changes
- TEE or StrongBox support

**Key Types:**
- AES-256 for encryption
- RSA-2048 for key wrapping
- HMAC-SHA256 for integrity

### Credential Manager (Android 14+)

Use Android's Credential Manager:

**Features:**
- Unified credential interface
- Passkey support
- Password autofill
- Biometric authentication

**Integration:**
```kotlin
val credentialManager = CredentialManager.create(context)
val request = GetCredentialRequest.Builder()
    .addCredentialOption(GetPublicKeyCredentialOption(...))
    .build()
val result = credentialManager.getCredential(context, request)
```

## Security Best Practices

### For Users

1. **Enable Strong Authentication**: Use Class 3 biometrics
2. **Configure Fallback**: Always have device credential backup
3. **Regular Security Updates**: Keep device updated
4. **Multiple Biometrics**: Enroll multiple fingerprints
5. **Monitor Access**: Review authentication logs
6. **Secure Device**: Use device encryption and secure lock screen
7. **Grace Period**: Use short grace periods for security
8. **Test Regularly**: Verify authentication works

### For Developers

1. **Use BiometricPrompt API**: Standard Android API
2. **Handle Failures Gracefully**: Proper error handling
3. **Implement Fallback**: Always provide alternative
4. **Key Invalidation**: Invalidate keys on security changes
5. **Timeout Handling**: Implement proper timeouts
6. **Secure Key Storage**: Use Android Keystore
7. **Minimal Permissions**: Request only needed permissions
8. **Audit Logging**: Log authentication attempts

## Troubleshooting

### Biometric Authentication Not Available

**Possible Causes:**
- No biometric hardware
- No biometrics enrolled
- Biometric temporarily locked out
- Security patch needed

**Solutions:**
1. Check device has biometric sensor
2. Enroll biometric in device settings
3. Wait for lockout period
4. Update security patches

### Authentication Always Fails

**Check:**
1. Biometric enrolled correctly
2. Sensor is clean
3. Correct finger/face being used
4. Not locked out due to failed attempts
5. App permissions granted

**Solutions:**
- Re-enroll biometric
- Clean sensor
- Use fallback method
- Wait for lockout expiry

### Keys Invalidated

**Causes:**
- Biometric added or removed
- Device security changed
- Security patch applied
- Factory reset

**Solutions:**
1. Backup will prompt for re-authentication
2. Use password fallback if available
3. May need to re-encrypt backups
4. Contact support if unable to access

### Passkey Not Working

**Requirements:**
- Android 14+
- Google Play Services updated
- Passkey enrolled
- Network connectivity (for sync)

**Solutions:**
1. Update to Android 14+
2. Update Google Play Services
3. Create new passkey
4. Check network connection

## Migration Guide

### Upgrading from Password-Only

1. Enable biometric authentication
2. Keep password as fallback
3. Test biometric authentication
4. Gradually transition operations
5. Monitor for issues
6. Optionally remove password requirement

### Switching Biometric Methods

1. Enroll new biometric in device settings
2. Enable new method in app
3. Test new method
4. Disable old method (if desired)
5. Keys remain valid (same device)

### Transferring to New Device

**With Passkeys (Android 14+):**
1. Enable Google account sync
2. Passkeys sync automatically
3. Set up biometrics on new device
4. Authenticate to access synced backups

**Without Passkeys:**
1. Export encrypted backups
2. Transfer password/key securely
3. Import on new device
4. Set up biometrics on new device
5. Re-encrypt with new biometric key (optional)

## Compliance and Standards

### Security Standards

- **FIDO2**: Passkey authentication
- **WebAuthn**: Web authentication standard
- **Android CDD**: Compatibility Definition Document
- **BiometricPrompt**: Android standard API

### Privacy

- Biometric data never leaves device
- No biometric data in backups
- Keys stored in secure hardware
- No cloud storage of biometrics

### Certifications

Compatible with:
- Android Enterprise
- SafetyNet Attestation
- Play Integrity API
- Hardware Security Module (HSM)

## API Reference

See [API Documentation](../api/index.html) for detailed API reference.

**Key Classes:**
- `BiometricAuthenticator`
- `BiometricPromptBuilder`
- `KeystoreManager`
- `CredentialManager`

## Next Steps

- [Security Policy](../developer-guides/security-policy.md) - Security guidelines
- [Backup Configuration](backup-configuration.md) - Configure encryption
- [Troubleshooting](troubleshooting.md) - Common issues
