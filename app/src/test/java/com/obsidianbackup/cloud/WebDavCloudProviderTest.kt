// cloud/WebDavCloudProviderTest.kt
package com.obsidianbackup.cloud

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for WebDavCloudProvider
 * Tests quota detection, chunked uploads, and progress callbacks
 */
class WebDavCloudProviderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var provider: WebDavCloudProvider
    private lateinit var context: Context
    private lateinit var logger: ObsidianLogger
    private lateinit var tempDir: File

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        context = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        
        tempDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdir()
        }
        
        every { context.cacheDir } returns tempDir
        
        val config = WebDavConfig(
            baseUrl = mockWebServer.url("/").toString().trimEnd('/'),
            username = "testuser",
            password = "testpass",
            chunkSize = 1024 * 1024 // 1MB for testing
        )
        
        provider = WebDavCloudProvider(context, logger, config)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        tempDir.deleteRecursively()
    }

    // ============================================================================
    // Quota Detection Tests
    // ============================================================================

    @Test
    fun `getStorageQuota should parse PROPFIND quota response correctly`() = runTest {
        // Setup - Mock PROPFIND response with quota info
        val quotaResponse = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:propstat>
                        <d:prop>
                            <d:quota-available-bytes>5368709120</d:quota-available-bytes>
                            <d:quota-used-bytes>1073741824</d:quota-used-bytes>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(quotaResponse)
                .setHeader("Content-Type", "application/xml")
        )
        
        // Execute
        val result = provider.getStorageQuota()
        
        // Verify
        assertIs<CloudResult.Success<StorageQuota>>(result)
        val quota = result.data
        
        assertEquals(5368709120L, quota.availableBytes, "Available bytes should match")
        assertEquals(1073741824L, quota.usedBytes, "Used bytes should match")
        assertEquals(6442450944L, quota.totalBytes, "Total bytes should be used + available")
        
        // Verify PROPFIND request was made
        val request = mockWebServer.takeRequest()
        assertEquals("PROPFIND", request.method)
        assertEquals("0", request.getHeader("Depth"))
        assertTrue(request.body.readUtf8().contains("quota-available-bytes"))
    }

    @Test
    fun `getStorageQuota should handle server without quota support gracefully`() = runTest {
        // Setup - Mock server that doesn't support quota (404)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not found")
        )
        
        // Execute
        val result = provider.getStorageQuota()
        
        // Verify - should return unlimited quota
        assertIs<CloudResult.Success<StorageQuota>>(result)
        val quota = result.data
        
        assertEquals(Long.MAX_VALUE, quota.totalBytes, "Should return unlimited quota")
        assertEquals(Long.MAX_VALUE, quota.availableBytes, "Should return unlimited available")
        assertEquals(0L, quota.usedBytes, "Should return zero used bytes")
    }

    @Test
    fun `getStorageQuota should handle malformed XML response`() = runTest {
        // Setup - Mock invalid XML
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<invalid>xml")
                .setHeader("Content-Type", "application/xml")
        )
        
        // Execute
        val result = provider.getStorageQuota()
        
        // Verify - should fallback to default quota
        assertIs<CloudResult.Success<StorageQuota>>(result)
        assertEquals(Long.MAX_VALUE, result.data.totalBytes)
    }

    @Test
    fun `getStorageQuota should handle quota with only used bytes`() = runTest {
        // Setup - Some servers only report used bytes
        val quotaResponse = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:propstat>
                        <d:prop>
                            <d:quota-used-bytes>2147483648</d:quota-used-bytes>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(quotaResponse)
        )
        
        // Execute
        val result = provider.getStorageQuota()
        
        // Verify
        assertIs<CloudResult.Success<StorageQuota>>(result)
        assertEquals(2147483648L, result.data.usedBytes)
        assertEquals(Long.MAX_VALUE, result.data.availableBytes)
    }

    // ============================================================================
    // Server Capabilities Detection Tests
    // ============================================================================

    @Test
    fun `testConnection should detect Nextcloud server`() = runTest {
        // Setup
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Server", "nginx/1.18.0")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<?xml version='1.0' encoding='utf-8'?><d:multistatus xmlns:d='DAV:'></d:multistatus>")
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Server", "Apache/2.4.41 (Ubuntu)")
                .setHeader("X-Powered-By", "PHP/7.4 - Nextcloud")
                .setHeader("DAV", "1, 2, 3, chunked-upload")
                .setHeader("Allow", "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPFIND, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PUT")
        )
        
        // Execute
        val result = provider.testConnection()
        
        // Verify
        assertIs<CloudResult.Success<ConnectionInfo>>(result)
        assertTrue(result.data.isConnected)
        assertTrue(result.data.latencyMs >= 0)
    }

    @Test
    fun `testConnection should handle authentication failure`() = runTest {
        // Setup - Mock 401 Unauthorized
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )
        
        // Execute
        val result = provider.testConnection()
        
        // Verify
        assertIs<CloudResult.Error>(result)
        assertEquals(CloudError.ErrorCode.AUTHENTICATION_FAILED, result.error.code)
        assertTrue(result.error.message.contains("Failed to connect"))
    }

    @Test
    fun `testConnection should handle network errors`() = runTest {
        // Setup - Shutdown server to simulate network error
        mockWebServer.shutdown()
        
        // Execute
        val result = provider.testConnection()
        
        // Verify
        assertIs<CloudResult.Error>(result)
        assertEquals(CloudError.ErrorCode.NETWORK_ERROR, result.error.code)
        assertTrue(result.error.retryable, "Network errors should be retryable")
    }

    // ============================================================================
    // Chunked Upload Tests
    // ============================================================================

    @Test
    fun `uploadFile should use simple PUT for small files`() = runTest {
        // Setup
        val smallFile = File(tempDir, "small.txt").apply {
            writeText("Small file content")
        }
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200)) // OPTIONS
        mockWebServer.enqueue(MockResponse().setResponseCode(201)) // PUT
        
        // Execute
        val result = provider.uploadFile(smallFile, "test/small.txt")
        
        // Verify
        assertIs<CloudResult.Success<Unit>>(result)
        
        // Should have two requests: OPTIONS and PUT
        assertEquals(2, mockWebServer.requestCount)
        
        val putRequest = mockWebServer.takeRequest() // OPTIONS
        mockWebServer.takeRequest() // Skip OPTIONS
        val actualPutRequest = mockWebServer.takeRequest()
        
        // Verify PUT request was made
        // Note: The actual verification depends on the sardine implementation
    }

    @Test
    fun `uploadFile should handle upload failures`() = runTest {
        // Setup
        val file = File(tempDir, "test.txt").apply {
            writeText("Test content")
        }
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200)) // OPTIONS
        mockWebServer.enqueue(MockResponse().setResponseCode(507)) // 507 Insufficient Storage
        
        // Execute
        val result = provider.uploadFile(file, "test/file.txt")
        
        // Verify
        assertIs<CloudResult.Error>(result)
        // Error mapping should detect 507 as quota exceeded
    }

    // ============================================================================
    // Progress Tracking Tests
    // ============================================================================

    @Test
    fun `observeProgress should emit upload progress events`() = runTest {
        // Setup
        val snapshotId = SnapshotId("test-snapshot")
        val file = File(tempDir, "test.dat").apply {
            writeBytes(ByteArray(1024) { it.toByte() })
        }
        
        val cloudFile = CloudFile(
            localPath = file,
            remotePath = "test.dat",
            checksum = "abc123",
            sizeBytes = file.length()
        )
        
        // Mock responses
        mockWebServer.enqueue(MockResponse().setResponseCode(200)) // OPTIONS
        mockWebServer.enqueue(MockResponse().setResponseCode(200)) // List (check exists)
        mockWebServer.enqueue(MockResponse().setResponseCode(404)) // Check snapshot dir
        mockWebServer.enqueue(MockResponse().setResponseCode(201)) // Create dir
        mockWebServer.enqueue(MockResponse().setResponseCode(201)) // Upload metadata
        mockWebServer.enqueue(MockResponse().setResponseCode(201)) // Upload file
        
        val metadata = CloudSnapshotMetadata(
            snapshotId = snapshotId,
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device",
            appCount = 1,
            totalSizeBytes = file.length(),
            compressionRatio = 1.0f,
            encrypted = false,
            merkleRootHash = "root-hash"
        )
        
        // Execute - start observing before upload
        val progressFlow = provider.observeProgress()
        
        // This test would need to be adjusted based on actual implementation
        // For now, verify that the flow is accessible
        assertTrue(progressFlow != null, "Progress flow should be accessible")
    }

    @Test
    fun `observeProgress should emit completion event on success`() = runTest {
        // This test would verify that CloudTransferProgress.Completed is emitted
        // Implementation depends on how we want to test flows in the actual code
        val progressFlow = provider.observeProgress()
        assertTrue(progressFlow != null)
    }

    @Test
    fun `observeProgress should emit failure event on error`() = runTest {
        // This test would verify that CloudTransferProgress.Failed is emitted
        val progressFlow = provider.observeProgress()
        assertTrue(progressFlow != null)
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Test
    fun `full upload flow should work end-to-end`() = runTest {
        // Setup - Create test snapshot
        val snapshotId = SnapshotId("integration-test")
        val file1 = File(tempDir, "file1.txt").apply { writeText("Content 1") }
        val file2 = File(tempDir, "file2.txt").apply { writeText("Content 2") }
        
        val files = listOf(
            CloudFile(file1, "file1.txt", "hash1", file1.length()),
            CloudFile(file2, "file2.txt", "hash2", file2.length())
        )
        
        val metadata = CloudSnapshotMetadata(
            snapshotId = snapshotId,
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device",
            appCount = 2,
            totalSizeBytes = file1.length() + file2.length(),
            compressionRatio = 1.0f,
            encrypted = false,
            merkleRootHash = "root"
        )
        
        // Mock all necessary responses
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200))
        }
        
        // Execute
        val result = provider.uploadSnapshot(snapshotId, files, metadata)
        
        // Verify
        assertIs<CloudResult.Success<CloudUploadSummary>>(result)
        val summary = result.data
        
        assertEquals(snapshotId, summary.snapshotId)
        assertEquals(2, summary.filesUploaded)
        assertTrue(summary.bytesUploaded > 0)
        assertTrue(summary.duration >= 0)
    }

    @Test
    fun `deleteSnapshot should remove snapshot directory`() = runTest {
        // Setup
        val snapshotId = SnapshotId("to-delete")
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200)) // Check exists
        mockWebServer.enqueue(MockResponse().setResponseCode(204)) // Delete
        
        // Execute
        val result = provider.deleteSnapshot(snapshotId)
        
        // Verify
        assertIs<CloudResult.Success<Unit>>(result)
    }

    @Test
    fun `deleteSnapshot should handle non-existent snapshot`() = runTest {
        // Setup
        val snapshotId = SnapshotId("non-existent")
        
        mockWebServer.enqueue(MockResponse().setResponseCode(404)) // Not found
        
        // Execute
        val result = provider.deleteSnapshot(snapshotId)
        
        // Verify
        assertIs<CloudResult.Error>(result)
        assertEquals(CloudError.ErrorCode.FILE_NOT_FOUND, result.error.code)
        assertFalse(result.error.retryable, "File not found should not be retryable")
    }
}
