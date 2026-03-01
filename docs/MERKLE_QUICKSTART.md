# Merkle Verification Quick Start

## 5-Minute Integration Guide

### Step 1: Inject the Engine

```kotlin
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val merkleEngine: MerkleVerificationEngine
) : ViewModel()
```

### Step 2: Generate Merkle Root During Backup

```kotlin
suspend fun performBackup(files: List<File>, snapshotDir: File) {
    // After backing up files...
    val rootHash = merkleEngine.generateMerkleRoot(files, snapshotDir)
    
    // Store in metadata
    saveMetadata(rootHash)
}
```

### Step 3: Verify Snapshot

```kotlin
suspend fun verifySnapshot(backupId: BackupId) {
    val result = merkleEngine.verifySnapshot(backupId)
    
    if (result.allValid) {
        showSuccess("Backup verified: ${result.filesChecked} files OK")
    } else {
        showError("Corrupted files: ${result.corruptedFiles.joinToString()}")
    }
}
```

## That's It!

The Merkle verification system is now integrated. Files are automatically verified using cryptographic proofs.

## Advanced: Incremental Verification

Verify single files during restore:

```kotlin
suspend fun restoreFile(file: File, snapshotDir: File) {
    if (!merkleEngine.verifyFileIncremental(file, snapshotDir)) {
        throw SecurityException("File verification failed")
    }
    
    // Restore file...
}
```

## Testing Your Integration

```bash
# Run tests
./gradlew :app:testFreeDebugUnitTest --tests "*.MerkleTreeTest"

# Verify compilation
./gradlew :app:compileFreeDebugKotlin
```

## Files Created During Backup

In each snapshot directory:
- `.merkle_metadata.json` - Root hash + metadata
- `.merkle_proofs.json` - Individual file proofs
- `metadata.json` - Updated with `merkleRootHash` field

## Troubleshooting

**Q: Verification failing for old backups?**  
A: System automatically falls back to standard checksum verification for backups without Merkle data.

**Q: Performance concerns?**  
A: Merkle verification is O(log n) - faster than full re-hash for large backups.

**Q: How to disable Merkle verification?**  
A: Simply don't call `generateMerkleRoot()`. Verification will use standard checksums.

## Example: Complete Backup Flow

```kotlin
@Inject lateinit var merkleEngine: MerkleVerificationEngine
@Inject lateinit var catalog: BackupCatalog

suspend fun backupWithVerification(request: BackupRequest) {
    // 1. Backup files
    val files = backupFiles(request.appIds)
    val snapshotDir = getSnapshotDir(snapshotId)
    
    // 2. Calculate checksums
    val checksums = calculateChecksums(files)
    
    // 3. Generate Merkle root
    val merkleRoot = merkleEngine.generateMerkleRootFromChecksums(
        checksums,
        snapshotDir
    )
    
    // 4. Save metadata with Merkle root
    val metadata = BackupMetadata(
        snapshotId = snapshotId,
        timestamp = System.currentTimeMillis(),
        apps = request.appIds,
        checksums = checksums,
        merkleRootHash = merkleRoot  // Added!
        // ... other fields
    )
    
    catalog.saveSnapshot(metadata)
    
    // 5. Verify immediately after backup
    val result = merkleEngine.verifySnapshot(BackupId(snapshotId.value))
    
    if (!result.allValid) {
        throw BackupException("Backup verification failed!")
    }
}
```

## Next Steps

- Read full documentation: `/docs/MERKLE_VERIFICATION.md`
- Review test examples: `/app/src/test/.../verification/`
- Check integration patterns: `/docs/DI_ARCHITECTURE.md`
