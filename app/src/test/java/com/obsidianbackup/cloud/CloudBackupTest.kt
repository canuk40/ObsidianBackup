package com.obsidianbackup.cloud

import com.obsidianbackup.testing.mocks.*
import com.obsidianbackup.testing.TestDataFactory
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for cloud backup operations.
 */
@DisplayName("Cloud Backup Tests")
class CloudBackupTest {
    
    private lateinit var mockCloudProvider: MockCloudProvider
    private lateinit var mockBackupEngine: MockBackupEngine
    
    @BeforeEach
    fun setup() {
        mockCloudProvider = MockCloudProvider()
        mockBackupEngine = MockBackupEngine()
    }
    
    @AfterEach
    fun teardown() {
        mockCloudProvider.reset()
        mockBackupEngine.reset()
    }
    
    @Nested
    @DisplayName("Upload Operations")
    inner class UploadOperations {
        
        @Test
        @DisplayName("Should upload file successfully")
        fun testUploadFileSuccess() = runTest {
            // Given
            val localPath = "/local/test.txt"
            val remotePath = "/remote/test.txt"
            
            // When
            val result = mockCloudProvider.upload(localPath, remotePath)
            
            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(mockCloudProvider.uploadedFiles).hasSize(1)
            assertThat(mockCloudProvider.uploadedFiles[0].first).isEqualTo(remotePath)
        }
        
        @Test
        @DisplayName("Should handle upload failure")
        fun testUploadFileFailure() = runTest {
            // Given
            mockCloudProvider.shouldFail = true
            val localPath = "/local/test.txt"
            val remotePath = "/remote/test.txt"
            
            // When
            val result = mockCloudProvider.upload(localPath, remotePath)
            
            // Then
            assertThat(result.isFailure).isTrue()
            assertThat(mockCloudProvider.uploadedFiles).isEmpty()
        }
        
        @ParameterizedTest
        @ValueSource(ints = [1, 5, 10, 50])
        @DisplayName("Should upload multiple files")
        fun testUploadMultipleFiles(fileCount: Int) = runTest {
            // Given & When
            repeat(fileCount) { index ->
                mockCloudProvider.upload("/local/file$index.txt", "/remote/file$index.txt")
            }
            
            // Then
            assertThat(mockCloudProvider.uploadedFiles).hasSize(fileCount)
        }
    }
    
    @Nested
    @DisplayName("Download Operations")
    inner class DownloadOperations {
        
        @Test
        @DisplayName("Should download file successfully")
        fun testDownloadFileSuccess() = runTest {
            // Given
            val remotePath = "/remote/test.txt"
            val localPath = "/local/test.txt"
            
            // When
            val result = mockCloudProvider.download(remotePath, localPath)
            
            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(mockCloudProvider.downloadedFiles).containsKey(remotePath)
        }
        
        @Test
        @DisplayName("Should handle download failure")
        fun testDownloadFileFailure() = runTest {
            // Given
            mockCloudProvider.shouldFail = true
            
            // When
            val result = mockCloudProvider.download("/remote/test.txt", "/local/test.txt")
            
            // Then
            assertThat(result.isFailure).isTrue()
            assertThat(mockCloudProvider.downloadedFiles).isEmpty()
        }
    }
    
    @Nested
    @DisplayName("File Existence Checks")
    inner class FileExistenceChecks {
        
        @Test
        @DisplayName("Should check if file exists")
        fun testFileExists() = runTest {
            // Given
            val remotePath = "/remote/test.txt"
            mockCloudProvider.upload("/local/test.txt", remotePath)
            
            // When
            val result = mockCloudProvider.exists(remotePath)
            
            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isTrue()
        }
        
        @Test
        @DisplayName("Should return false for non-existent file")
        fun testFileDoesNotExist() = runTest {
            // When
            val result = mockCloudProvider.exists("/remote/nonexistent.txt")
            
            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isFalse()
        }
    }
    
    @Nested
    @DisplayName("Delete Operations")
    inner class DeleteOperations {
        
        @Test
        @DisplayName("Should delete file successfully")
        fun testDeleteFileSuccess() = runTest {
            // Given
            val remotePath = "/remote/test.txt"
            
            // When
            val result = mockCloudProvider.delete(remotePath)
            
            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(mockCloudProvider.deletedFiles).contains(remotePath)
        }
        
        @Test
        @DisplayName("Should handle delete failure")
        fun testDeleteFileFailure() = runTest {
            // Given
            mockCloudProvider.shouldFail = true
            
            // When
            val result = mockCloudProvider.delete("/remote/test.txt")
            
            // Then
            assertThat(result.isFailure).isTrue()
            assertThat(mockCloudProvider.deletedFiles).isEmpty()
        }
    }
}
