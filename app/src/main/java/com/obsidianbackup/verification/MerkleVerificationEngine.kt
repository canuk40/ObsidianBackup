package com.obsidianbackup.verification

import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerkleVerificationEngine @Inject constructor(
    private val merkleTree: MerkleTree,
    private val checksumVerifier: ChecksumVerifier,
    private val catalog: BackupCatalog
) : BackupEngine {
    
    private val _progress = MutableStateFlow<OperationProgress>(
        OperationProgress(
            operationType = OperationType.VERIFY,
            currentItem = "",
            itemsCompleted = 0,
            totalItems = 0
        )
    )
    
    private val json = Json { prettyPrint = true }
    
    override suspend fun backupApps(request: BackupRequest): BackupResult {
        return BackupResult.Failure(
            reason = "MerkleVerificationEngine is verification-only, use primary backup engine",
            appsFailed = request.appIds
        )
    }
    
    override suspend fun restoreApps(request: RestoreRequest): RestoreResult {
        return RestoreResult.Failure(
            reason = "MerkleVerificationEngine is verification-only, use primary restore engine"
        )
    }
    
    override suspend fun verifySnapshot(id: BackupId): VerificationResult = 
        withContext(Dispatchers.IO) {
            val snapshotDir = catalog.getSnapshotDirectory(id)
            
            if (!snapshotDir.exists() || !snapshotDir.isDirectory) {
                return@withContext VerificationResult(
                    snapshotId = SnapshotId(id.value),
                    filesChecked = 0,
                    allValid = false,
                    corruptedFiles = listOf("Snapshot directory not found")
                )
            }
            
            val merkleMetadataFile = File(snapshotDir, MERKLE_METADATA_FILE)
            val merkleProofsFile = File(snapshotDir, MERKLE_PROOFS_FILE)
            
            if (!merkleMetadataFile.exists()) {
                return@withContext performBasicChecksumVerification(id, snapshotDir)
            }
            
            performMerkleVerification(id, snapshotDir, merkleMetadataFile, merkleProofsFile)
        }
    
    override suspend fun deleteSnapshot(id: BackupId): Boolean {
        return false
    }
    
    override fun observeProgress(): Flow<OperationProgress> = _progress.asStateFlow()
    
    suspend fun generateMerkleRoot(files: List<File>, snapshotDir: File): String = 
        withContext(Dispatchers.Default) {
            if (files.isEmpty()) {
                return@withContext ""
            }
            
            val rootHash = merkleTree.buildTree(files)
            
            val metadata = merkleTree.getMetadata()
            val metadataFile = File(snapshotDir, MERKLE_METADATA_FILE)
            metadataFile.writeText(json.encodeToString(metadata))
            
            val proofsFile = File(snapshotDir, MERKLE_PROOFS_FILE)
            val proofs = mutableMapOf<String, MerkleProof>()
            
            files.forEach { file ->
                val relativePath = file.relativeTo(snapshotDir).path
                merkleTree.generateProof(file.absolutePath)?.let { proof ->
                    proofs[relativePath] = proof
                }
            }
            
            proofsFile.writeText(json.encodeToString(proofs))
            
            rootHash
        }
    
    suspend fun generateMerkleRootFromChecksums(
        checksums: Map<String, String>,
        snapshotDir: File
    ): String = withContext(Dispatchers.Default) {
        if (checksums.isEmpty()) {
            return@withContext ""
        }
        
        val rootHash = merkleTree.buildTreeFromHashes(checksums)
        
        val metadata = merkleTree.getMetadata()
        val metadataFile = File(snapshotDir, MERKLE_METADATA_FILE)
        metadataFile.writeText(json.encodeToString(metadata))
        
        val proofsFile = File(snapshotDir, MERKLE_PROOFS_FILE)
        val proofs = mutableMapOf<String, MerkleProof>()
        
        checksums.keys.forEach { filePath ->
            merkleTree.generateProof(filePath)?.let { proof ->
                proofs[filePath] = proof
            }
        }
        
        proofsFile.writeText(json.encodeToString(proofs))
        
        rootHash
    }
    
    suspend fun verifyFileIncremental(
        file: File,
        snapshotDir: File
    ): Boolean = withContext(Dispatchers.IO) {
        val merkleMetadataFile = File(snapshotDir, MERKLE_METADATA_FILE)
        val merkleProofsFile = File(snapshotDir, MERKLE_PROOFS_FILE)
        
        if (!merkleMetadataFile.exists() || !merkleProofsFile.exists()) {
            return@withContext false
        }
        
        val relativePath = file.relativeTo(snapshotDir).path
        val proofsMap = json.decodeFromString<Map<String, MerkleProof>>(
            merkleProofsFile.readText()
        )
        
        val proof = proofsMap[relativePath] ?: return@withContext false
        
        merkleTree.verifyFileWithProof(file, proof)
    }
    
    private suspend fun performMerkleVerification(
        id: BackupId,
        snapshotDir: File,
        metadataFile: File,
        proofsFile: File
    ): VerificationResult = withContext(Dispatchers.IO) {
        
        val metadata = json.decodeFromString<MerkleTreeMetadata>(metadataFile.readText())
        
        val proofs = if (proofsFile.exists()) {
            json.decodeFromString<Map<String, MerkleProof>>(proofsFile.readText())
        } else {
            emptyMap()
        }
        
        val corruptedFiles = mutableListOf<String>()
        var filesChecked = 0
        
        updateProgress(
            OperationType.VERIFY,
            "Verifying Merkle tree",
            0,
            proofs.size
        )
        
        proofs.forEach { (relativePath, proof) ->
            val file = File(snapshotDir, relativePath)
            
            if (!file.exists()) {
                corruptedFiles.add("$relativePath (missing)")
            } else {
                filesChecked++
                
                if (!merkleTree.verifyFileWithProof(file, proof)) {
                    corruptedFiles.add("$relativePath (Merkle proof failed)")
                }
                
                updateProgress(
                    OperationType.VERIFY,
                    relativePath,
                    filesChecked,
                    proofs.size
                )
            }
        }
        
        VerificationResult(
            snapshotId = SnapshotId(id.value),
            filesChecked = filesChecked,
            allValid = corruptedFiles.isEmpty(),
            corruptedFiles = corruptedFiles
        )
    }
    
    private suspend fun performBasicChecksumVerification(
        id: BackupId,
        snapshotDir: File
    ): VerificationResult = withContext(Dispatchers.IO) {
        
        val metadata = catalog.getSnapshot(id) ?: return@withContext VerificationResult(
            snapshotId = SnapshotId(id.value),
            filesChecked = 0,
            allValid = false,
            corruptedFiles = listOf("Metadata not found")
        )
        
        checksumVerifier.verifySnapshot(snapshotDir, metadata.checksums)
    }
    
    private fun updateProgress(
        operationType: OperationType,
        currentItem: String,
        itemsCompleted: Int,
        totalItems: Int
    ) {
        _progress.value = OperationProgress(
            operationType = operationType,
            currentItem = currentItem,
            itemsCompleted = itemsCompleted,
            totalItems = totalItems
        )
    }
    
    companion object {
        private const val MERKLE_METADATA_FILE = ".merkle_metadata.json"
        private const val MERKLE_PROOFS_FILE = ".merkle_proofs.json"
    }
}
