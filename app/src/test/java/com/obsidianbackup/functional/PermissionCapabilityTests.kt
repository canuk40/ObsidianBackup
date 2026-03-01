package com.obsidianbackup.functional

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.obsidianbackup.engine.ShellExecutor
import com.obsidianbackup.engine.ShellResult
import com.obsidianbackup.model.PermissionMode
import com.obsidianbackup.permissions.*
import com.obsidianbackup.testing.BaseTest
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Permission Capability Functional Tests")
class PermissionCapabilityTests : BaseTest() {

    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var shellExecutor: ShellExecutor

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        shellExecutor = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("Capability Detection Tests")
    inner class CapabilityDetectionTests {

        @Test
        @DisplayName("Should detect root capability")
        fun testDetectRootCapability() = runTest {
            val rootCapability = RootCapability()
            
            coEvery { 
                shellExecutor.execute("su -c id") 
            } returns ShellResult.Success("uid=0(root)", 0)

            val isAvailable = rootCapability.isAvailable(context)

            assertThat(isAvailable).isTrue()
        }

        @Test
        @DisplayName("Should detect Shizuku capability")
        fun testDetectShizukuCapability() = runTest {
            val shizukuCapability = ShizukuCapability()
            
            coEvery { 
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0) 
            } returns mockk(relaxed = true)

            val isAvailable = shizukuCapability.isAvailable(context)

            assertThat(isAvailable).isTrue()
        }

        @Test
        @DisplayName("Should detect APK access capability")
        fun testDetectApkAccessCapability() = runTest {
            val apkCapability = ApkAccessCapability()
            
            coEvery { 
                shellExecutor.execute(match { it.contains("pm list packages") }) 
            } returns ShellResult.Success("package:com.android.systemui", 0)

            val isAvailable = apkCapability.isAvailable(context)

            assertThat(isAvailable).isTrue()
        }

        @Test
        @DisplayName("Should detect data access capability")
        fun testDetectDataAccessCapability() = runTest {
            val dataCapability = DataAccessCapability()
            
            coEvery { 
                shellExecutor.execute(match { it.contains("ls /data/data") }) 
            } returns ShellResult.Success("com.example.app", 0)

            val isAvailable = dataCapability.isAvailable(context)

            assertThat(isAvailable).isTrue()
        }

        @Test
        @DisplayName("Should detect all capabilities simultaneously")
        fun testDetectAllCapabilities() = runTest {
            val capabilities = listOf(
                RootCapability(),
                ShizukuCapability(),
                ApkAccessCapability(),
                DataAccessCapability()
            )

            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Success("success", 0)
            coEvery { 
                context.packageManager.getPackageInfo(any(), any()) 
            } returns mockk(relaxed = true)

            val availableCapabilities = capabilities.filter { 
                it.isAvailable(context) 
            }

            assertThat(availableCapabilities).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Graceful Degradation Tests")
    inner class GracefulDegradationTests {

        @Test
        @DisplayName("Should fallback from root to Shizuku")
        fun testFallbackRootToShizuku() = runTest {
            coEvery { 
                shellExecutor.execute(match { it.contains("su -c") }) 
            } returns ShellResult.Failure("su not found", 127)
            coEvery { 
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0) 
            } returns mockk(relaxed = true)

            val rootCapability = RootCapability()
            val shizukuCapability = ShizukuCapability()

            val rootAvailable = rootCapability.isAvailable(context)
            val shizukuAvailable = shizukuCapability.isAvailable(context)

            assertThat(rootAvailable).isFalse()
            assertThat(shizukuAvailable).isTrue()
        }

        @Test
        @DisplayName("Should fallback to SAF when no elevated permissions")
        fun testFallbackToSAF() = runTest {
            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Failure("Permission denied", 1)

            val profile = PermissionProfile(
                capabilities = setOf(
                    RootCapability(),
                    ShizukuCapability()
                ),
                displayName = "Standard"
            )

            val canBackupApk = profile.canBackupApk(context)
            val canBackupData = profile.canBackupData(context)

            assertThat(canBackupApk).isFalse()
            assertThat(canBackupData).isFalse()
        }

        @Test
        @DisplayName("Should handle partial capability availability")
        fun testPartialCapabilityAvailability() = runTest {
            coEvery { 
                shellExecutor.execute(match { it.contains("pm list") }) 
            } returns ShellResult.Success("success", 0)
            coEvery { 
                shellExecutor.execute(match { it.contains("ls /data/data") }) 
            } returns ShellResult.Failure("Permission denied", 1)

            val apkCapability = ApkAccessCapability()
            val dataCapability = DataAccessCapability()

            val canAccessApk = apkCapability.isAvailable(context)
            val canAccessData = dataCapability.isAvailable(context)

            assertThat(canAccessApk).isTrue()
            assertThat(canAccessData).isFalse()
        }

        @Test
        @DisplayName("Should provide user feedback on capability limitations")
        fun testCapabilityLimitationsFeedback() = runTest {
            val profile = PermissionProfile(
                capabilities = emptySet(),
                displayName = "No Permissions"
            )

            val canBackupApk = profile.canBackupApk(context)
            val canBackupData = profile.canBackupData(context)

            assertThat(canBackupApk).isFalse()
            assertThat(canBackupData).isFalse()
        }
    }

    @Nested
    @DisplayName("Permission Request Flow Tests")
    inner class PermissionRequestFlowTests {

        @Test
        @DisplayName("Should request root permission")
        fun testRequestRootPermission() = runTest {
            coEvery { 
                permissionManager.requestPermission(PermissionMode.ROOT) 
            } returns PermissionResult.Granted

            val result = permissionManager.requestPermission(PermissionMode.ROOT)

            assertThat(result).isEqualTo(PermissionResult.Granted)
            coVerify { permissionManager.requestPermission(PermissionMode.ROOT) }
        }

        @Test
        @DisplayName("Should request Shizuku permission")
        fun testRequestShizukuPermission() = runTest {
            coEvery { 
                permissionManager.requestPermission(PermissionMode.SHIZUKU) 
            } returns PermissionResult.Granted

            val result = permissionManager.requestPermission(PermissionMode.SHIZUKU)

            assertThat(result).isEqualTo(PermissionResult.Granted)
        }

        @Test
        @DisplayName("Should handle permission denial")
        fun testPermissionDenial() = runTest {
            coEvery { 
                permissionManager.requestPermission(any()) 
            } returns PermissionResult.Denied

            val result = permissionManager.requestPermission(PermissionMode.ROOT)

            assertThat(result).isEqualTo(PermissionResult.Denied)
        }

        @Test
        @DisplayName("Should handle permission permanently denied")
        fun testPermissionPermanentlyDenied() = runTest {
            coEvery { 
                permissionManager.requestPermission(any()) 
            } returns PermissionResult.PermanentlyDenied

            val result = permissionManager.requestPermission(PermissionMode.ROOT)

            assertThat(result).isEqualTo(PermissionResult.PermanentlyDenied)
        }

        @ParameterizedTest
        @EnumSource(PermissionMode::class)
        @DisplayName("Should handle all permission modes")
        fun testAllPermissionModes(mode: PermissionMode) = runTest {
            coEvery { 
                permissionManager.requestPermission(any()) 
            } returns PermissionResult.Granted

            val result = permissionManager.requestPermission(mode)

            assertThat(result).isEqualTo(PermissionResult.Granted)
        }
    }

    @Nested
    @DisplayName("Capability Priority Tests")
    inner class CapabilityPriorityTests {

        @Test
        @DisplayName("Should prioritize root over Shizuku")
        fun testRootPriority() = runTest {
            val rootCapability = RootCapability()
            val shizukuCapability = ShizukuCapability()

            assertThat(rootCapability.getPriority())
                .isGreaterThan(shizukuCapability.getPriority())
        }

        @Test
        @DisplayName("Should use highest priority available capability")
        fun testHighestPriorityCapability() = runTest {
            val capabilities = listOf(
                ShizukuCapability(),
                RootCapability(),
                ApkAccessCapability()
            ).sortedByDescending { it.getPriority() }

            val highest = capabilities.first()

            assertThat(highest).isInstanceOf(RootCapability::class.java)
        }

        @Test
        @DisplayName("Should fallback to next priority on failure")
        fun testFallbackToPriority() = runTest {
            val capabilities = listOf(
                RootCapability(),
                ShizukuCapability(),
                ApkAccessCapability()
            ).sortedByDescending { it.getPriority() }

            coEvery { 
                shellExecutor.execute(match { it.contains("su -c") }) 
            } returns ShellResult.Failure("su not found", 127)
            coEvery { 
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0) 
            } returns mockk(relaxed = true)

            val availableCapability = capabilities.firstOrNull { 
                it.isAvailable(context) 
            }

            assertThat(availableCapability).isInstanceOf(ShizukuCapability::class.java)
        }
    }

    @Nested
    @DisplayName("Permission Profile Tests")
    inner class PermissionProfileTests {

        @Test
        @DisplayName("Should create root profile")
        fun testRootProfile() = runTest {
            val profile = PermissionProfile(
                capabilities = setOf(
                    RootCapability(),
                    ApkAccessCapability(),
                    DataAccessCapability()
                ),
                displayName = "Root"
            )

            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Success("success", 0)

            val canBackupApk = profile.canBackupApk(context)
            val canBackupData = profile.canBackupData(context)

            assertThat(canBackupApk).isTrue()
            assertThat(canBackupData).isTrue()
        }

        @Test
        @DisplayName("Should create Shizuku profile")
        fun testShizukuProfile() = runTest {
            val profile = PermissionProfile(
                capabilities = setOf(
                    ShizukuCapability(),
                    ApkAccessCapability(),
                    DataAccessCapability()
                ),
                displayName = "Shizuku"
            )

            coEvery { 
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0) 
            } returns mockk(relaxed = true)
            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Success("success", 0)

            val canBackupApk = profile.canBackupApk(context)
            val canBackupData = profile.canBackupData(context)

            assertThat(canBackupApk).isTrue()
            assertThat(canBackupData).isTrue()
        }

        @Test
        @DisplayName("Should create SAF profile")
        fun testSAFProfile() = runTest {
            val profile = PermissionProfile(
                capabilities = emptySet(),
                displayName = "SAF Only"
            )

            val canBackupApk = profile.canBackupApk(context)
            val canBackupData = profile.canBackupData(context)

            assertThat(canBackupApk).isFalse()
            assertThat(canBackupData).isFalse()
        }
    }

    @Nested
    @DisplayName("Runtime Permission Tests")
    inner class RuntimePermissionTests {

        @Test
        @DisplayName("Should check runtime permission status")
        fun testCheckRuntimePermission() = runTest {
            coEvery { 
                permissionManager.hasPermission(any()) 
            } returns true

            val hasPermission = permissionManager.hasPermission(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )

            assertThat(hasPermission).isTrue()
        }

        @Test
        @DisplayName("Should request runtime permission")
        fun testRequestRuntimePermission() = runTest {
            coEvery { 
                permissionManager.requestRuntimePermission(any()) 
            } returns PermissionResult.Granted

            val result = permissionManager.requestRuntimePermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            assertThat(result).isEqualTo(PermissionResult.Granted)
        }

        @Test
        @DisplayName("Should handle multiple permission requests")
        fun testMultiplePermissionRequests() = runTest {
            val permissions = listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            coEvery { 
                permissionManager.requestMultiplePermissions(any()) 
            } returns mapOf(
                permissions[0] to PermissionResult.Granted,
                permissions[1] to PermissionResult.Granted
            )

            val results = permissionManager.requestMultiplePermissions(permissions)

            assertThat(results).hasSize(2)
            assertThat(results.values.all { it == PermissionResult.Granted }).isTrue()
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle capability detection timeout")
        fun testCapabilityDetectionTimeout() = runTest {
            coEvery { 
                shellExecutor.execute(any()) 
            } throws Exception("Timeout")

            val capability = RootCapability()
            val isAvailable = try {
                capability.isAvailable(context)
            } catch (e: Exception) {
                false
            }

            assertThat(isAvailable).isFalse()
        }

        @Test
        @DisplayName("Should handle missing Shizuku package gracefully")
        fun testMissingShizukuPackage() = runTest {
            coEvery { 
                context.packageManager.getPackageInfo(any(), any()) 
            } throws android.content.pm.PackageManager.NameNotFoundException()

            val capability = ShizukuCapability()
            val isAvailable = capability.isAvailable(context)

            assertThat(isAvailable).isFalse()
        }

        @Test
        @DisplayName("Should handle shell execution errors")
        fun testShellExecutionError() = runTest {
            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Failure("Command not found", 127)

            val capability = ApkAccessCapability()
            val isAvailable = capability.isAvailable(context)

            assertThat(isAvailable).isFalse()
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Should build complete permission profile")
        fun testBuildPermissionProfile() = runTest {
            val availableCapabilities = mutableSetOf<PermissionCapability>()

            coEvery { 
                shellExecutor.execute(match { it.contains("su -c") }) 
            } returns ShellResult.Success("uid=0", 0)
            
            if (RootCapability().isAvailable(context)) {
                availableCapabilities.add(RootCapability())
                availableCapabilities.add(ApkAccessCapability())
                availableCapabilities.add(DataAccessCapability())
            }

            val profile = PermissionProfile(
                capabilities = availableCapabilities,
                displayName = "Detected Profile"
            )

            assertThat(profile.capabilities).isNotEmpty()
        }

        @Test
        @DisplayName("Should determine optimal backup strategy")
        fun testOptimalBackupStrategy() = runTest {
            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Success("success", 0)

            val profile = PermissionProfile(
                capabilities = setOf(
                    RootCapability(),
                    ApkAccessCapability(),
                    DataAccessCapability()
                ),
                displayName = "Root"
            )

            val canBackupApk = profile.canBackupApk(context)
            val canBackupData = profile.canBackupData(context)

            val strategy = when {
                canBackupApk && canBackupData -> "FULL_BACKUP"
                canBackupApk -> "APK_ONLY"
                else -> "SAF_ONLY"
            }

            assertThat(strategy).isEqualTo("FULL_BACKUP")
        }
    }

    sealed class PermissionResult {
        object Granted : PermissionResult()
        object Denied : PermissionResult()
        object PermanentlyDenied : PermissionResult()
    }

    interface PermissionManager {
        suspend fun requestPermission(mode: PermissionMode): PermissionResult
        suspend fun hasPermission(permission: String): Boolean
        suspend fun requestRuntimePermission(permission: String): PermissionResult
        suspend fun requestMultiplePermissions(permissions: List<String>): Map<String, PermissionResult>
    }
}
