package com.obsidianbackup.testing.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock implementation of CloudProvider for testing.
 */
class MockCloudProvider : CloudProvider {
    var uploadedFiles = mutableListOf<Pair<String, ByteArray>>()
    var downloadedFiles = mutableMapOf<String, ByteArray>()
    var deletedFiles = mutableListOf<String>()
    var shouldFail = false
    var failureMessage = "Mock failure"
    
    override suspend fun upload(localPath: String, remotePath: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            uploadedFiles.add(remotePath to byteArrayOf())
            Result.success(Unit)
        }
    }
    
    override suspend fun download(remotePath: String, localPath: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            downloadedFiles[remotePath] = byteArrayOf()
            Result.success(Unit)
        }
    }
    
    override suspend fun delete(remotePath: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            deletedFiles.add(remotePath)
            Result.success(Unit)
        }
    }
    
    override suspend fun listFiles(remotePath: String): Result<List<CloudFile>> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            Result.success(emptyList())
        }
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            Result.success(uploadedFiles.any { it.first == remotePath })
        }
    }
    
    fun reset() {
        uploadedFiles.clear()
        downloadedFiles.clear()
        deletedFiles.clear()
        shouldFail = false
    }
}

interface CloudProvider {
    suspend fun upload(localPath: String, remotePath: String): Result<Unit>
    suspend fun download(remotePath: String, localPath: String): Result<Unit>
    suspend fun delete(remotePath: String): Result<Unit>
    suspend fun listFiles(remotePath: String): Result<List<CloudFile>>
    suspend fun exists(remotePath: String): Result<Boolean>
}

data class CloudFile(
    val name: String,
    val path: String,
    val size: Long,
    val modifiedTime: Long,
    val isDirectory: Boolean = false
)
