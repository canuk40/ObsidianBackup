package com.obsidianbackup.verification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerkleTree @Inject constructor() {
    
    private val mutex = Mutex()
    private var nodes: List<List<String>> = emptyList()
    private var leafHashes: Map<String, Int> = emptyMap()
    
    suspend fun buildTree(files: List<File>): String = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (files.isEmpty()) {
                return@withContext ""
            }
            
            val leaves = files.map { file -> 
                hashFile(file) to file.absolutePath 
            }
            
            leafHashes = leaves.mapIndexed { index, (hash, path) -> 
                path to index 
            }.toMap()
            
            val hashes = leaves.map { it.first }
            nodes = buildTreeLayers(hashes)
            
            nodes.lastOrNull()?.firstOrNull() ?: ""
        }
    }
    
    suspend fun buildTreeFromHashes(fileHashes: Map<String, String>): String = 
        withContext(Dispatchers.Default) {
            mutex.withLock {
                if (fileHashes.isEmpty()) {
                    return@withContext ""
                }
                
                leafHashes = fileHashes.keys.mapIndexed { index, path -> 
                    path to index 
                }.toMap()
                
                val hashes = fileHashes.values.toList()
                nodes = buildTreeLayers(hashes)
                
                nodes.lastOrNull()?.firstOrNull() ?: ""
            }
        }
    
    private fun buildTreeLayers(leaves: List<String>): List<List<String>> {
        if (leaves.isEmpty()) return emptyList()
        
        val layers = mutableListOf<List<String>>()
        layers.add(leaves)
        
        var currentLayer = leaves
        
        while (currentLayer.size > 1) {
            val nextLayer = mutableListOf<String>()
            
            for (i in currentLayer.indices step 2) {
                val left = currentLayer[i]
                val right = if (i + 1 < currentLayer.size) {
                    currentLayer[i + 1]
                } else {
                    left
                }
                
                val combined = hashPair(left, right)
                nextLayer.add(combined)
            }
            
            layers.add(nextLayer)
            currentLayer = nextLayer
        }
        
        return layers
    }
    
    suspend fun generateProof(filePath: String): MerkleProof? = 
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val leafIndex = leafHashes[filePath] ?: return@withLock null
                
                if (nodes.isEmpty()) {
                    return@withLock null
                }
                
                val fileHash = nodes[0].getOrNull(leafIndex) ?: return@withLock null
                val siblings = mutableListOf<String>()
                
                var currentIndex = leafIndex
                
                for (layerIndex in 0 until nodes.size - 1) {
                    val layer = nodes[layerIndex]
                    val siblingIndex = if (currentIndex % 2 == 0) {
                        currentIndex + 1
                    } else {
                        currentIndex - 1
                    }
                    
                    if (siblingIndex < layer.size) {
                        siblings.add(layer[siblingIndex])
                    } else if (currentIndex < layer.size) {
                        siblings.add(layer[currentIndex])
                    }
                    
                    currentIndex /= 2
                }
                
                val rootHash = getRootHash()
                
                MerkleProof(
                    fileHash = fileHash,
                    filePath = filePath,
                    leafIndex = leafIndex,
                    siblings = siblings,
                    rootHash = rootHash
                )
            }
        }
    
    suspend fun verifyProof(proof: MerkleProof): Boolean = 
        withContext(Dispatchers.Default) {
            if (!proof.isValid()) {
                return@withContext false
            }
            
            var currentHash = proof.fileHash
            var index = proof.leafIndex
            
            for (sibling in proof.siblings) {
                currentHash = if (index % 2 == 0) {
                    hashPair(currentHash, sibling)
                } else {
                    hashPair(sibling, currentHash)
                }
                index /= 2
            }
            
            currentHash == proof.rootHash
        }
    
    suspend fun verifyFileWithProof(file: File, proof: MerkleProof): Boolean = 
        withContext(Dispatchers.IO) {
            val actualHash = hashFile(file)
            
            if (actualHash != proof.fileHash) {
                return@withContext false
            }
            
            verifyProof(proof)
        }
    
    suspend fun getRootHash(): String = mutex.withLock {
        nodes.lastOrNull()?.firstOrNull() ?: ""
    }
    
    suspend fun getMetadata(): MerkleTreeMetadata = mutex.withLock {
        MerkleTreeMetadata(
            rootHash = getRootHash(),
            leafCount = leafHashes.size,
            algorithm = "SHA-256"
        )
    }
    
    private suspend fun hashFile(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        
        file.inputStream().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    private fun hashPair(left: String, right: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(left.toByteArray())
        digest.update(right.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    suspend fun clear() = mutex.withLock {
        nodes = emptyList()
        leafHashes = emptyMap()
    }
    
    suspend fun getLeafCount(): Int = mutex.withLock {
        leafHashes.size
    }
}
