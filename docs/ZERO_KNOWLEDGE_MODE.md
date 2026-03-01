# Zero-Knowledge Encryption Mode

## Overview

ObsidianBackup's Zero-Knowledge Encryption mode provides the highest level of privacy and security by ensuring that **only you** have access to your encryption keys. This document explains the architecture, security properties, usage, and limitations of this mode.

## Table of Contents

1. [What is Zero-Knowledge Encryption?](#what-is-zero-knowledge-encryption)
2. [Security Architecture](#security-architecture)
3. [Key Features](#key-features)
4. [Setup Guide](#setup-guide)
5. [Usage Guide](#usage-guide)
6. [Key Management](#key-management)
7. [Privacy Audit](#privacy-audit)
8. [Encrypted Search](#encrypted-search)
9. [Local-Only Mode](#local-only-mode)
10. [Security Considerations](#security-considerations)
11. [Threat Model](#threat-model)
12. [Technical Specifications](#technical-specifications)
13. [FAQ](#faq)
14. [Warnings and Limitations](#warnings-and-limitations)

---

## What is Zero-Knowledge Encryption?

Zero-Knowledge Encryption is a security model where:

- **Your encryption keys NEVER leave your device**
- **ObsidianBackup servers cannot decrypt your data**
- **No account recovery or key escrow is possible**
- **You are solely responsible for key backup and management**

### The Trade-Off

**Maximum Privacy = Maximum Responsibility**

✅ **You gain:**
- Complete data privacy
- No trust required in service provider
- Protection against server breaches
- Proof that no one else can access your data

⚠️ **You accept:**
- Lost passphrase = Lost data FOREVER
- No password reset or account recovery
- Manual key backup is critical
- Higher complexity vs. standard encryption

---

## Security Architecture

### Layered Security Model

```
┌─────────────────────────────────────────────┐
│         User Master Passphrase              │
│         (User memorizes this)               │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │ PBKDF2-HMAC-SHA512  │
         │ 600,000 iterations  │
         │ 256-bit salt        │
         └──────────┬──────────┘
                    │
                    ▼
         ┌─────────────────────┐
         │  Master Key (AES-256)│
         │  (Cached in memory)  │
         └──────────┬──────────┘
                    │
                    ▼
         ┌─────────────────────┐
         │   AES-256-GCM       │
         │   12-byte IV        │
         │   128-bit auth tag  │
         └─────────────────────┘
                    │
                    ▼
         ┌─────────────────────┐
         │  Encrypted Backups  │
         └─────────────────────┘
```

### Components

#### 1. **ZeroKnowledgeEncryption.kt**
Core cryptographic operations:
- Key derivation (PBKDF2)
- Encryption/decryption (AES-256-GCM)
- Key export/import
- Searchable encryption
- Integrity verification

#### 2. **ZeroKnowledgeManager.kt**
Lifecycle management:
- Configuration storage
- Key caching
- Privacy audits
- Local-only mode enforcement

#### 3. **ZeroKnowledgeScreen.kt**
User interface:
- Setup wizard
- Key management
- Privacy settings
- Audit results

---

## Key Features

### 1. Client-Side Only Encryption
- All encryption happens on your device
- Keys are derived from your passphrase locally
- No keys are transmitted over network

### 2. PBKDF2 Key Derivation
- **Algorithm:** PBKDF2-HMAC-SHA512
- **Iterations:** 600,000 (OWASP 2023 recommendation)
- **Salt:** 256-bit cryptographically random
- **Output:** 256-bit AES key

### 3. AES-256-GCM Encryption
- **Algorithm:** AES-256 in GCM mode
- **Key Size:** 256 bits (32 bytes)
- **IV:** 96 bits (12 bytes) - unique per encryption
- **Auth Tag:** 128 bits - integrity verification
- **Authenticated encryption:** Protects against tampering

### 4. Key Backup/Restore
- Export encrypted key backup
- Backup protected with separate passphrase
- Base64-encoded for easy storage
- Import on new device or after data loss

### 5. Privacy Audit Mode
- Verify zero-knowledge properties
- Detect potential privacy leaks
- Check key storage locations
- Ensure no cloud key access

### 6. Local-Only Mode
- Disable all network operations
- Never sync encrypted data to cloud
- Maximum privacy guarantee
- Air-gapped backup storage

### 7. Searchable Encryption (Optional)
- Search encrypted backups without decryption
- HMAC-based deterministic indexing
- Exact match searches
- Optional feature (can be disabled)

---

## Setup Guide

### Prerequisites

- Android device with ObsidianBackup installed
- Strong passphrase (12+ characters recommended)
- Secure backup storage for key export

### Initial Setup

1. **Navigate to Settings**
   ```
   Settings → Encryption → Zero-Knowledge Encryption
   ```

2. **Read Critical Warning**
   - Understand that key loss = data loss
   - Accept sole responsibility for key management
   - No recovery options available

3. **Set Master Passphrase**
   - Choose a strong, memorable passphrase
   - Minimum 12 characters recommended
   - Use mix of letters, numbers, symbols
   - DO NOT use common passwords

4. **Confirm Passphrase**
   - Re-enter passphrase to confirm
   - Ensure accuracy (no typos)

5. **Setup Complete**
   - Master key derived from passphrase
   - Key cached in memory (app lifecycle)
   - Salt stored securely in DataStore

### Post-Setup (Critical!)

**IMMEDIATELY Export Your Key Backup:**

1. Go to "Key Management" section
2. Click "Export Key Backup"
3. Set a backup passphrase (can differ from master)
4. Save the Base64 backup string securely:
   - Password manager (encrypted vault)
   - Offline storage (USB drive, paper backup)
   - Multiple secure locations (redundancy)

⚠️ **Without key backup, device loss = data loss!**

---

## Usage Guide

### Unlocking on App Start

After device reboot or app restart:

1. Open ObsidianBackup
2. Navigate to Zero-Knowledge settings
3. Click "Unlock"
4. Enter master passphrase
5. Key is cached until app closes or you lock manually

### Creating Encrypted Backups

Once unlocked:

1. Configure backup as usual
2. Select apps/data to backup
3. Enable encryption in backup settings
4. Backups are automatically encrypted with master key
5. Encrypted files stored with `.zkenc` extension

### Restoring Encrypted Backups

1. Ensure Zero-Knowledge mode is unlocked
2. Navigate to restore screen
3. Select encrypted backup
4. Automatic decryption with cached key
5. Data restored to original location

### Locking

To clear cached keys from memory:

1. Go to Zero-Knowledge settings
2. Click "Lock"
3. Keys securely wiped from memory
4. Must re-enter passphrase to unlock

---

## Key Management

### Key Lifecycle

```
┌─────────────┐
│   Creation  │ ← User sets master passphrase
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Caching   │ ← Key derived and cached in RAM
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Usage     │ ← Encrypt/decrypt operations
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Locking   │ ← Secure memory wipe
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Restore   │ ← Re-derive from passphrase
└─────────────┘
```

### Key Export

**What is exported:**
- Master encryption key (AES-256)
- Encrypted with backup passphrase
- Base64-encoded for storage

**Export format:**
```
OBZKE|VERSION|SALT|ENCRYPTED_KEY
```

**Security properties:**
- Backup passphrase can differ from master
- Export is itself encrypted (layered security)
- No plaintext key material in export

### Key Import

**Use cases:**
- New device setup
- After data loss
- Device replacement
- Recovery scenario

**Process:**
1. Click "Import Key Backup"
2. Paste Base64 backup string
3. Enter backup passphrase
4. Key imported and cached
5. Can now decrypt existing backups

### Key Rotation

To rotate keys (change passphrase):

1. Export current key backup (if not done)
2. Decrypt all existing backups
3. Disable Zero-Knowledge mode
4. Re-enable with new passphrase
5. Re-encrypt all backups

⚠️ **This is a manual, time-consuming process!**

---

## Privacy Audit

### What is Audited?

The privacy audit verifies:

✅ **Keys Stored Locally**
- Checks that keys are in device memory only
- Verifies no cloud key storage

✅ **No Cloud Key Access**
- Confirms server cannot access keys
- Validates zero-knowledge property

✅ **No Telemetry**
- Ensures no analytics in ZK mode
- No usage data transmitted

✅ **Encryption Active**
- Verifies encryption is enabled
- Confirms key derivation successful

✅ **Local-Only Mode**
- Checks if network disabled (optional)
- Confirms no cloud sync

### Running an Audit

1. Navigate to Zero-Knowledge settings
2. Scroll to "Privacy Audit" section
3. Click "Run Privacy Audit"
4. Review results

### Audit Results

**PASSED:** All checks successful, no warnings

**WARNINGS:** Issues detected:
- Key backup not exported (data loss risk)
- Local-only mode disabled (potential cloud sync)
- Master key not cached (unlock required)

### Audit Frequency

- Automatic: Every 24 hours (background)
- Manual: On-demand via UI
- Logged: Results stored in DataStore

---

## Encrypted Search

### How It Works

Searchable encryption allows you to search encrypted backups without full decryption.

**Technical approach:**
1. **Tokenization:** Content split into searchable terms
2. **HMAC Hashing:** Each term hashed with master key
3. **Deterministic:** Same term = same hash
4. **Indexed:** Hashes stored in search index
5. **Matching:** Query hashed and compared

### Security Properties

- ✅ Search without decryption
- ✅ No plaintext term leakage
- ✅ Deterministic for exact matches
- ⚠️ Frequency analysis possible (term counts visible)
- ⚠️ Not fully homomorphic (no complex queries)

### Limitations

- **Exact matches only:** No fuzzy search
- **Case-sensitive:** Terms normalized to lowercase
- **No wildcards:** Must match complete terms
- **Minimum length:** 3 characters per term
- **Performance:** Index building overhead

### Disabling Search

If you prefer maximum privacy:

1. Go to Privacy Settings
2. Toggle "Searchable Encryption" OFF
3. Search disabled, no index built
4. Requires full decryption to search

---

## Local-Only Mode

### What is Local-Only Mode?

A strict privacy mode that:
- **Disables all network operations**
- **Never syncs to cloud storage**
- **Keeps all backups on device**
- **Enforces air-gapped security**

### When to Use

- Maximum paranoia mode
- Sensitive data handling
- Compliance requirements
- Distrust of cloud providers
- Legal/regulatory constraints

### Enabling Local-Only Mode

1. Go to Privacy Settings
2. Toggle "Local-Only Mode" ON
3. All network operations blocked
4. Cloud sync disabled automatically

### Limitations

- ❌ No cloud backup redundancy
- ❌ No off-site disaster recovery
- ❌ Device loss = data loss (unless exported)
- ⚠️ Must manage local storage carefully
- ⚠️ Higher data loss risk

### Local-Only + Key Export

**Best practice:**
1. Enable local-only mode
2. Export key backup
3. Store key backup in separate location
4. Periodically export backups to external storage
5. Maintain offline backup redundancy

---

## Security Considerations

### Passphrase Strength

**Strong passphrase characteristics:**
- Length: 12+ characters (20+ recommended)
- Complexity: Mix uppercase, lowercase, numbers, symbols
- Uniqueness: Not used elsewhere
- Memorability: Can recall without writing down
- Entropy: High randomness

**Examples of strong passphrases:**
```
Good: correct-horse-battery-staple-42!
Good: My$3cureP@ssw0rd!2024
Good: Tr0ub4dor&3-extended-version
Bad:  password123
Bad:  qwerty
Bad:  letmein
```

### Key Storage

**Where keys are stored:**

1. **In Memory (Runtime):**
   - Cached while app is running
   - Cleared on app close or manual lock
   - Vulnerable to memory dumps (requires root)

2. **In DataStore (Encrypted):**
   - Salt stored for key derivation
   - Configuration settings
   - NOT the master key itself

3. **In Export (User-Managed):**
   - User chooses storage location
   - Should be offline/encrypted
   - Multiple redundant copies

**Where keys are NOT stored:**
- ❌ Cloud servers
- ❌ ObsidianBackup servers
- ❌ Plaintext on disk
- ❌ Android KeyStore (by design)

### Attack Vectors

**Threats mitigated:**
- ✅ Server breach (encrypted data useless)
- ✅ Network interception (keys never transmitted)
- ✅ Unauthorized access (requires passphrase)
- ✅ Data tampering (authenticated encryption)

**Threats NOT mitigated:**
- ⚠️ Weak passphrase (brute-force vulnerable)
- ⚠️ Keylogger on device (captures passphrase)
- ⚠️ Physical device access (memory dump possible)
- ⚠️ Supply chain attacks (compromised device)
- ⚠️ Social engineering (user tricked into revealing)

### Best Practices

1. **Strong Passphrase:**
   - Use password manager to generate
   - Memorize (don't write down)
   - Never share with anyone

2. **Key Backup:**
   - Export immediately after setup
   - Store in multiple secure locations
   - Use encrypted password manager
   - Consider offline backup (USB, paper)

3. **Regular Audits:**
   - Run privacy audit weekly
   - Verify no warnings
   - Check backup integrity

4. **Device Security:**
   - Enable full disk encryption
   - Use strong device PIN/password
   - Keep OS and apps updated
   - Install security updates promptly

5. **Operational Security:**
   - Lock when not in use
   - Avoid public Wi-Fi for setup
   - Verify app authenticity (official source)
   - Monitor for suspicious activity

---

## Threat Model

### Assumptions

**In scope:**
- Adversary with access to encrypted backups
- Adversary with access to cloud servers
- Adversary intercepting network traffic
- Passive surveillance by service provider

**Out of scope:**
- Adversary with physical device access (unlocked)
- Adversary with root/jailbreak capabilities
- Quantum computers (future threat)
- Nation-state targeted attacks

### Security Goals

1. **Confidentiality:** Only user can decrypt data
2. **Integrity:** Detect tampering with backups
3. **Authentication:** Verify backup authenticity
4. **Zero-Knowledge:** Server learns nothing about keys

### Trust Boundaries

**You must trust:**
- Your device hardware (no backdoors)
- Android OS (no key-stealing malware)
- ObsidianBackup app code (no malicious crypto)
- Cryptographic primitives (AES, PBKDF2 secure)

**You do NOT need to trust:**
- ObsidianBackup servers
- Cloud storage providers
- Network infrastructure
- Other apps on device (sandboxed)

---

## Technical Specifications

### Cryptographic Primitives

| Component | Algorithm | Parameters |
|-----------|-----------|------------|
| Key Derivation | PBKDF2-HMAC-SHA512 | 600,000 iterations |
| Salt | SecureRandom | 256 bits (32 bytes) |
| Encryption | AES-256-GCM | 256-bit key |
| IV | SecureRandom | 96 bits (12 bytes) |
| Auth Tag | GCM | 128 bits |
| Key Size | AES | 256 bits |

### File Format

**Encrypted File Structure:**
```
┌──────────────────────────────────────┐
│ Magic: "OBZKEF" (6 bytes)            │ Header
│ Version: 1 (1 byte)                  │
│ Salt: 32 bytes                       │
│ IV: 12 bytes                         │
├──────────────────────────────────────┤
│ Encrypted Data (variable length)     │ Body
│ ...                                  │
├──────────────────────────────────────┤
│ Authentication Tag (16 bytes)        │ Footer
└──────────────────────────────────────┘
```

**Key Backup Format:**
```
┌──────────────────────────────────────┐
│ Magic: "OBZKE" (5 bytes)             │
│ Version: 1 (1 byte)                  │
│ Backup Salt: 32 bytes                │
│ Encrypted Master Key (variable)      │
└──────────────────────────────────────┘
(All Base64-encoded for storage)
```

### Performance Characteristics

**Key Derivation:**
- Time: ~1-2 seconds on modern devices
- Memory: ~100 MB peak (PBKDF2 with 600k iterations)
- CPU: Intensive (intentional for brute-force resistance)

**Encryption:**
- Throughput: ~50-100 MB/s (device-dependent)
- Latency: <10ms for small files (<1MB)
- Overhead: ~50 bytes per file (header + tag)

**Search Index:**
- Build time: ~1 second per MB of content
- Storage: ~10% of original content size
- Query time: O(1) for exact match

### Standards Compliance

- ✅ OWASP: PBKDF2 iteration count (600k)
- ✅ NIST: AES-256-GCM for encryption
- ✅ FIPS 140-2: Approved algorithms
- ✅ RFC 8018: PBKDF2 specification
- ✅ SP 800-38D: GCM mode specification

---

## FAQ

### Q: Can ObsidianBackup recover my data if I forget my passphrase?

**A: NO.** Absolutely not. This is the fundamental property of zero-knowledge encryption. If you lose your passphrase, your data is permanently lost. There is no backdoor, no recovery mechanism, no support that can help. This is by design.

### Q: Why 600,000 iterations for PBKDF2?

**A:** This is the current OWASP recommendation (2023) for password-based key derivation. Higher iterations make brute-force attacks more expensive. As hardware improves, this number increases.

### Q: Is zero-knowledge encryption slower?

**A:** Slightly. Key derivation takes 1-2 seconds on first unlock. Encryption/decryption speeds are comparable to standard mode (both use AES-256-GCM). The main overhead is the initial passphrase unlock.

### Q: Can I use biometric authentication?

**A:** Not directly. Zero-knowledge mode requires passphrase-based key derivation. However, you can:
1. Unlock once with passphrase
2. Key cached until app closes
3. Use app normally without re-entering

A future update may allow biometric unlock of cached keys (but initial setup always requires passphrase).

### Q: What happens if my device is seized?

**A:** If zero-knowledge mode is locked (keys not cached), encrypted data is safe. Adversary would need your passphrase to derive keys. If unlocked (keys cached in memory), a forensic memory dump might reveal keys. Always lock when not in use.

### Q: Can I sync encrypted backups to cloud?

**A:** Yes, unless you enable Local-Only Mode. Encrypted backups can safely sync to cloud storage. The cloud provider sees only encrypted blobs, no keys, no metadata. However, Local-Only Mode provides maximum privacy by never touching the network.

### Q: Is this secure against quantum computers?

**A:** Current implementation (AES-256, PBKDF2) is believed to be quantum-resistant for symmetric encryption. However, if quantum computers break SHA-512 (used in PBKDF2), key derivation could be vulnerable. This is a future concern (10+ years away).

### Q: Can I export unencrypted backups if ZK mode is enabled?

**A:** Yes. Zero-knowledge mode is opt-in per backup. You can choose to:
- Create encrypted backups (with ZK keys)
- Create standard backups (unencrypted or keystore-encrypted)
- Mix both approaches

### Q: How do I verify the implementation is secure?

**A:** The code is open-source. Security audit recommendations:
1. Review cryptographic code in `ZeroKnowledgeEncryption.kt`
2. Verify key derivation parameters
3. Check for key storage locations
4. Run privacy audit mode
5. Monitor network traffic (should see no keys)

### Q: What if I want to change my passphrase?

**A:** Key rotation requires:
1. Decrypt all backups with old key
2. Disable ZK mode
3. Re-enable with new passphrase
4. Re-encrypt all backups

This is intentionally manual and time-consuming. Choose your passphrase carefully during initial setup.

---

## Warnings and Limitations

### ⚠️ CRITICAL WARNINGS

1. **KEY LOSS = DATA LOSS (PERMANENT)**
   - No recovery, no backdoors, no exceptions
   - ObsidianBackup cannot help you
   - Export and backup your keys immediately

2. **USER RESPONSIBILITY**
   - You are solely responsible for key management
   - No account recovery possible
   - No password reset mechanism

3. **DEVICE LOSS**
   - Lost device without key export = lost data
   - Cannot recover from cloud without keys
   - Backup keys to separate location

4. **PASSPHRASE SECURITY**
   - Weak passphrase = weak security
   - Shoulder surfing, keyloggers, social engineering
   - Never share or write down passphrase

### 📋 LIMITATIONS

1. **Performance:**
   - Key derivation takes 1-2 seconds
   - Higher CPU usage during unlock
   - Battery drain during encryption

2. **Usability:**
   - Must remember strong passphrase
   - Manual key backup required
   - No biometric-only unlock

3. **Compatibility:**
   - Cannot share encrypted backups easily
   - Recipient needs your key backup
   - No group encryption

4. **Search:**
   - Only exact matches supported
   - Case-sensitive (normalized)
   - No fuzzy or wildcard search

5. **Recovery:**
   - No cloud-based recovery
   - No support-assisted recovery
   - No backup to ObsidianBackup servers

### 🔒 SECURITY LIMITATIONS

1. **Not protected against:**
   - Physical device access (unlocked state)
   - Keyloggers/screen recorders
   - Memory dumps (requires root)
   - Supply chain attacks
   - $5 wrench attack (XKCD 538)

2. **Quantum computing:**
   - Current algorithms quantum-resistant
   - Future threat (10+ years)
   - May require algorithm upgrade

3. **Side channels:**
   - Timing attacks (mitigated by constant-time crypto)
   - Power analysis (hardware-dependent)
   - Cache attacks (OS sandboxing helps)

---

## Getting Help

### Support Channels

- **Documentation:** This file and inline code comments
- **Issues:** GitHub repository (for bugs)
- **Community:** Discussion forum
- **Email:** support@obsidianbackup.com

### What Support CAN Help With

- ✅ Understanding features
- ✅ Setup guidance
- ✅ Bug reports
- ✅ Feature requests
- ✅ General questions

### What Support CANNOT Help With

- ❌ Password/passphrase recovery
- ❌ Lost key recovery
- ❌ Decrypting without keys
- ❌ Bypassing zero-knowledge
- ❌ Breaking encryption

---

## Conclusion

Zero-Knowledge Encryption mode provides the highest level of privacy and security available in ObsidianBackup. By ensuring that only you control your encryption keys, it eliminates trust requirements in service providers and protects against server breaches.

However, this security comes with significant responsibility. You must manage your keys carefully, backup regularly, and accept that key loss means permanent data loss.

**Use zero-knowledge mode if:**
- You need maximum privacy
- You handle sensitive data
- You don't trust cloud providers
- You can manage key backups reliably
- You understand the risks and trade-offs

**Don't use zero-knowledge mode if:**
- You frequently forget passwords
- You want convenience over security
- You need account recovery options
- You share devices with others
- You prefer managed security

The choice is yours. Choose wisely. 🔒

---

**Document Version:** 1.0  
**Last Updated:** 2024  
**Author:** ObsidianBackup Security Team  
**License:** GPL-3.0
