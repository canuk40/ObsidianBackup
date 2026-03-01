# 003. Merkle Tree for Backup Verification

Date: 2024-01-25
Status: Accepted

## Context

Backup integrity verification is critical. We need to:
1. Detect corrupted backups quickly
2. Enable efficient incremental backups
3. Verify large backups without reading entire archive
4. Provide cryptographic proof of integrity

Traditional approaches (CRC32, SHA256 of entire archive) require reading the entire backup for verification, which is slow for large backups.

## Decision

Use Merkle trees (hash trees) for backup verification:

1. Each file is hashed (SHA-256)
2. Hashes are organized in a binary tree
3. Parent nodes are hashes of child nodes
4. Root hash represents entire backup
5. Changed files only affect path to root

## Alternatives Considered

### Simple Checksums (CRC32)
- Fast but not cryptographically secure
- Cannot detect intentional tampering
- Still requires full read for verification

### SHA-256 of Entire Archive
- Cryptographically secure
- Requires reading entire backup
- Slow for large backups
- Cannot verify individual files

### Blockchain
- Overkill for local verification
- Performance overhead
- Unnecessary complexity

## Consequences

### Positive
- Fast partial verification (only changed files)
- Efficient incremental backups
- Cryptographically secure
- Industry standard (used by Git, Bitcoin, etc.)
- Visual tree representation possible
- Tamper detection

### Negative
- Additional storage for tree structure
- Complexity in implementation
- Tree must be updated on changes
- Requires understanding of concept

## Implementation

```kotlin
data class MerkleNode(
    val hash: String,
    val left: MerkleNode?,
    val right: MerkleNode?,
    val file: String?  // Leaf nodes only
)

class MerkleTree {
    fun build(files: List<File>): MerkleNode
    fun verify(root: MerkleNode, files: List<File>): Boolean
    fun diff(oldRoot: MerkleNode, newRoot: MerkleNode): List<String>
}
```

## Performance

- Build tree: O(n log n)
- Verify full backup: O(n)
- Verify single file: O(log n)
- Find changes: O(n)

Where n is the number of files.

## Use Cases

1. **Incremental Backup**: Compare trees to find changed files
2. **Integrity Verification**: Verify without full read
3. **Corruption Detection**: Identify corrupted files
4. **Tamper Detection**: Detect unauthorized modifications
5. **Visual Verification**: Show tree structure to users

## References

- [Merkle Tree (Wikipedia)](https://en.wikipedia.org/wiki/Merkle_tree)
- [Git Internals](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)
- [Certificate Transparency](https://tools.ietf.org/html/rfc6962)
