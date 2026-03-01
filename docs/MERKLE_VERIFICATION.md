# Merkle Tree Verification System

## Overview

The Merkle Tree Verification System provides cryptographic integrity verification for ObsidianBackup snapshots. This enterprise-grade feature ensures that backup data has not been tampered with or corrupted using cryptographic proofs.

## Architecture

### Components

1. **MerkleTree.kt** - Core Merkle tree data structure
   - Builds Merkle trees from files or hashes
   - Generates cryptographic proofs for individual files
   - Verifies proofs against root hash
   - Thread-safe implementation using Mutex

2. **MerkleProof.kt** - Data classes for proofs and metadata
   - `MerkleProof`: Contains proof path for file verification
   - `MerkleTreeMetadata`: Stores tree metadata (root hash, leaf count, etc.)
   - Fully serializable for persistence

3. **MerkleVerificationEngine.kt** - Integration with backup system
   - Implements `BackupEngine` interface
   - Generates Merkle roots during backup operations
   - Performs incremental verification without re-hashing entire snapshot
   - Falls back to standard checksum verification if Merkle data unavailable

4. **ChecksumVerifier.kt** - Enhanced with Merkle verification
   - Primary verification using SHA-256 checksums
   - Secondary verification using Merkle proofs
   - Dual-layer integrity checking

## How It Works

### 1. Backup Phase

When a backup is created:

```kotlin
val files = listOf(file1, file2, file3, file4)
val rootHash = merkleVerificationEngine.generateMerkleRoot(files, snapshotDir)
```

This creates:
- `.merkle_metadata.json` - Contains root hash and tree metadata
- `.merkle_proofs.json` - Contains individual file proofs

The root hash is also stored in `BackupMetadata.merkleRootHash`.

### 2. Verification Phase

#### Full Verification

```kotlin
val result = merkleVerificationEngine.verifySnapshot(backupId)
// Verifies all files using Merkle proofs
```

#### Incremental Verification

```kotlin
val isValid = merkleVerificationEngine.verifyFileIncremental(file, snapshotDir)
// Verifies single file without re-reading entire snapshot
```

### 3. Merkle Tree Structure

```
         Root Hash
        /         \
      H(AB)      H(CD)
     /    \      /    \
   H(A)  H(B)  H(C)  H(D)
    |     |     |     |
  File1 File2 File3 File4
```

Each file proof contains:
- File hash (leaf node)
- Sibling hashes (proof path to root)
- Leaf index (position in tree)
- Root hash (for verification)

## Usage Examples

### Generate Merkle Root During Backup

```kotlin
@Inject lateinit var merkleEngine: MerkleVerificationEngine

suspend fun backupFiles(files: List<File>, snapshotDir: File) {
    // Perform backup...
    
    // Generate Merkle root
    val rootHash = merkleEngine.generateMerkleRoot(files, snapshotDir)
    
    // Store in metadata
    val metadata = BackupMetadata(
        // ... other fields
        merkleRootHash = rootHash
    )
    catalog.saveSnapshot(metadata)
}
```

### Verify Entire Snapshot

```kotlin
@Inject lateinit var merkleEngine: MerkleVerificationEngine

suspend fun verifyBackup(backupId: BackupId) {
    val result = merkleEngine.verifySnapshot(backupId)
    
    if (result.allValid) {
        println("All ${result.filesChecked} files verified successfully")
    } else {
        println("Verification failed!")
        result.corruptedFiles.forEach { file ->
            println("Corrupted: $file")
        }
    }
}
```

### Incremental File Verification

```kotlin
@Inject lateinit var merkleEngine: MerkleVerificationEngine

suspend fun restoreFile(file: File, snapshotDir: File) {
    // Verify before restoring
    if (merkleEngine.verifyFileIncremental(file, snapshotDir)) {
        // File is valid, proceed with restore
        restoreFileToDevice(file)
    } else {
        throw SecurityException("File failed Merkle verification: ${file.name}")
    }
}
```

### Generate from Existing Checksums

If you already have checksums calculated:

```kotlin
val checksums = mapOf(
    "file1.txt" to "abc123...",
    "file2.txt" to "def456...",
    "file3.txt" to "ghi789..."
)

val rootHash = merkleEngine.generateMerkleRootFromChecksums(checksums, snapshotDir)
```

## Benefits

### Security

1. **Tamper Detection**: Any modification to a file invalidates the Merkle proof
2. **Cryptographic Integrity**: Uses SHA-256 for all hash operations
3. **Efficient Verification**: Verify single files without re-hashing entire backup
4. **Root Hash Storage**: Single hash in metadata represents entire snapshot integrity

### Performance

1. **Incremental Verification**: O(log n) complexity for single file verification
2. **Parallel-Safe**: Thread-safe implementation using Mutex
3. **No Re-hashing**: Proofs stored allow verification without file I/O
4. **Scalable**: Handles large file sets efficiently

### Enterprise Features

1. **Audit Trail**: Merkle metadata includes timestamp and algorithm
2. **Proof Persistence**: Individual file proofs stored for forensic analysis
3. **Backward Compatible**: Falls back to checksum verification for old backups
4. **Standard Algorithms**: Uses industry-standard SHA-256

## Integration Points

### BackupMetadata

```kotlin
data class BackupMetadata(
    // ... existing fields
    val merkleRootHash: String? = null  // Added for Merkle verification
)
```

### SnapshotEntity (Room Database)

```kotlin
@Entity(tableName = "snapshots")
data class SnapshotEntity(
    // ... existing fields
    val merkleRootHash: String? = null  // Added field
)
```

### Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object VerificationModule {
    @Provides @Singleton
    fun provideMerkleTree(): MerkleTree = MerkleTree()
    
    @Provides @Singleton
    fun provideMerkleVerificationEngine(
        merkleTree: MerkleTree,
        checksumVerifier: ChecksumVerifier,
        catalog: BackupCatalog
    ): MerkleVerificationEngine = 
        MerkleVerificationEngine(merkleTree, checksumVerifier, catalog)
}
```

## Testing

Comprehensive test coverage includes:

- `MerkleTreeTest`: Core Merkle tree operations
  - Tree building from files and hashes
  - Proof generation and verification
  - Tamper detection
  - Thread safety

- `MerkleVerificationEngineTest`: Engine integration
  - Snapshot verification
  - Incremental file verification
  - Fallback to checksum verification
  - Corrupted file detection

Run tests:

```bash
./gradlew :app:testFreeDebugUnitTest --tests "com.obsidianbackup.verification.*"
```

## File Formats

### .merkle_metadata.json

```json
{
  "rootHash": "a1b2c3d4e5f6...",
  "leafCount": 42,
  "algorithm": "SHA-256",
  "timestamp": 1707523200000
}
```

### .merkle_proofs.json

```json
{
  "file1.txt": {
    "fileHash": "abc123...",
    "filePath": "file1.txt",
    "leafIndex": 0,
    "siblings": ["def456...", "ghi789..."],
    "rootHash": "a1b2c3d4e5f6..."
  },
  "file2.txt": {
    "fileHash": "def456...",
    "filePath": "file2.txt",
    "leafIndex": 1,
    "siblings": ["abc123...", "ghi789..."],
    "rootHash": "a1b2c3d4e5f6..."
  }
}
```

## Performance Characteristics

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Build tree | O(n log n) | O(n) |
| Generate proof | O(log n) | O(log n) |
| Verify proof | O(log n) | O(1) |
| Verify file | O(log n) | O(1) |

Where n = number of files in backup

## Security Considerations

1. **Hash Algorithm**: Uses SHA-256 (256-bit security)
2. **Collision Resistance**: Cryptographically secure against collisions
3. **Proof Storage**: Proofs stored in hidden files (prefixed with `.`)
4. **Atomic Operations**: All tree operations protected by mutex
5. **No Sensitive Data**: Only hashes stored, never actual file contents

## Future Enhancements

1. **Streaming Verification**: Verify files during download/transfer
2. **Delta Verification**: Efficient verification for incremental backups
3. **Multi-Algorithm Support**: Add SHA-512, BLAKE3 options
4. **Proof Compression**: Reduce storage size of proof files
5. **Cloud Integration**: Verify cloud-stored files without download

## References

- [Merkle Tree (Wikipedia)](https://en.wikipedia.org/wiki/Merkle_tree)
- [NIST SHA-256 Specification](https://csrc.nist.gov/publications/detail/fips/180/4/final)
- ObsidianBackup Architecture Docs: `/docs/DI_ARCHITECTURE.md`
