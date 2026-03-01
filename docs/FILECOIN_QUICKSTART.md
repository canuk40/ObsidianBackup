# Filecoin/IPFS Decentralized Backup - Quick Start Guide

## 🚀 Quick Start (5 minutes)

### Step 1: Get API Token
1. Go to https://web3.storage
2. Sign up (free, no credit card needed)
3. Create API token
4. Copy token (starts with `eyJ...`)

### Step 2: Configure in App
1. Open ObsidianBackup
2. Settings → Cloud & Sync
3. Select "Decentralized Storage"
4. Paste token
5. Click "Test" then "Save"

### Step 3: Create Backup
```kotlin
// Upload backup to IPFS/Filecoin
val result = filecoinProvider.uploadSnapshot(
    snapshotId = SnapshotId("my_backup"),
    files = myBackupFiles,
    metadata = backupMetadata
)
```

## 💡 Key Concepts

### Content Identifier (CID)
Each file gets a unique hash-based address:
```
CID: bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi
URL: https://dweb.link/ipfs/bafybeigdyrzt...
```

### Why It's Special
- ✅ Can't be censored or blocked
- ✅ No single point of failure
- ✅ Cryptographic verification built-in
- ✅ Permanent storage (Filecoin deals)
- ✅ Ultra-low cost ($0.000001/GB vs $0.02/GB)

## 📊 Cost Comparison

| Storage | Traditional Cloud | Filecoin/IPFS |
|---------|------------------|---------------|
| 100GB   | $20-24/year | ~$0.0001 one-time |
| 1TB     | $200-300/year | ~$0.001 one-time |
| Savings | - | **99.99%+** |

## 🔒 Security Best Practices

1. **Always encrypt before upload**
   ```kotlin
   val encrypted = encrypt(data)
   filecoinProvider.upload(encrypted)
   ```

2. **Store CIDs securely**
   - CIDs are keys to your data
   - Keep catalog encrypted

3. **Rotate API tokens regularly**
   - Generate new token quarterly
   - Revoke old tokens

4. **Use multiple gateways**
   - Provider uses 4+ gateways automatically
   - Ensures high availability

## 🛠️ Common Operations

### Upload Backup
```kotlin
val provider = FilecoinCloudProvider(context, logger, config)
val result = provider.uploadSnapshot(snapshotId, files, metadata)
```

### Download Backup
```kotlin
val result = provider.downloadSnapshot(
    snapshotId = snapshotId,
    destinationDir = restoreDir,
    verifyIntegrity = true
)
```

### List Backups
```kotlin
val result = provider.listSnapshots(
    CloudSnapshotFilter(maxResults = 50)
)
```

### Delete Backup
```kotlin
val result = provider.deleteSnapshot(snapshotId)
// Note: Unpins content, allows garbage collection
```

## 🐛 Troubleshooting

### "Authentication Failed"
- Check token is valid on web3.storage
- Ensure token has "upload" permission
- Generate new token if expired

### "Content Not Found"
- Content may not be propagated yet (wait 30s)
- Try different IPFS gateway
- Check CID is correct

### Slow Speeds
- Provider tries multiple gateways automatically
- Peak hours may be slower
- Consider dedicated IPFS node for production

## 📚 Learn More

- Full docs: `DECENTRALIZED_BACKUP.md`
- Example code: `examples/FilecoinBackupExample.kt`
- Plugin impl: `plugins/builtin/FilecoinCloudProviderPlugin.kt`

## 🌐 Resources

- **IPFS**: https://docs.ipfs.io
- **Filecoin**: https://docs.filecoin.io  
- **web3.storage**: https://docs.web3.storage
- **CID Inspector**: https://cid.ipfs.io

## 🎯 Use Cases

✅ **Perfect For:**
- Privacy-conscious users
- Censorship-resistant backups
- Long-term archival
- Cross-border data sovereignty
- Open-source projects

❌ **Not Ideal For:**
- Real-time sync needs
- Frequently changing data
- < 1MB files (overhead)
- Users needing immediate retrieval

## 💪 Advanced Features

### Custom IPFS Node
```kotlin
val config = FilecoinConfig(
    ipfsGateways = listOf(
        "http://127.0.0.1:8080", // Local node
        "https://dweb.link"       // Fallback
    )
)
```

### Cost Estimation
```kotlin
val cost = provider.getStorageCost(sizeBytes)
println("Cost: ${cost.filAmount} FIL (~$${cost.usdAmount})")
```

### Storage Deals
```kotlin
val config = FilecoinConfig(
    enableFilecoinDeals = true // Automatic Filecoin storage
)
```

## 🎉 Benefits

1. **Censorship Resistance**: No government or corporation can block access
2. **Data Permanence**: Filecoin deals last ~18 months, renewable
3. **Cost Efficiency**: 99.99% cheaper than traditional cloud
4. **No Vendor Lock-in**: Content-addressed, works with any IPFS client
5. **Cryptographic Verification**: Built-in integrity checking via CIDs
6. **Distributed**: Data replicated across global network

## 📞 Support

Issues? Check:
1. Token valid on web3.storage
2. Internet connectivity
3. Logs: `adb logcat | grep FilecoinCloudProvider`
4. Full documentation in `DECENTRALIZED_BACKUP.md`

---

**Start backing up to IPFS/Filecoin today!** 🚀
