# 🌐 IPFS/Filecoin Decentralized Backup for ObsidianBackup

## Overview

ObsidianBackup now supports **decentralized, censorship-resistant backup storage** using IPFS and Filecoin. This revolutionary feature provides an alternative to traditional cloud storage, offering unprecedented privacy, permanence, and cost efficiency.

---

## 🎯 Why Choose Decentralized Backup?

### For Privacy Advocates
- ✅ **Censorship-Resistant**: No government or corporation can block your backups
- ✅ **No Surveillance**: Content-addressed storage, not user-tracked
- ✅ **Data Sovereignty**: You control your data completely

### For Cost-Conscious Users
- 💰 **99.99% Cheaper**: $0.000001/GB vs $0.02/GB traditional cloud
- 💰 **No Recurring Fees**: One-time storage deals, not monthly subscriptions
- 💰 **Free Tier**: web3.storage offers generous free storage

### For Security-Minded Users
- 🔒 **Cryptographic Verification**: Built-in content integrity via CIDs
- 🔒 **Immutable Storage**: Content cannot be tampered with
- 🔒 **Multi-Gateway Redundancy**: No single point of failure

---

## 🚀 Quick Start (5 Minutes)

### 1. Get Free API Token
```
Visit: https://web3.storage
Sign up (no credit card needed)
Create API token
Copy token (starts with "eyJ...")
```

### 2. Configure in App
```
Open ObsidianBackup
→ Settings 
→ Cloud & Sync
→ Decentralized Storage (IPFS/Filecoin)
→ Paste token
→ Test Connection
→ Save
```

### 3. Create Backup
Your next backup will be stored on IPFS/Filecoin!
- Data stored across distributed network
- Each file gets unique CID (Content Identifier)
- Automatic Filecoin storage deals created
- Multi-gateway retrieval for reliability

---

## 💡 How It Works

### Content-Addressed Storage
Traditional cloud: `storage.com/user123/backup.tar`
Decentralized: `bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi`

The CID (Content Identifier) is a cryptographic hash of your data:
- **Unique**: No two different files have the same CID
- **Verifiable**: CID proves content hasn't been tampered with
- **Universal**: Same CID works on any IPFS gateway worldwide

### Architecture
```
Your Device
    ↓
web3.storage API
    ↓
IPFS Network ←→ Filecoin Network
(fast retrieval)  (long-term storage)
    ↓
Multiple Public Gateways
(censorship-resistant access)
```

---

## 💰 Cost Comparison

### Traditional Cloud (annual)
| Provider | 100GB | 1TB |
|----------|-------|-----|
| Google Drive | $24 | $240 |
| Dropbox | $29 | $290 |
| AWS S3 | $28 | $276 |

### Filecoin/IPFS (one-time)
| Service | 100GB | 1TB |
|---------|-------|-----|
| web3.storage | **FREE** | **FREE** |
| Filecoin deals | $0.0001 | $0.001 |

**Savings: 99.99%+**

---

## 🔒 Security Features

### End-to-End Encryption
```kotlin
// Encrypt before upload (recommended)
val encrypted = encrypt(backupData)
filecoinProvider.uploadSnapshot(encrypted, ...)

// Only you have the decryption keys
// IPFS stores only encrypted ciphertext
```

### Content Integrity
```kotlin
// Every download is verified
val result = provider.downloadSnapshot(
    snapshotId = id,
    verifyIntegrity = true  // Automatic CID verification
)
```

### Multi-Gateway Redundancy
- Automatic failover across 4+ IPFS gateways
- No single point of failure
- Global accessibility

---

## 📊 Feature Comparison

| Feature | Traditional Cloud | Filecoin/IPFS |
|---------|------------------|---------------|
| Censorship Resistant | ❌ | ✅ |
| No Vendor Lock-in | ❌ | ✅ |
| Cryptographic Verification | ⚠️ | ✅ |
| Content-Addressed | ❌ | ✅ |
| Permanent Storage | ❌ | ✅ |
| Cost per GB | $0.02/mo | $0.000001 |
| Monthly Fees | ✅ | ❌ |
| Geographic Restrictions | Sometimes | Never |

---

## 🎨 Screenshots

### Configuration Screen
- Clean, modern Material 3 design
- Educational "Why Decentralized?" section
- Real-time connection testing
- Storage cost estimator
- Help text and troubleshooting

### Benefits Showcase
- Censorship Resistance
- No Single Point of Failure
- Content Integrity
- Permanent Storage

---

## 📚 Documentation

### Quick References
- **5-Minute Setup**: `FILECOIN_QUICKSTART.md`
- **Complete Guide**: `DECENTRALIZED_BACKUP.md`
- **Implementation**: `FILECOIN_IMPLEMENTATION_SUMMARY.md`

### Code Examples
- **Basic Usage**: `examples/FilecoinBackupExample.kt`
- **Plugin Integration**: `plugins/builtin/FilecoinCloudProviderPlugin.kt`
- **Provider Implementation**: `cloud/FilecoinCloudProvider.kt`

---

## 🛠️ Technical Details

### Implementation
- **Provider**: FilecoinCloudProvider.kt (~900 lines)
- **UI**: FilecoinConfigScreen.kt (~450 lines)
- **Plugin**: FilecoinCloudProviderPlugin.kt (~250 lines)
- **Tests**: FilecoinCloudProviderTest.kt (~200 lines)

### Technologies
- **IPFS**: InterPlanetary File System
- **Filecoin**: Decentralized storage network
- **web3.storage**: Easy web3 storage API
- **CID**: Content Identifier (content-based addressing)

### API Integration
- HTTP-based web3.storage API
- Bearer token authentication
- Multi-gateway retrieval
- Automatic pinning service

---

## 🌟 Use Cases

### Perfect For:
- ✅ Privacy advocates
- ✅ Journalists and activists
- ✅ International users
- ✅ Open-source projects
- ✅ Long-term archival
- ✅ Cost-conscious users

### Not Ideal For:
- ❌ Real-time sync needs
- ❌ Frequently changing data
- ❌ Very small files (<1MB)
- ❌ Users needing immediate retrieval guarantees

---

## 🔧 Configuration Options

### Basic Configuration
```kotlin
val config = FilecoinConfig(
    web3StorageToken = "your_token_here",
    ipfsGateways = listOf(
        "https://dweb.link",
        "https://ipfs.io"
    ),
    enableFilecoinDeals = true
)
```

### Advanced Options
- Custom IPFS gateways
- Local IPFS node support
- Pinning service selection
- Storage deal parameters

---

## 🐛 Troubleshooting

### Connection Issues
**Problem**: "Authentication Failed"
**Solution**: Check token at web3.storage dashboard

**Problem**: "Content Not Found"
**Solution**: Wait 30s for IPFS propagation, provider tries multiple gateways

**Problem**: Slow speeds
**Solution**: Provider automatically tries 4+ gateways for best performance

### Need Help?
1. Check `DECENTRALIZED_BACKUP.md` troubleshooting section
2. Review logs: `adb logcat | grep FilecoinCloudProvider`
3. Test connection in app settings
4. Verify token at web3.storage

---

## 🚧 Future Enhancements

### Planned Features
- [ ] Native Android IPFS node
- [ ] IPFS Cluster integration
- [ ] Filecoin Plus support
- [ ] NFT.Storage integration
- [ ] Smart contract automation
- [ ] Multi-provider redundancy

### Community Contributions Welcome
- Additional gateway support
- Local node improvements
- UI enhancements
- Documentation translations

---

## 📞 Support & Resources

### Documentation
- Setup: `FILECOIN_QUICKSTART.md`
- Complete guide: `DECENTRALIZED_BACKUP.md`
- API docs: `DECENTRALIZED_BACKUP.md#api-reference`

### External Resources
- **IPFS**: https://docs.ipfs.io
- **Filecoin**: https://docs.filecoin.io
- **web3.storage**: https://docs.web3.storage
- **CID Inspector**: https://cid.ipfs.io

### Community
- ObsidianBackup GitHub Issues
- IPFS Forum: https://discuss.ipfs.io
- Filecoin Slack: https://filecoin.io/slack

---

## 🎉 Why This Matters

### Data Freedom
Traditional cloud storage puts your data at the mercy of companies and governments. One account suspension, one server failure, one policy change - and your backups are gone.

Decentralized storage gives you **true data sovereignty**:
- No company can delete your backups
- No government can block access
- No service can go out of business and take your data
- No subscription fees can force you to delete old backups

### The Future of Storage
IPFS and Filecoin represent the future of data storage:
- **Protocol-based**: Like email, not like Gmail
- **Community-owned**: No single company controls it
- **Economically sustainable**: Market-driven pricing
- **Technically superior**: Content addressing, built-in verification

### Privacy as Default
In a world of increasing surveillance and data breaches, decentralized storage offers:
- Content addressing (not user tracking)
- End-to-end encryption
- No central database of users
- Censorship-resistant access

---

## ✅ Implementation Status

All features implemented and documented:
- [x] FilecoinCloudProvider with web3.storage API
- [x] IPFS gateway fallback mechanism  
- [x] Content-addressed storage (CID)
- [x] Filecoin storage deal management
- [x] Cost calculator and pricing display
- [x] UI configuration screen
- [x] Pinning service integration
- [x] Comprehensive documentation
- [x] Code examples and tests

---

## 🙏 Acknowledgments

Built on open protocols:
- **IPFS**: Protocol Labs
- **Filecoin**: Protocol Labs  
- **web3.storage**: Protocol Labs
- **Android**: Google
- **Kotlin**: JetBrains

---

## 📄 License

This implementation follows ObsidianBackup's licensing terms. IPFS and Filecoin are open-source protocols.

---

**Start backing up to the decentralized web today!** 🚀

For questions, issues, or contributions, see the documentation in:
- `DECENTRALIZED_BACKUP.md` - Complete technical guide
- `FILECOIN_QUICKSTART.md` - Quick start guide
- `FILECOIN_IMPLEMENTATION_SUMMARY.md` - Implementation details
