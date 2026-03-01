# Decentralized Backup with IPFS/Filecoin

## Overview

ObsidianBackup now supports **decentralized, censorship-resistant backup storage** using IPFS (InterPlanetary File System) and Filecoin. This feature provides an alternative to traditional cloud storage, offering enhanced privacy, permanence, and resistance to censorship.

## Table of Contents

1. [Why Decentralized Storage?](#why-decentralized-storage)
2. [Technical Architecture](#technical-architecture)
3. [Setup Guide](#setup-guide)
4. [Features](#features)
5. [Cost Comparison](#cost-comparison)
6. [Security & Privacy](#security--privacy)
7. [API Reference](#api-reference)
8. [Troubleshooting](#troubleshooting)

---

## Why Decentralized Storage?

### Key Benefits

🔐 **Censorship Resistance**
- No central authority can block or remove your backups
- Content-addressed storage ensures data permanence
- Distributed network prevents single points of failure

🌍 **Decentralization**
- Data stored across thousands of nodes worldwide
- No dependence on a single company or service
- Filecoin network provides economic incentives for storage

✅ **Content Integrity**
- Cryptographic CID (Content Identifier) hashes verify data
- Impossible to tamper with stored content
- Built-in verification at every retrieval

💰 **Cost Efficiency**
- Extremely low storage costs compared to traditional cloud
- One-time payment model vs. recurring subscriptions
- Transparent pricing based on actual storage used

🔓 **Data Sovereignty**
- You control your data completely
- No vendor lock-in
- Export/migrate at any time

---

## Technical Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    ObsidianBackup App                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │         FilecoinCloudProvider.kt                   │    │
│  └─────────────────┬──────────────────────────────────┘    │
│                    │                                         │
└────────────────────┼─────────────────────────────────────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │   web3.storage API   │  ← API Gateway
          └──────────┬───────────┘
                     │
         ┏━━━━━━━━━━━┻━━━━━━━━━━━┓
         ▼                        ▼
   ┌──────────┐            ┌──────────┐
   │   IPFS   │            │ Filecoin │
   │ Network  │◄───────────┤ Network  │
   └──────────┘            └──────────┘
   Fast Retrieval          Long-term Storage
```

### Data Flow

1. **Upload Process**
   ```
   Backup Data → Encrypt → Split into chunks → Upload to web3.storage
                                                       ↓
                                              Store on IPFS
                                                       ↓
                                              Pin content
                                                       ↓
                                              Create Filecoin storage deal
                                                       ↓
                                              Generate CID → Store in catalog
   ```

2. **Download Process**
   ```
   CID from catalog → Query IPFS gateways → Download chunks
                                                  ↓
                                            Verify integrity
                                                  ↓
                                            Reassemble data
                                                  ↓
                                            Decrypt → Restore
   ```

### Content Addressing (CID)

Each file gets a unique **Content Identifier (CID)** based on its content:

```kotlin
// Example CID
val cid = "bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi"

// CID Properties:
// - Cryptographic hash of content
// - Self-describing format
// - Immutable identifier
// - Universal across IPFS network
```

The CID acts as both:
- **Storage address**: Where to find the content
- **Integrity proof**: Verifies content hasn't been tampered with

---

## Setup Guide

### Prerequisites

1. **Create web3.storage Account**
   - Visit: https://web3.storage
   - Sign up for a free account
   - Free tier includes generous storage limits

2. **Generate API Token**
   - Log into web3.storage dashboard
   - Navigate to Account → API Tokens
   - Create new token with "Upload" permissions
   - Copy the token (starts with `eyJ...`)

### Configuration Steps

#### Via UI (Recommended)

1. Open ObsidianBackup
2. Navigate to **Settings → Cloud & Sync**
3. Select **Decentralized Storage (IPFS/Filecoin)**
4. Paste your web3.storage API token
5. Click **Test Connection** to verify
6. Click **Save Token**

#### Programmatic Configuration

```kotlin
import com.obsidianbackup.cloud.FilecoinCloudProvider
import com.obsidianbackup.cloud.FilecoinConfig

// Create configuration
val config = FilecoinConfig(
    web3StorageToken = "YOUR_API_TOKEN_HERE",
    ipfsGateways = listOf(
        "https://dweb.link",
        "https://ipfs.io",
        "https://cloudflare-ipfs.com"
    ),
    enableFilecoinDeals = true,
    pinningService = "web3.storage"
)

// Initialize provider
val provider = FilecoinCloudProvider(context, logger, config)

// Test connection
val result = provider.testConnection()
```

### Verification

After configuration, verify your setup:

```kotlin
// Upload test file
val testFile = File("test.txt")
val result = provider.uploadFile(
    localFile = testFile,
    remotePath = "test/test.txt"
)

// Check result
when (result) {
    is CloudResult.Success -> println("✓ Upload successful")
    is CloudResult.Error -> println("✗ Upload failed: ${result.error.message}")
}
```

---

## Features

### 1. Content-Addressed Storage

Every backup is stored using content addressing:

```kotlin
// Upload returns CID
val uploadResult = provider.uploadSnapshot(
    snapshotId = SnapshotId("snapshot_001"),
    files = listOf(cloudFile1, cloudFile2),
    metadata = metadata
)

// CIDs stored in remoteUrls map
val cids = uploadResult.data.remoteUrls
// Example: {"app1.apk" -> "bafybei...", "app2.apk" -> "bafybei..."}
```

**Benefits:**
- Deduplication (identical content = same CID)
- Integrity verification built-in
- Location-independent addressing

### 2. Multi-Gateway Fallback

Automatic failover across IPFS gateways:

```kotlin
private val IPFS_GATEWAYS = listOf(
    "https://dweb.link",          // Primary
    "https://ipfs.io",            // Fallback 1
    "https://cloudflare-ipfs.com", // Fallback 2
    "https://gateway.pinata.cloud" // Fallback 3
)
```

If one gateway is slow or unavailable, automatically tries the next.

### 3. Pinning & Persistence

Content is "pinned" to prevent garbage collection:

```kotlin
// Automatic pinning on upload
private suspend fun pinContent(cid: String) {
    // web3.storage automatically pins uploaded content
    // Content remains available on IPFS network
    logger.i(TAG, "Content $cid is pinned")
}
```

**Pinning ensures:**
- Long-term availability
- Protection from garbage collection
- Integration with Filecoin storage deals

### 4. Storage Deal Management

Filecoin storage deals provide redundancy:

```kotlin
val config = FilecoinConfig(
    enableFilecoinDeals = true  // Enable automatic Filecoin deals
)
```

**Filecoin deals:**
- Cryptographically proven storage
- Economic incentives for storage providers
- Automatic renewal mechanisms
- Redundant copies across network

### 5. Cost Estimation

Real-time storage cost calculation:

```kotlin
suspend fun getStorageCost(sizeBytes: Long): CloudResult<StorageCost> {
    val filPerGB = 0.0000001 // Current Filecoin rate
    val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
    val estimatedFil = sizeGB * filPerGB
    
    return CloudResult.Success(
        StorageCost(
            filAmount = estimatedFil,
            usdAmount = estimatedFil * filToUsdRate,
            duration = "permanent"
        )
    )
}
```

### 6. Snapshot Verification

Built-in integrity checking:

```kotlin
override suspend fun downloadSnapshot(
    snapshotId: SnapshotId,
    destinationDir: File,
    verifyIntegrity: Boolean = true
): CloudResult<CloudDownloadSummary> {
    // Download files
    // ...
    
    // Verify using Merkle tree root hash
    val verificationResult = verifySnapshotIntegrity(
        destinationDir,
        snapshotInfo.metadata
    )
    
    // Check CID matches
    // Verify file count
    // Validate checksums
}
```

### 7. CAR (Content Addressable aRchive) Export

Snapshots packaged as CAR files:

```kotlin
private fun createSnapshotCAR(
    snapshotId: SnapshotId,
    files: List<CloudFile>,
    metadata: CloudSnapshotMetadata,
    cidMapping: Map<String, String>
): File {
    val carData = mapOf(
        "snapshot_id" to snapshotId.value,
        "timestamp" to metadata.timestamp,
        "merkle_root" to metadata.merkleRootHash,
        "cids" to cidMapping
    )
    // Package as CAR format for IPFS/Filecoin
}
```

**CAR benefits:**
- Single-file snapshot representation
- Efficient transfer and storage
- Standard IPFS/Filecoin format

---

## Cost Comparison

### Traditional Cloud Storage

| Provider | Cost per GB/month | 100GB/year | 1TB/year |
|----------|------------------|------------|----------|
| Google Drive | $0.020 | $24.00 | $240.00 |
| Dropbox | $0.024 | $28.80 | $288.00 |
| AWS S3 | $0.023 | $27.60 | $276.00 |
| Azure Blob | $0.018 | $21.60 | $216.00 |

### Filecoin/IPFS

| Service | Cost per GB | 100GB | 1TB |
|---------|-------------|-------|-----|
| web3.storage | **FREE** tier | $0.00 | $0.00 |
| Filecoin Network | ~$0.000001 (one-time) | ~$0.0001 | ~$0.001 |

**Key Differences:**

1. **Payment Model**
   - Traditional: Recurring monthly subscription
   - Filecoin: One-time storage deal (lasts ~1.5 years, renewable)

2. **Total Cost of Ownership (3 years)**
   - Traditional: $648 - $864 (100GB)
   - Filecoin: ~$0.0003 (100GB)
   - **Savings: 99.99%+**

3. **Free Tier**
   - web3.storage offers generous free storage
   - No credit card required
   - Suitable for most personal backup needs

---

## Security & Privacy

### Encryption

**End-to-End Encryption (Recommended)**

```kotlin
// Encrypt before uploading
val encrypted = encryptionManager.encrypt(backupData)
val result = filecoinProvider.uploadSnapshot(encrypted, ...)

// Decrypt after downloading
val downloaded = filecoinProvider.downloadSnapshot(...)
val decrypted = encryptionManager.decrypt(downloaded)
```

**Privacy Guarantees:**
- All encryption happens on-device
- Private keys never leave your device
- IPFS only stores encrypted ciphertext
- Even storage providers can't read your data

### Content Privacy

**Public vs. Private Content:**

- **IPFS is content-addressed but not necessarily public**
- Without the CID, content is practically undiscoverable
- CIDs are stored in your encrypted catalog
- Only you have access to your backup CIDs

**Best Practices:**
1. Always encrypt sensitive data before upload
2. Store CIDs securely (encrypted catalog)
3. Use strong API token protection
4. Consider additional encryption layers for ultra-sensitive data

### Threat Model

**What IPFS/Filecoin Protects Against:**

✅ Censorship by governments or corporations  
✅ Service provider going out of business  
✅ Account suspension or termination  
✅ Geographic restrictions  
✅ Data center failures  
✅ DDoS attacks on single provider  

**What It Doesn't Protect Against:**

❌ Malware on your device  
❌ Weak encryption keys  
❌ Stolen API tokens (rotate regularly)  
❌ Physical device seizure (use encryption)  

---

## API Reference

### FilecoinCloudProvider

```kotlin
class FilecoinCloudProvider(
    private val context: Context,
    private val logger: ObsidianLogger,
    private val config: FilecoinConfig
) : CloudProvider
```

#### Configuration

```kotlin
data class FilecoinConfig(
    val web3StorageToken: String,
    val ipfsGateways: List<String> = defaultGateways,
    val enableFilecoinDeals: Boolean = true,
    val pinningService: String = "web3.storage"
)
```

#### Core Methods

**Test Connection**
```kotlin
suspend fun testConnection(): CloudResult<ConnectionInfo>
```

**Upload Snapshot**
```kotlin
suspend fun uploadSnapshot(
    snapshotId: SnapshotId,
    files: List<CloudFile>,
    metadata: CloudSnapshotMetadata
): CloudResult<CloudUploadSummary>
```

**Download Snapshot**
```kotlin
suspend fun downloadSnapshot(
    snapshotId: SnapshotId,
    destinationDir: File,
    verifyIntegrity: Boolean = true
): CloudResult<CloudDownloadSummary>
```

**List Snapshots**
```kotlin
suspend fun listSnapshots(
    filter: CloudSnapshotFilter = CloudSnapshotFilter()
): CloudResult<List<CloudSnapshotInfo>>
```

**Delete Snapshot**
```kotlin
suspend fun deleteSnapshot(
    snapshotId: SnapshotId
): CloudResult<Unit>
```

**Get Storage Cost**
```kotlin
suspend fun getStorageCost(
    sizeBytes: Long
): CloudResult<StorageCost>
```

#### Data Classes

**Storage Cost**
```kotlin
data class StorageCost(
    val filAmount: Double,        // Cost in FIL tokens
    val usdAmount: Double,        // Equivalent USD
    val duration: String,         // "permanent" or time period
    val lastUpdated: Long         // Timestamp
)
```

**Cloud Result**
```kotlin
sealed class CloudResult<out T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error(val error: CloudError) : CloudResult<Nothing>()
}
```

---

## Troubleshooting

### Common Issues

#### 1. "Authentication Failed" Error

**Problem:** Invalid or expired API token

**Solution:**
```bash
# Verify token is correct
# Check web3.storage dashboard for token status
# Generate new token if needed
# Update configuration
```

#### 2. Slow Upload/Download Speeds

**Problem:** Gateway congestion or network issues

**Solution:**
- Provider automatically tries multiple gateways
- Check your internet connection
- Try during off-peak hours
- Consider using dedicated IPFS node (advanced)

#### 3. "Content Not Found" Error

**Problem:** CID not propagated to network yet

**Solution:**
```kotlin
// Add retry logic with exponential backoff
suspend fun downloadWithRetry(cid: String, maxRetries: Int = 3) {
    repeat(maxRetries) { attempt ->
        try {
            return downloadFromIPFS(cid)
        } catch (e: Exception) {
            if (attempt < maxRetries - 1) {
                delay(1000L * (attempt + 1))  // Exponential backoff
            }
        }
    }
    throw Exception("Content not found after $maxRetries attempts")
}
```

#### 4. "Quota Exceeded" Error

**Problem:** Exceeded web3.storage free tier limits

**Solution:**
- Check usage at web3.storage dashboard
- Upgrade to paid plan if needed
- Clean up old/unused backups
- Consider running your own IPFS node

#### 5. Gateway Timeout

**Problem:** All IPFS gateways timing out

**Solution:**
```kotlin
// Increase timeout values
val connection = (url.openConnection() as HttpURLConnection).apply {
    connectTimeout = 30000  // 30 seconds
    readTimeout = 60000     // 60 seconds
}
```

### Debug Mode

Enable verbose logging:

```kotlin
// In CloudModule or provider initialization
val logger = ObsidianLogger(context).apply {
    setLogLevel(LogLevel.DEBUG)
}

val provider = FilecoinCloudProvider(context, logger, config)
```

Check logs for detailed error information:
```bash
adb logcat | grep FilecoinCloudProvider
```

### Network Diagnostics

Test IPFS gateway connectivity:

```bash
# Test primary gateway
curl -I https://dweb.link/ipfs/QmExample

# Test fallback gateways
curl -I https://ipfs.io/ipfs/QmExample
curl -I https://cloudflare-ipfs.com/ipfs/QmExample
```

---

## Advanced Usage

### Running Your Own IPFS Node

For maximum control and privacy, run a local IPFS node:

**Note:** Currently experimental on Android

```kotlin
// Configuration for local node
val config = FilecoinConfig(
    web3StorageToken = token,
    ipfsGateways = listOf(
        "http://127.0.0.1:8080",  // Local node
        "https://dweb.link"        // Fallback
    )
)
```

**Benefits:**
- No dependence on public gateways
- Faster retrieval
- Full control over pinned content
- Contribute to IPFS network

**Resources:**
- IPFS Android Library: https://github.com/ipfs-shipyard/gomobile-ipfs
- IPFS Documentation: https://docs.ipfs.io

### Custom Pinning Services

Integrate additional pinning services:

```kotlin
// Example: Pinata pinning service
suspend fun pinToPinata(cid: String) {
    val url = URL("https://api.pinata.cloud/pinning/pinByHash")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        setRequestProperty("pinata_api_key", PINATA_KEY)
        setRequestProperty("pinata_secret_api_key", PINATA_SECRET)
        doOutput = true
    }
    
    val body = """{"hashToPin": "$cid"}"""
    connection.outputStream.write(body.toByteArray())
    // Handle response
}
```

### Filecoin Storage Deal Monitoring

Monitor your storage deals:

```kotlin
suspend fun getStorageDeals(cid: String): List<StorageDeal> {
    // Query Filecoin network for storage deals
    // Check deal status, expiration, etc.
}

data class StorageDeal(
    val dealId: Long,
    val provider: String,
    val startEpoch: Long,
    val endEpoch: Long,
    val verified: Boolean
)
```

---

## Future Enhancements

### Planned Features

1. **IPFS Cluster Integration**
   - Collaborative pinning
   - Automatic replication

2. **Filecoin Plus Support**
   - Verified deals for 10x storage power
   - Datacap allocation

3. **NFT.Storage Integration**
   - Long-term preservation guarantees
   - NFT-backed storage proofs

4. **Local IPFS Node**
   - Native Android IPFS implementation
   - Offline-first capabilities

5. **Multi-Provider Redundancy**
   - Automatic cross-provider replication
   - Intelligent gateway selection

6. **Smart Contract Integration**
   - Automated storage deal renewal
   - On-chain backup registry

---

## Resources

### Documentation

- **IPFS**: https://docs.ipfs.io
- **Filecoin**: https://docs.filecoin.io
- **web3.storage**: https://docs.web3.storage

### Community

- **IPFS Forum**: https://discuss.ipfs.io
- **Filecoin Slack**: https://filecoin.io/slack

### Tools

- **IPFS Desktop**: https://docs.ipfs.io/install/ipfs-desktop
- **Filecoin Lotus**: https://lotus.filecoin.io
- **CID Inspector**: https://cid.ipfs.io

---

## License

This implementation follows ObsidianBackup's licensing terms. IPFS and Filecoin are open-source protocols with permissive licenses.

---

## Support

For issues or questions:
1. Check this documentation
2. Review troubleshooting section
3. Search existing issues on GitHub
4. Open new issue with logs and error messages

---

**Last Updated:** 2024
**Version:** 1.0.0
**Author:** ObsidianBackup Team
