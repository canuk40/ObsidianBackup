# Zero-Knowledge Encryption - Quick Reference

## 🔐 What is Zero-Knowledge?

**Your keys. Your data. Nobody else.**

- Keys **NEVER** leave your device
- ObsidianBackup **CANNOT** access your data
- Lost passphrase = Lost data **FOREVER**

---

## ⚡ Quick Start

### 1. Enable (30 seconds)

```
Settings → Encryption → Zero-Knowledge Encryption
→ Read Warning → "I Understand"
→ Set Passphrase (12+ chars)
→ Confirm Passphrase
```

### 2. Backup Key (CRITICAL!)

```
Key Management → Export Key Backup
→ Set Backup Passphrase
→ Save Base64 string to:
   • Password manager (recommended)
   • USB drive (offline)
   • Paper backup (secure location)
```

### 3. Use

```
Unlock → Enter Passphrase → Create Backups
(Automatically encrypted with your key)
```

---

## 🎯 Common Tasks

### Unlock After Restart

```
Zero-Knowledge Settings → Unlock → Enter Passphrase
```

### Lock (Clear Keys from Memory)

```
Zero-Knowledge Settings → Lock
```

### Export Key Backup

```
Key Management → Export Key Backup → Set Backup Passphrase
→ Copy Base64 string → Store securely
```

### Import Key Backup (New Device)

```
Zero-Knowledge Settings → Import Key Backup
→ Paste Base64 string → Enter Backup Passphrase
```

### Run Privacy Audit

```
Privacy Audit → Run Privacy Audit → Review Results
```

### Enable Local-Only Mode

```
Privacy Settings → Local-Only Mode → ON
(Disables all cloud sync)
```

### Disable Search Index

```
Privacy Settings → Searchable Encryption → OFF
(Maximum privacy, no search)
```

---

## 🔒 Security Checklist

- [ ] Strong passphrase (12+ characters)
- [ ] Key backup exported
- [ ] Backup stored in 2+ locations
- [ ] Privacy audit passed (no warnings)
- [ ] Device encryption enabled
- [ ] Lock when not in use
- [ ] Regular backup integrity checks

---

## ⚠️ Critical Warnings

### DO

✅ Use strong, unique passphrase  
✅ Export key backup immediately  
✅ Store backup in multiple locations  
✅ Lock when not in use  
✅ Run privacy audits regularly  

### DON'T

❌ Use weak or common passwords  
❌ Skip key backup export  
❌ Write passphrase on paper  
❌ Share passphrase with anyone  
❌ Reuse passphrase from other services  

### REMEMBER

🔴 **KEY LOSS = DATA LOSS (NO RECOVERY)**

---

## 🛠️ Troubleshooting

### "Unlock failed"
→ Check passphrase (case-sensitive)  
→ Verify caps lock off  
→ Try key backup import if available  

### "Privacy audit warnings"
→ Export key backup if not done  
→ Enable local-only mode for maximum privacy  
→ Check for cloud sync settings  

### "Decryption failed"
→ Ensure zero-knowledge mode unlocked  
→ Verify correct key (try re-unlock)  
→ Check backup file integrity  

### "Key backup import failed"
→ Verify backup string complete (no truncation)  
→ Check backup passphrase correct  
→ Ensure backup not corrupted  

---

## 📊 Technical Specs

| Component | Specification |
|-----------|---------------|
| **Key Derivation** | PBKDF2-HMAC-SHA512 |
| **Iterations** | 600,000 |
| **Encryption** | AES-256-GCM |
| **Key Size** | 256 bits |
| **Salt** | 256 bits (random) |
| **IV** | 96 bits (random per encryption) |
| **Auth Tag** | 128 bits |

---

## 🔗 Key Locations

### ✅ Stored (Encrypted)
- Configuration salt (DataStore)
- Privacy settings
- Audit results

### ✅ Cached (Memory)
- Master key (during session)
- Cleared on lock/app close

### ❌ NEVER Stored
- Master passphrase
- Plaintext keys
- Cloud servers
- ObsidianBackup servers

---

## 💡 Best Practices

### Passphrase

**Good:**
```
correct-horse-battery-staple-2024!
My$3cureP@ssphrase#42
Tr0ub4dor&3-ExtendedVersion
```

**Bad:**
```
password123
qwerty
letmein
```

### Key Backup Storage

**Recommended:**
- Bitwarden (encrypted vault)
- 1Password (secure note)
- KeePass (offline database)
- Encrypted USB drive (air-gapped)
- Paper in safe (for paranoid)

**Not Recommended:**
- Plain text file
- Email to yourself
- Cloud notes (unencrypted)
- Sticky note
- Memory only

---

## 🚀 Advanced Features

### Searchable Encryption

**Pros:**
- Search without decryption
- Fast exact-match queries
- Minimal overhead

**Cons:**
- Term frequency visible
- No fuzzy search
- Exact matches only

**Toggle:**
```
Privacy Settings → Searchable Encryption
```

### Local-Only Mode

**When to use:**
- Maximum paranoia
- Sensitive data
- Air-gapped security
- Distrust cloud providers

**Trade-off:**
- No cloud backup redundancy
- Device loss risk higher
- Manual export required

**Toggle:**
```
Privacy Settings → Local-Only Mode
```

---

## 📞 Support

### We CAN Help With:
- Feature questions
- Setup guidance
- Bug reports
- Privacy audit interpretation

### We CANNOT Help With:
- Password recovery
- Key recovery
- Decryption without keys
- Bypassing security

**Contact:** support@obsidianbackup.com

---

## 🔑 Key Commands (Mental Model)

```bash
# Setup
enable_zk --passphrase "your-strong-passphrase"
export_key --backup-passphrase "backup-passphrase" > key.backup

# Daily Use
unlock --passphrase "your-strong-passphrase"
create_backup --encrypted
lock

# Recovery
import_key --backup-file key.backup --passphrase "backup-passphrase"

# Audit
run_audit --check privacy,security,keys
```

---

## 📈 Performance

| Operation | Time (approx) |
|-----------|---------------|
| Key Derivation | 1-2 seconds |
| Encrypt 1MB | ~20-50ms |
| Decrypt 1MB | ~20-50ms |
| Build Search Index (1MB) | ~1 second |
| Export Key Backup | <100ms |
| Import Key Backup | 1-2 seconds |
| Privacy Audit | <500ms |

*Times vary by device*

---

## 🎓 Learn More

- **Full Documentation:** `ZERO_KNOWLEDGE_MODE.md`
- **Code:** `app/src/main/java/com/obsidianbackup/crypto/`
- **Tests:** `app/src/test/java/com/obsidianbackup/crypto/`

---

**Remember: With great privacy comes great responsibility!** 🕷️

---

*Zero-Knowledge Encryption v1.0*  
*Last Updated: 2024*
