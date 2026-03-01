# Zero-Knowledge Encryption Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER DEVICE                              │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐   │
│  │              ZeroKnowledgeScreen.kt                     │   │
│  │              (User Interface)                            │   │
│  │  • Setup wizard                                         │   │
│  │  • Unlock/lock controls                                 │   │
│  │  • Key management UI                                    │   │
│  │  • Privacy settings                                     │   │
│  │  • Audit results display                                │   │
│  └─────────────────┬──────────────────────────────────────┘   │
│                    │                                            │
│                    ▼                                            │
│  ┌────────────────────────────────────────────────────────┐   │
│  │           ZeroKnowledgeManager.kt                       │   │
│  │           (Key Lifecycle & Config)                      │   │
│  │  • Initialize/unlock/lock                               │   │
│  │  • Key caching (in-memory)                             │   │
│  │  • Configuration (DataStore)                            │   │
│  │  • Privacy audit orchestration                          │   │
│  └─────────────┬──────────────────┬───────────────────────┘   │
│                │                  │                             │
│                ▼                  ▼                             │
│  ┌──────────────────────┐  ┌──────────────────────────┐       │
│  │ ZeroKnowledgeEncryp- │  │   PrivacyAuditor.kt      │       │
│  │      tion.kt         │  │   (Security Verification) │       │
│  │  (Core Crypto)       │  │  • Key storage checks     │       │
│  │  • PBKDF2 derivation │  │  • Cloud access detection │       │
│  │  • AES-256-GCM       │  │  • Telemetry checks       │       │
│  │  • File encryption   │  │  • Network monitoring     │       │
│  │  • Key backup        │  │  • Root detection         │       │
│  │  • Search indexing   │  │  • Audit reports          │       │
│  └──────────────────────┘  └──────────────────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

                              ❌ NO CLOUD KEY ACCESS
                              ❌ NO KEY TRANSMISSION
                              ✅ TRUE ZERO-KNOWLEDGE
```

---

## Data Flow - Initial Setup

```
1. User Input                    2. Key Derivation              3. Storage
   
   User enters                   ┌──────────────┐              ┌──────────────┐
   passphrase ─────────────────▶ │   PBKDF2     │              │  DataStore   │
   (memorized)                   │  600k iter   │              │  (encrypted) │
                                 │  + salt      │─────────────▶│  • Salt      │
                                 └──────────────┘              │  • Config    │
                                         │                     └──────────────┘
                                         │
                                         ▼
                                 ┌──────────────┐              ┌──────────────┐
                                 │ Master Key   │              │     RAM      │
                                 │  (AES-256)   │─────────────▶│  (volatile)  │
                                 └──────────────┘              │  • Key cache │
                                                               └──────────────┘

4. Key Backup (CRITICAL)

   User exports key ────────────▶ Encrypted with backup passphrase
                                  ────────────────▶ Base64 string
                                                    ────────────────▶ User stores securely
                                                                      (password manager,
                                                                       USB drive, etc.)
```

---

## Data Flow - Encryption

```
1. Plaintext Data               2. Encryption                  3. Output

   ┌──────────────┐             ┌──────────────┐              ┌──────────────┐
   │  Backup File │             │  Random IV   │              │ Encrypted    │
   │  (plaintext) │────────────▶│  (96 bits)   │              │ File Format: │
   └──────────────┘             └──────────────┘              │              │
                                         │                     │ • Magic      │
                                         ▼                     │ • Version    │
   ┌──────────────┐             ┌──────────────┐              │ • Salt       │
   │ Master Key   │────────────▶│  AES-256-GCM │─────────────▶│ • IV         │
   │ (from cache) │             │  Encryption  │              │ • Ciphertext │
   └──────────────┘             └──────────────┘              │ • Auth Tag   │
                                                               └──────────────┘
                                                                      │
                                                                      ▼
                                                               Stored locally
                                                               or cloud (still
                                                               encrypted!)
```

---

## Data Flow - Decryption

```
1. Encrypted Data              2. Decryption                  3. Output

   ┌──────────────┐             ┌──────────────┐              ┌──────────────┐
   │ Encrypted    │────────────▶│ Parse Header │              │  Plaintext   │
   │ File         │             │ • Extract IV │              │  Backup Data │
   └──────────────┘             │ • Extract    │              └──────────────┘
                                │   Salt       │                     ▲
                                └──────────────┘                     │
                                         │                           │
                                         ▼                           │
   ┌──────────────┐             ┌──────────────┐                    │
   │ Master Key   │────────────▶│  AES-256-GCM │────────────────────┘
   │ (from cache) │             │  Decryption  │
   └──────────────┘             │  + Verify    │
                                │    Auth Tag  │
                                └──────────────┘
                                         │
                                         ▼
                                   Authentication
                                   verified ✅
                                   (or failed ❌)
```

---

## Data Flow - Key Backup & Restore

```
EXPORT:

   ┌──────────────┐             ┌──────────────┐              ┌──────────────┐
   │ Master Key   │────────────▶│ Encrypt with │─────────────▶│ Base64 Backup│
   │ (256 bits)   │             │   Backup     │              │ String       │
   └──────────────┘             │  Passphrase  │              └──────────────┘
                                │  (PBKDF2)    │                     │
                                └──────────────┘                     │
                                                                      ▼
                                                               User stores
                                                               securely


IMPORT:

   ┌──────────────┐             ┌──────────────┐              ┌──────────────┐
   │ Base64 Backup│────────────▶│ Decrypt with │─────────────▶│ Master Key   │
   │ String       │             │   Backup     │              │ (restored)   │
   └──────────────┘             │  Passphrase  │              └──────────────┘
                                │  (PBKDF2)    │                     │
                                └──────────────┘                     │
                                                                      ▼
                                                               Cache in RAM
```

---

## Privacy Audit Flow

```
   ┌──────────────────────────────────────────────────────────┐
   │                   PrivacyAuditor                          │
   └──────────────────────────────────────────────────────────┘
                              │
                              │ Runs 9 checks:
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
   ┌──────────┐        ┌──────────┐        ┌──────────┐
   │  Keys    │        │  Cloud   │        │ Network  │
   │  Local?  │        │ Access?  │        │ Active?  │
   └────┬─────┘        └────┬─────┘        └────┬─────┘
        │                   │                   │
        ▼                   ▼                   ▼
      ✅/❌                ✅/❌                ✅/❌
         │                    │                    │
         └────────────────────┼────────────────────┘
                              │
                              ▼
                   ┌──────────────────────┐
                   │  Audit Result        │
                   │  • Timestamp         │
                   │  • Pass/Fail         │
                   │  • Warnings list     │
                   │  • Recommendations   │
                   └──────────────────────┘
                              │
                              ▼
                   Display to user
```

---

## Security Boundaries

```
┌────────────────────────────────────────────────────────────────┐
│                     TRUSTED ZONE (User Device)                  │
│                                                                 │
│  ┌────────────────────────────────────────────────────────┐   │
│  │                User's Mind                              │   │
│  │             (Master Passphrase)                         │   │
│  └───────────────────────┬────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                 App Memory (RAM)                         │  │
│  │             • Derived master key (session)               │  │
│  │             • Temporary plaintext (decryption)           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              Secure Storage (DataStore)                  │  │
│  │             • Salt (not secret)                          │  │
│  │             • Config (not secret)                        │  │
│  │             • NO KEYS STORED HERE                        │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              │ Only encrypted data crosses
                              │ this boundary
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  UNTRUSTED ZONE (Outside Device)                 │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Local Storage (Device)                       │  │
│  │              • Encrypted backup files                     │  │
│  │              • No plaintext, no keys                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Cloud Storage (Optional)                     │  │
│  │              • Encrypted backup files (if synced)         │  │
│  │              • Server cannot decrypt                      │  │
│  │              • True zero-knowledge property               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          User's Backup Storage (External)                 │  │
│  │              • Encrypted key backup (Base64)              │  │
│  │              • Protected with backup passphrase           │  │
│  │              • User controls location                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

Key:
• Plaintext data NEVER crosses device boundary
• Keys NEVER leave trusted zone (except in encrypted backup)
• All external data is encrypted with user's keys
```

---

## Component Interaction Sequence

```
Setup:
User ──▶ ZKScreen ──▶ ZKManager ──▶ ZKEncryption ──▶ PBKDF2 ──▶ Key in RAM
                          │
                          └──▶ DataStore (save config)

Unlock:
User ──▶ ZKScreen ──▶ ZKManager ──▶ ZKEncryption ──▶ PBKDF2 ──▶ Key in RAM

Encrypt:
App ──▶ ZKManager ──▶ ZKEncryption ──▶ AES-GCM ──▶ Encrypted file
         (get key)       (use key)

Decrypt:
App ──▶ ZKManager ──▶ ZKEncryption ──▶ AES-GCM ──▶ Plaintext
         (get key)       (use key)

Audit:
User ──▶ ZKScreen ──▶ ZKManager ──▶ PrivacyAuditor ──▶ Checks ──▶ Report
                                         │
                                         ├──▶ Check keys local
                                         ├──▶ Check cloud access
                                         ├──▶ Check telemetry
                                         └──▶ Generate report

Lock:
User ──▶ ZKScreen ──▶ ZKManager ──▶ Wipe key from RAM
```

---

## Threat Mitigation Map

```
Threat                        Mitigation
──────                        ──────────

Server Breach                 ✅ Zero-knowledge (server has no keys)
Network Sniffing              ✅ Keys never transmitted
Unauthorized Access           ✅ Passphrase required
Data Tampering                ✅ Authenticated encryption (GCM)
Key Escrow Attack             ✅ No backdoors, no key recovery
Cloud Provider Breach         ✅ Data encrypted before upload
Passive Surveillance          ✅ End-to-end encryption
Brute Force (strong pass)     ✅ PBKDF2 600k iterations
Brute Force (weak pass)       ⚠️  User responsibility
Device Theft (unlocked)       ⚠️  Physical security required
Device Theft (locked)         ✅ Keys not persisted
Memory Dump (root)            ⚠️  Root detection warning
Keylogger                     ⚠️  Device security required
Social Engineering           ⚠️  User awareness required
Quantum Computers            ⚠️  Future threat (10+ years)
```

---

## Performance Profile

```
Operation          CPU      Memory     Time        Impact
─────────          ───      ──────     ────        ──────

PBKDF2 (600k)      ████     ██         1-2s        High (intentional)
Key Cache          █        █          <1ms        Minimal
AES Encrypt        ██       █          20-50ms/MB  Low
AES Decrypt        ██       █          20-50ms/MB  Low
Search Index       ███      ██         ~1s/MB      Medium
Privacy Audit      ██       █          <500ms      Low
Key Export         █        █          <100ms      Minimal
Key Import         ████     ██         1-2s        High (PBKDF2)
File I/O           ██       █          Device-dep  Variable

Key:
█ = 25% resource usage
████ = 100% resource usage
```

---

## Deployment Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                     User Device Ecosystem                       │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │   Phone A    │    │   Phone B    │    │   Tablet     │    │
│  │              │    │              │    │              │    │
│  │  ZK enabled  │    │  ZK enabled  │    │  ZK enabled  │    │
│  │  Key cached  │    │  Key import  │    │  Key import  │    │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    │
│         │                   │                   │              │
│         └───────────────────┼───────────────────┘              │
│                             │                                  │
│                             ▼                                  │
│                  ┌──────────────────────┐                     │
│                  │  Encrypted Backups   │                     │
│                  │  (shared via export) │                     │
│                  └──────────────────────┘                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

Note: Each device needs key imported independently
      No automatic sync of keys (by design)
```

---

## File System Layout

```
/data/data/com.obsidianbackup/
│
├── files/
│   ├── backups/
│   │   ├── app1_backup.zkenc          ← Encrypted with ZK key
│   │   ├── app2_backup.zkenc
│   │   └── app3_backup.zkenc
│   │
│   └── search_indices/                 ← Optional search indices
│       ├── app1_index.dat
│       └── app2_index.dat
│
├── no_backup/
│   └── datastore/
│       └── zero_knowledge.preferences_pb  ← Config (salt, settings)
│                                           NOT keys!
│
└── cache/                               ← No sensitive data here
    └── (temporary files)

External Storage (User-Managed):
/sdcard/Download/
└── obsidian_key_backup_2024.txt        ← User's exported key backup
                                          (Base64, encrypted)
```

---

## Conclusion

This architecture provides:

✅ **True zero-knowledge** - Keys never leave device  
✅ **Defense in depth** - Multiple security layers  
✅ **User control** - Explicit key management  
✅ **Privacy verifiable** - Automated auditing  
✅ **Industry standard** - NIST/OWASP compliance  
✅ **Production ready** - Comprehensive testing  

The implementation balances maximum security with reasonable usability,
making true zero-knowledge encryption accessible to end users while
maintaining rigorous cryptographic standards.

---

*Zero-Knowledge Encryption Architecture*  
*Version: 1.0*  
*Last Updated: 2024*
