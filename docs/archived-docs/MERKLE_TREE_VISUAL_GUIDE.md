# Merkle Tree Visual Reference

## Quick Reference Diagram

### 1. Two Files (Simplest Tree)

```
Files: [file1.txt, file2.txt]
Checksums: [H1, H2]

        ROOT
      H(H1||H2)
       /     \
      /       \
    H1        H2
   (file1)  (file2)
```

**Code:**
```kotlin
val files = listOf(
    CloudFile(file1, "remote/file1", "H1", 1024),
    CloudFile(file2, "remote/file2", "H2", 2048)
)
val root = calculateMerkleRoot(files)
// root = SHA-256(H1 || H2)
```

---

### 2. Four Files (Perfect Binary Tree)

```
Files: [A, B, C, D]
Checksums: [H(A), H(B), H(C), H(D)]

              ROOT
           H(AB||CD)
           /       \
          /         \
      H(A||B)      H(C||D)
       /   \        /   \
      /     \      /     \
    H(A)  H(B)  H(C)   H(D)
    (A)   (B)   (C)    (D)

Level 2 (Root):  1 node
Level 1:         2 nodes
Level 0 (Leaf):  4 nodes

Tree Height: log₂(4) = 2
```

---

### 3. Three Files (Odd Number - Duplication)

```
Files: [A, B, C]
Checksums: [H(A), H(B), H(C)]

              ROOT
           H(AB||CC)
           /       \
          /         \
      H(A||B)      H(C||C)  ← C duplicated
       /   \        /   \
      /     \      /     \
    H(A)  H(B)  H(C)   H(C)
    (A)   (B)   (C)    (C)

Level 2 (Root):  1 node
Level 1:         2 nodes
Level 0 (Leaf):  4 nodes (3 unique + 1 duplicate)
```

**Why Duplicate?**
- Maintains binary tree structure
- Standard Bitcoin/blockchain approach
- Enables efficient O(log n) proofs

---

### 4. Seven Files (Larger Odd Example)

```
Files: [A, B, C, D, E, F, G]

                    ROOT
                 H(ABCD||EFG)
                 /           \
                /             \
           H(AB||CD)       H(EF||GG)  ← G duplicated
           /       \         /     \
          /         \       /       \
      H(A||B)    H(C||D) H(E||F)  H(G||G)
       /   \      /   \   /   \    /   \
      A   B     C   D   E   F   G     G

Level 3 (Root):  1 node
Level 2:         2 nodes
Level 1:         4 nodes
Level 0 (Leaf):  8 nodes (7 unique + 1 duplicate)

Tree Height: log₂(8) = 3
```

---

### 5. Algorithm Flow

```
Input: List of files with checksums

Step 1: Initialize leaf level
[H(file1), H(file2), H(file3), H(file4)]
        ↓
Step 2: Build parent level (pair and hash)
[H(H1||H2), H(H3||H4)]
        ↓
Step 3: Build next level
[H(H12||H34)]
        ↓
Step 4: Root reached (single node)
Return: Root hash as hex string
```

---

### 6. Memory Usage Per Level

```
Example: 1000 Files

Level 0 (Leaves):  1000 nodes × 32 bytes = 32,000 bytes  ← Current
                   Discard Level 0
Level 1:           500 nodes × 32 bytes  = 16,000 bytes  ← Current
                   Discard Level 1
Level 2:           250 nodes × 32 bytes  = 8,000 bytes   ← Current
                   Discard Level 2
Level 3:           125 nodes × 32 bytes  = 4,000 bytes   ← Current
...
Level 10:          1 node × 32 bytes     = 32 bytes      ← ROOT

Max Memory: ~32 KB (only one level at a time)
Total Hashes Computed: 1000 + 500 + 250 + ... + 1 ≈ 2000 hashes
```

---

### 7. Verification Example

```
Scenario: Verify backup integrity after cloud download

Upload Time:
Files: [A, B, C, D]
Root: H(ABCD) = "fc3cddf64b6f8a..."
      ↓
  [Store root in CloudSnapshotMetadata]

Download Time:
Files: [A', B', C', D']  ← Downloaded from cloud
Root': H(A'B'C'D') = ?
      ↓
  [Calculate root from downloaded files]
      ↓
  [Compare: Root == Root'?]
      ↓
  ✓ Match → Backup intact
  ✗ Mismatch → Backup corrupted/tampered
```

---

### 8. Tamper Detection

```
Original Tree:
              ROOT1
           H(AB||CD)
           /       \
      H(A||B)    H(C||D)
       /   \      /   \
      A   B     C     D

If file C is modified to C':
              ROOT2  ← Different!
           H(AB||C'D)  ← Different!
           /       \
      H(A||B)    H(C'||D)  ← Different!
       /   \      /   \
      A   B     C'    D  ← Modified

ROOT1 ≠ ROOT2 → Tampering detected!

The change propagates up the tree, changing:
- H(C'||D) at level 1
- H(AB||C'D) at level 2 (root)
```

---

### 9. Real-World Example: ObsidianBackup

```
Backup Scenario:
- Snapshot ID: "backup_20250208_123456"
- Files: 
  1. WhatsApp.apk         (5 MB)
  2. WhatsApp_data.tar    (100 MB)
  3. Instagram.apk        (8 MB)
  4. Instagram_data.tar   (50 MB)
  5. Photos.apk           (3 MB)

Tree Construction:
                    ROOT
                H(WI||PWW)
               /           \
          H(W||I)        H(P||WW)  ← 5th duplicated
          /     \         /     \
      H(Wa)  H(Id)    H(Pa)  H(Wa)
         |      |        |      |
      WhatsApp Insta  Photos WhatsApp
        APK+   APK+    APK    data
        data   data                 (duplicate)

Root Hash: "a3f2c1d8e9b4f7..."
↓
Stored in CloudSnapshotMetadata
↓
Later: Download & verify integrity
```

---

### 10. Edge Cases Handled

#### Empty List
```
Input:  []
Output: ""
Reason: No files to hash
```

#### Single File
```
Input:  [H(A)]
Output: H(A)
Reason: File hash is already the root
```

#### Two Files
```
Input:  [H(A), H(B)]
Output: H(H(A)||H(B))
Reason: Simple parent-child relationship
```

#### Large Even Number (1024 files)
```
Input:  1024 files
Levels: log₂(1024) = 10 levels
Memory: ~32 KB per level (max)
Time:   O(n) = O(1024) ≈ 50ms
```

#### Large Odd Number (1023 files)
```
Input:  1023 files
Action: Duplicate last hash → 1024 nodes
Levels: 10 levels
Result: Same as 1024 files
```

---

## Code Reference

### Core Algorithm
```kotlin
internal suspend fun calculateMerkleRoot(files: List<CloudFile>): String {
    if (files.isEmpty()) return ""
    if (files.size == 1) return files[0].checksum
    
    var currentLevel = files.map { hexToBytes(it.checksum) }
    
    while (currentLevel.size > 1) {
        currentLevel = buildNextLevel(currentLevel)
    }
    
    return bytesToHex(currentLevel[0])
}

private suspend fun buildNextLevel(currentLevel: List<ByteArray>): List<ByteArray> {
    val nextLevel = mutableListOf<ByteArray>()
    val digest = MessageDigest.getInstance("SHA-256")
    
    var i = 0
    while (i < currentLevel.size) {
        val left = currentLevel[i]
        val right = if (i + 1 < currentLevel.size) {
            currentLevel[i + 1]
        } else {
            currentLevel[i]  // Duplicate for odd numbers
        }
        
        digest.reset()
        digest.update(left)
        digest.update(right)
        nextLevel.add(digest.digest())
        
        i += 2
    }
    
    return nextLevel
}
```

---

## Performance Comparison

| Files | Naive Approach* | Merkle Tree | Improvement |
|-------|----------------|-------------|-------------|
| 10    | 10 hashes      | 19 hashes   | Slightly worse |
| 100   | 100 hashes     | ~200 hashes | 2× more work |
| 1000  | 1000 hashes    | ~2000 hashes | 2× more work |

*Naive = just concatenate all hashes

**Why use Merkle trees if more work?**
- ✓ Verify single file with O(log n) hashes (not O(n))
- ✓ Efficient incremental updates
- ✓ Tamper detection shows WHICH file is bad
- ✓ Industry standard (Git, Bitcoin, IPFS)

---

## Verification Proof Example

To verify file C is in the tree, you only need:
```
              ROOT
           H(AB||CD)
           /       \
      H(AB)      H(CD)  ← Need this
       (?)        /  \
                 /    \
              H(C) ← Verify  H(D) ← Need this

Proof Path: [H(D), H(AB)]  ← Only 2 hashes!
Verify: H(H(C)||H(D)) = H(CD)
        H(H(AB)||H(CD)) = ROOT ✓

For 1000 files: Only ~10 hashes needed (not 999)!
```

---

## Summary

✓ Binary tree structure
✓ SHA-256 hashing at each level
✓ Duplicate last hash for odd nodes
✓ Memory efficient (one level at a time)
✓ Tamper detection (changes propagate to root)
✓ Industry standard approach

**Implementation in:** `app/src/main/java/com/obsidianbackup/cloud/CloudSyncManager.kt`
