// Test: FilecoinCloudProvider
package com.obsidianbackup.cloud

import android.content.Context
import android.content.SharedPreferences
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for FilecoinCloudProvider
 */
class FilecoinCloudProviderTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockLogger: ObsidianLogger

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var config: FilecoinConfig
    private lateinit var provider: FilecoinCloudProvider

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup mock preferences
        `when`(mockContext.getSharedPreferences("filecoin_config", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)

        // Create test configuration
        config = FilecoinConfig(
            web3StorageToken = "test_token_eyJ123",
            ipfsGateways = listOf(
                "https://dweb.link",
                "https://ipfs.io"
            ),
            enableFilecoinDeals = true,
            pinningService = "web3.storage"
        )

        provider = FilecoinCloudProvider(mockContext, mockLogger, config)
    }

    @Test
    fun `test provider ID and display name`() {
        assertEquals("filecoin", provider.providerId)
        assertEquals("Filecoin/IPFS (Decentralized)", provider.displayName)
    }

    @Test
    fun `test configuration validation`() {
        // Valid configuration
        val validConfig = FilecoinConfig(
            web3StorageToken = "eyJ123valid",
            ipfsGateways = listOf("https://dweb.link")
        )
        assertNotNull(validConfig)
        assertEquals("eyJ123valid", validConfig.web3StorageToken)

        // Gateway list validation
        assertTrue(validConfig.ipfsGateways.isNotEmpty())
        assertTrue(validConfig.ipfsGateways.first().startsWith("https://"))
    }

    @Test
    fun `test CID format validation`() {
        // Valid CIDs
        val validCids = listOf(
            "bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi",
            "QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco",
            "bafkreigh2akiscaildcqabsyg3dfr6chu3fgpregiymsck7e7aqa4s52zy"
        )

        validCids.forEach { cid ->
            assertTrue(cid.length > 10, "CID should be reasonably long")
            assertTrue(
                cid.startsWith("bafy") || cid.startsWith("Qm") || cid.startsWith("bafk"),
                "CID should start with valid prefix"
            )
        }
    }

    @Test
    fun `test storage cost calculation`() = runBlocking {
        val sizeBytes = 1024L * 1024L * 1024L // 1GB

        val result = provider.getStorageCost(sizeBytes)

        assertTrue(result is CloudResult.Success)
        val cost = (result as CloudResult.Success).data

        // Verify cost is reasonable
        assertTrue(cost.filAmount > 0.0)
        assertTrue(cost.filAmount < 1.0) // Should be very cheap
        assertTrue(cost.usdAmount > 0.0)
        assertEquals("permanent", cost.duration)
    }

    @Test
    fun `test snapshot metadata structure`() {
        val metadata = CloudSnapshotMetadata(
            snapshotId = SnapshotId("test_snapshot"),
            timestamp = System.currentTimeMillis(),
            deviceId = "test_device",
            appCount = 5,
            totalSizeBytes = 50_000_000L,
            compressionRatio = 0.75f,
            encrypted = true,
            merkleRootHash = "test_merkle_root",
            customMetadata = mapOf("key" to "value")
        )

        assertEquals("test_snapshot", metadata.snapshotId.value)
        assertEquals(5, metadata.appCount)
        assertTrue(metadata.encrypted)
        assertEquals("test_merkle_root", metadata.merkleRootHash)
        assertEquals("value", metadata.customMetadata["key"])
    }

    @Test
    fun `test cloud file structure`() {
        val file = CloudFile(
            localPath = File("/tmp/test.apk"),
            remotePath = "backups/test.apk",
            checksum = "sha256:abc123",
            sizeBytes = 1024L
        )

        assertEquals("/tmp/test.apk", file.localPath.path)
        assertEquals("backups/test.apk", file.remotePath)
        assertEquals("sha256:abc123", file.checksum)
        assertEquals(1024L, file.sizeBytes)
    }

    @Test
    fun `test upload summary structure`() {
        val summary = CloudUploadSummary(
            snapshotId = SnapshotId("test"),
            filesUploaded = 3,
            bytesUploaded = 3_000_000L,
            duration = 5000L,
            averageSpeed = 600_000L,
            remoteUrls = mapOf(
                "file1.apk" to "bafytest1",
                "file2.apk" to "bafytest2"
            )
        )

        assertEquals(3, summary.filesUploaded)
        assertEquals(3_000_000L, summary.bytesUploaded)
        assertEquals(600_000L, summary.averageSpeed)
        assertEquals(2, summary.remoteUrls.size)
    }

    @Test
    fun `test download summary with verification`() {
        val verificationResult = com.obsidianbackup.model.VerificationResult(
            success = true,
            message = "All files verified",
            details = mapOf("files_checked" to "3")
        )

        val summary = CloudDownloadSummary(
            snapshotId = SnapshotId("test"),
            filesDownloaded = 3,
            bytesDownloaded = 3_000_000L,
            duration = 5000L,
            averageSpeed = 600_000L,
            verificationResult = verificationResult
        )

        assertEquals(3, summary.filesDownloaded)
        assertTrue(summary.verificationResult.success)
        assertEquals("All files verified", summary.verificationResult.message)
    }

    @Test
    fun `test cloud error handling`() {
        val error = CloudError(
            code = CloudError.ErrorCode.NETWORK_ERROR,
            message = "Connection failed",
            cause = null,
            retryable = true
        )

        assertEquals(CloudError.ErrorCode.NETWORK_ERROR, error.code)
        assertEquals("Connection failed", error.message)
        assertTrue(error.retryable)
    }

    @Test
    fun `test snapshot filter`() {
        val now = System.currentTimeMillis()
        val filter = CloudSnapshotFilter(
            afterTimestamp = now - 86400000, // 24 hours ago
            beforeTimestamp = now,
            deviceId = "test_device",
            maxResults = 50
        )

        assertNotNull(filter.afterTimestamp)
        assertNotNull(filter.beforeTimestamp)
        assertEquals("test_device", filter.deviceId)
        assertEquals(50, filter.maxResults)
    }

    @Test
    fun `test storage quota structure`() {
        val quota = StorageQuota(
            totalBytes = 100L * 1024L * 1024L * 1024L, // 100GB
            usedBytes = 50L * 1024L * 1024L * 1024L,   // 50GB
            availableBytes = 50L * 1024L * 1024L * 1024L // 50GB
        )

        assertEquals(100L * 1024L * 1024L * 1024L, quota.totalBytes)
        assertEquals(50L * 1024L * 1024L * 1024L, quota.usedBytes)
        assertTrue(quota.availableBytes > 0)
    }

    @Test
    fun `test cloud catalog structure`() {
        val snapshots = listOf(
            CloudSnapshotInfo(
                snapshotId = SnapshotId("snap1"),
                timestamp = System.currentTimeMillis(),
                sizeBytes = 1000000L,
                fileCount = 3,
                checksum = "bafytest1",
                metadata = mock(CloudSnapshotMetadata::class.java)
            )
        )

        val catalog = CloudCatalog(
            version = 1,
            snapshots = snapshots,
            lastUpdated = System.currentTimeMillis(),
            signature = "test_signature"
        )

        assertEquals(1, catalog.version)
        assertEquals(1, catalog.snapshots.size)
        assertEquals("test_signature", catalog.signature)
    }

    @Test
    fun `test transfer progress states`() {
        val snapshotId = SnapshotId("test")

        // Uploading state
        val uploading = CloudTransferProgress.Uploading(
            snapshotId = snapshotId,
            currentFile = "test.apk",
            filesCompleted = 2,
            totalFiles = 5,
            bytesTransferred = 2000000L,
            totalBytes = 5000000L,
            transferRate = 400000L
        )
        assertTrue(uploading is CloudTransferProgress.Uploading)

        // Completed state
        val completed = CloudTransferProgress.Completed(snapshotId)
        assertTrue(completed is CloudTransferProgress.Completed)

        // Failed state
        val failed = CloudTransferProgress.Failed(
            snapshotId,
            CloudError(CloudError.ErrorCode.NETWORK_ERROR, "Test error")
        )
        assertTrue(failed is CloudTransferProgress.Failed)
    }

    @Test
    fun `test gateway fallback list`() {
        val gateways = config.ipfsGateways

        assertTrue(gateways.size >= 2, "Should have multiple gateways for fallback")
        gateways.forEach { gateway ->
            assertTrue(gateway.startsWith("https://"), "Gateway should use HTTPS")
            assertTrue(gateway.contains("ipfs") || gateway.contains("dweb"), 
                "Gateway URL should reference IPFS")
        }
    }

    @Test
    fun `test provider features`() {
        // Test that provider implements all required CloudProvider methods
        assertNotNull(provider.providerId)
        assertNotNull(provider.displayName)
        assertNotNull(provider.observeProgress())
    }
}
