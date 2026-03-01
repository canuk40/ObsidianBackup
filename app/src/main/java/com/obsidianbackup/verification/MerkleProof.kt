package com.obsidianbackup.verification

import kotlinx.serialization.Serializable

@Serializable
data class MerkleProof(
    val fileHash: String,
    val filePath: String,
    val leafIndex: Int,
    val siblings: List<String>,
    val rootHash: String
) {
    fun isValid(): Boolean = siblings.isNotEmpty() && rootHash.isNotEmpty()
    
    fun size(): Int = siblings.size
}

@Serializable
data class MerkleTreeMetadata(
    val rootHash: String,
    val leafCount: Int,
    val algorithm: String = "SHA-256",
    val timestamp: Long = System.currentTimeMillis()
)
