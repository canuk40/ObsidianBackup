// verification/ChecksumVerifier.kt
package com.obsidianbackup.verification

import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecksumVerifier @Inject constructor(
    private val merkleTree: MerkleTree
) {
    
    suspend fun verifyWithMerkle(
        file: File,
        proof: MerkleProof
    ): Boolean = withContext(Dispatchers.IO) {
        merkleTree.verifyFileWithProof(file, proof)
    }
    
    suspend fun verifySnapshotWithMerkle(
        snapshotDir: File,
        merkleMetadataFile: File,
        expectedChecksums: Map<String, String>
    ): VerificationResult = withContext(Dispatchers.IO) {
        val corruptedFiles = mutableListOf<String>()
        var filesChecked = 0
        
        if (!merkleMetadataFile.exists()) {
            return@withContext verifySnapshot(snapshotDir, expectedChecksums)
        }
        
        val checksumMap = expectedChecksums.mapKeys { (path, _) ->
            File(snapshotDir, path).absolutePath
        }
        
        val rootHash = merkleTree.buildTreeFromHashes(checksumMap)
        
        expectedChecksums.forEach { (relativePath, expectedChecksum) ->
            val file = File(snapshotDir, relativePath)
            
            if (!file.exists()) {
                corruptedFiles.add("$relativePath (missing)")
            } else {
                filesChecked++
                
                val actualChecksum = calculateChecksum(file)
                if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                    corruptedFiles.add("$relativePath (checksum mismatch)")
                } else {
                    val proof = merkleTree.generateProof(file.absolutePath)
                    if (proof == null || !merkleTree.verifyProof(proof)) {
                        corruptedFiles.add("$relativePath (Merkle verification failed)")
                    }
                }
            }
        }
        
        if (corruptedFiles.isEmpty()) {
            VerificationResult(
                snapshotId = SnapshotId(snapshotDir.name),
                filesChecked = filesChecked,
                allValid = true,
                corruptedFiles = emptyList()
            )
        } else {
            VerificationResult(
                snapshotId = SnapshotId(snapshotDir.name),
                filesChecked = filesChecked,
                allValid = false,
                corruptedFiles = corruptedFiles
            )
        }
    }

    /**
     * Calculate SHA256 checksum for a file
     */
    suspend fun calculateChecksum(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)

        FileInputStream(file).buffered(8192).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate checksums for multiple files
     */
    suspend fun calculateChecksums(files: List<File>): Map<String, String> = withContext(Dispatchers.IO) {
        files.associate { file ->
            file.name to calculateChecksum(file)
        }
    }

    /**
     * Verify file against expected checksum
     */
    suspend fun verifyFile(file: File, expectedChecksum: String): Boolean {
        val actualChecksum = calculateChecksum(file)
        return actualChecksum.equals(expectedChecksum, ignoreCase = true)
    }

    /**
     * Verify entire snapshot directory
     */
    suspend fun verifySnapshot(
        snapshotDir: File,
        expectedChecksums: Map<String, String>
    ): VerificationResult = withContext(Dispatchers.IO) {
        val corruptedFiles = mutableListOf<String>()
        var filesChecked = 0

        expectedChecksums.forEach { (relativePath, expectedChecksum) ->
            val file = File(snapshotDir, relativePath)

            if (!file.exists()) {
                corruptedFiles.add("$relativePath (missing)")
            } else {
                filesChecked++
                val actualChecksum = calculateChecksum(file)
                if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                    corruptedFiles.add("$relativePath (checksum mismatch)")
                }
            }
        }

        if (corruptedFiles.isEmpty()) {
            VerificationResult(
                snapshotId = SnapshotId(snapshotDir.name),
                filesChecked = filesChecked,
                allValid = true,
                corruptedFiles = emptyList()
            )
        } else {
            VerificationResult(
                snapshotId = SnapshotId(snapshotDir.name),
                filesChecked = filesChecked,
                allValid = false,
                corruptedFiles = corruptedFiles
            )
        }
    }

    /**
     * Write checksums to file
     */
    suspend fun writeChecksumsFile(
        checksums: Map<String, String>,
        outputFile: File
    ) = withContext(Dispatchers.IO) {
        val content = checksums.entries.joinToString("\n") { (file, checksum) ->
            "$checksum  $file"
        }
        outputFile.writeText(content)
    }

    /**
     * Read checksums from file
     */
    suspend fun readChecksumsFile(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        file.readLines()
            .filter { it.isNotBlank() }
            .associate { line ->
                val parts = line.split(Regex("\\s+"), 2)
                if (parts.size == 2) {
                    parts[1] to parts[0]
                } else {
                    "" to ""
                }
            }
            .filterKeys { it.isNotEmpty() }
    }
}
