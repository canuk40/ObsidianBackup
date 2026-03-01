package com.obsidianbackup.functional

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.obsidianbackup.automation.BackupScheduler
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.builtin.DefaultAutomationPlugin
import com.obsidianbackup.plugins.interfaces.TriggerCondition
import com.obsidianbackup.plugins.interfaces.TriggerEvent
import com.obsidianbackup.plugins.interfaces.TriggerType
import com.obsidianbackup.testing.BaseTest
import com.obsidianbackup.testing.TestFixtures
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.time.Duration.Companion.seconds

@DisplayName("Automation Functional Tests")
class AutomationFunctionalTests : BaseTest() {

    private lateinit var automationPlugin: DefaultAutomationPlugin
    private lateinit var context: Context
    private lateinit var backupOrchestrator: BackupOrchestrator
    private lateinit var backupScheduler: BackupScheduler
    private lateinit var logger: ObsidianLogger

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        backupOrchestrator = mockk(relaxed = true)
        backupScheduler = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        automationPlugin = DefaultAutomationPlugin(
            context = context,
            backupOrchestrator = backupOrchestrator,
            backupScheduler = backupScheduler,
            logger = logger
        )
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("Default Automation Plugin Tests")
    inner class DefaultAutomationPluginTests {

        @Test
        @DisplayName("Should initialize plugin successfully")
        fun testPluginInitialization() = runTest {
            automationPlugin.initialize()

            coVerify { logger.i(any(), match { it.contains("initialized") }) }
        }

        @Test
        @DisplayName("Should register trigger successfully")
        fun testTriggerRegistration() = runTest {
            val triggerId = automationPlugin.registerTrigger(
                type = TriggerType.SCHEDULED,
                condition = TriggerCondition.Time(hour = 2, minute = 0),
                action = { backupOrchestrator.executeBackup(mockk(relaxed = true)) }
            )

            assertThat(triggerId).isNotEmpty()
            coVerify { logger.i(any(), match { it.contains("Registered trigger") }) }
        }

        @Test
        @DisplayName("Should unregister trigger successfully")
        fun testTriggerUnregistration() = runTest {
            val triggerId = automationPlugin.registerTrigger(
                type = TriggerType.SCHEDULED,
                condition = TriggerCondition.Time(hour = 2, minute = 0),
                action = {}
            )

            automationPlugin.unregisterTrigger(triggerId)

            coVerify { logger.i(any(), match { it.contains("Unregistered trigger") }) }
        }

        @Test
        @DisplayName("Should list all active triggers")
        fun testListActiveTriggers() = runTest {
            val trigger1 = automationPlugin.registerTrigger(
                type = TriggerType.SCHEDULED,
                condition = TriggerCondition.Time(hour = 2, minute = 0),
                action = {}
            )
            val trigger2 = automationPlugin.registerTrigger(
                type = TriggerType.ON_APP_INSTALL,
                condition = TriggerCondition.Always,
                action = {}
            )

            val triggers = automationPlugin.listTriggers()

            assertThat(triggers).hasSize(2)
            assertThat(triggers.map { it.id }).containsExactly(trigger1, trigger2)
        }
    }

    @Nested
    @DisplayName("App Install Detection Tests")
    inner class AppInstallDetectionTests {

        @Test
        @DisplayName("Should trigger backup on app install")
        fun testBackupOnAppInstall() = runTest {
            val packageName = "com.test.newapp"
            var backupTriggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_APP_INSTALL,
                condition = TriggerCondition.PackageName(packageName),
                action = { backupTriggered = true }
            )

            automationPlugin.onAppInstalled(packageName)

            assertThat(backupTriggered).isTrue()
        }

        @Test
        @DisplayName("Should trigger backup on app update")
        fun testBackupOnAppUpdate() = runTest {
            val packageName = "com.test.existingapp"
            var backupTriggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_APP_UPDATE,
                condition = TriggerCondition.PackageName(packageName),
                action = { backupTriggered = true }
            )

            automationPlugin.onAppUpdated(packageName)

            assertThat(backupTriggered).isTrue()
        }

        @Test
        @DisplayName("Should not trigger on system app install")
        fun testNoTriggerOnSystemApp() = runTest {
            val systemPackage = "com.android.systemui"
            var backupTriggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_APP_INSTALL,
                condition = TriggerCondition.ExcludeSystemApps,
                action = { backupTriggered = true }
            )

            automationPlugin.onAppInstalled(systemPackage)

            assertThat(backupTriggered).isFalse()
        }

        @Test
        @DisplayName("Should trigger on multiple app installs")
        fun testMultipleAppInstalls() = runTest {
            val installedApps = mutableListOf<String>()

            automationPlugin.registerTrigger(
                type = TriggerType.ON_APP_INSTALL,
                condition = TriggerCondition.Always,
                action = { installedApps.add("triggered") }
            )

            repeat(5) { i ->
                automationPlugin.onAppInstalled("com.test.app$i")
            }

            assertThat(installedApps).hasSize(5)
        }
    }

    @Nested
    @DisplayName("Scheduled Backup Tests")
    inner class ScheduledBackupTests {

        @Test
        @DisplayName("Should schedule nightly backup")
        fun testNightlyBackup() = runTest {
            automationPlugin.scheduleNightlyBackup(hour = 2, minute = 0)

            coVerify { 
                backupScheduler.scheduleBackup(
                    match { it.contains("nightly") },
                    any(),
                    any()
                ) 
            }
        }

        @Test
        @DisplayName("Should schedule weekly backup")
        fun testWeeklyBackup() = runTest {
            automationPlugin.scheduleWeeklyBackup(
                dayOfWeek = 1,
                hour = 3,
                minute = 0
            )

            coVerify { 
                backupScheduler.scheduleBackup(
                    match { it.contains("weekly") },
                    any(),
                    any()
                ) 
            }
        }

        @Test
        @DisplayName("Should cancel scheduled backup")
        fun testCancelScheduledBackup() = runTest {
            val scheduleId = automationPlugin.scheduleNightlyBackup(hour = 2, minute = 0)

            automationPlugin.cancelScheduledBackup(scheduleId)

            coVerify { backupScheduler.cancelBackup(scheduleId) }
        }

        @Test
        @DisplayName("Should reschedule backup after boot")
        fun testRescheduleAfterBoot() = runTest {
            automationPlugin.scheduleNightlyBackup(hour = 2, minute = 0)

            automationPlugin.onDeviceBoot()

            coVerify(atLeast = 1) { 
                backupScheduler.scheduleBackup(any(), any(), any()) 
            }
        }
    }

    @Nested
    @DisplayName("Tasker Integration Tests")
    inner class TaskerIntegrationTests {

        @Test
        @DisplayName("Should execute backup via Tasker action")
        fun testTaskerBackupAction() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            
            coEvery { 
                backupOrchestrator.executeBackup(any()) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L,
                duration = 1000L
            )

            val result = automationPlugin.executeTaskerAction(
                action = "backup",
                parameters = mapOf("apps" to appIds.map { it.value })
            )

            assertThat(result.success).isTrue()
            coVerify { backupOrchestrator.executeBackup(any()) }
        }

        @Test
        @DisplayName("Should query backup status via Tasker")
        fun testTaskerStatusQuery() = runTest {
            val snapshotId = SnapshotId(TestFixtures.randomUUID())

            val result = automationPlugin.executeTaskerAction(
                action = "query_status",
                parameters = mapOf("snapshot_id" to snapshotId.value)
            )

            assertThat(result.success).isTrue()
            assertThat(result.data).containsKey("status")
        }

        @Test
        @DisplayName("Should handle Tasker variable substitution")
        fun testTaskerVariableSubstitution() = runTest {
            val result = automationPlugin.executeTaskerAction(
                action = "backup",
                parameters = mapOf(
                    "apps" to "%apps_to_backup",
                    "path" to "%backup_path"
                )
            )

            assertThat(result.success).isTrue()
        }
    }

    @Nested
    @DisplayName("Trigger Condition Tests")
    inner class TriggerConditionTests {

        @Test
        @DisplayName("Should trigger on battery level condition")
        fun testBatteryLevelCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_BATTERY_LEVEL,
                condition = TriggerCondition.BatteryLevel(minLevel = 80),
                action = { triggered = true }
            )

            automationPlugin.onBatteryLevelChanged(85)

            assertThat(triggered).isTrue()
        }

        @Test
        @DisplayName("Should trigger on WiFi connected condition")
        fun testWiFiConnectedCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_WIFI_CONNECTED,
                condition = TriggerCondition.NetworkType("WIFI"),
                action = { triggered = true }
            )

            automationPlugin.onNetworkChanged(networkType = "WIFI", connected = true)

            assertThat(triggered).isTrue()
        }

        @Test
        @DisplayName("Should trigger on charging condition")
        fun testChargingCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_CHARGING,
                condition = TriggerCondition.Always,
                action = { triggered = true }
            )

            automationPlugin.onChargingStateChanged(isCharging = true)

            assertThat(triggered).isTrue()
        }

        @Test
        @DisplayName("Should trigger on storage threshold condition")
        fun testStorageThresholdCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.ON_STORAGE_THRESHOLD,
                condition = TriggerCondition.StorageAvailable(minBytes = 1024L * 1024L * 1024L),
                action = { triggered = true }
            )

            automationPlugin.onStorageChanged(availableBytes = 2L * 1024L * 1024L * 1024L)

            assertThat(triggered).isTrue()
        }

        @ParameterizedTest
        @EnumSource(TriggerType::class)
        @DisplayName("Should handle all trigger types")
        fun testAllTriggerTypes(triggerType: TriggerType) = runTest {
            var triggered = false

            val triggerId = automationPlugin.registerTrigger(
                type = triggerType,
                condition = TriggerCondition.Always,
                action = { triggered = true }
            )

            assertThat(triggerId).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Complex Condition Tests")
    inner class ComplexConditionTests {

        @Test
        @DisplayName("Should trigger on AND condition")
        fun testAndCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.And(
                    TriggerCondition.BatteryLevel(minLevel = 80),
                    TriggerCondition.NetworkType("WIFI")
                ),
                action = { triggered = true }
            )

            automationPlugin.evaluateTriggerConditions(
                batteryLevel = 85,
                networkType = "WIFI"
            )

            assertThat(triggered).isTrue()
        }

        @Test
        @DisplayName("Should trigger on OR condition")
        fun testOrCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.Or(
                    TriggerCondition.BatteryLevel(minLevel = 80),
                    TriggerCondition.NetworkType("WIFI")
                ),
                action = { triggered = true }
            )

            automationPlugin.evaluateTriggerConditions(
                batteryLevel = 50,
                networkType = "WIFI"
            )

            assertThat(triggered).isTrue()
        }

        @Test
        @DisplayName("Should not trigger on failed AND condition")
        fun testFailedAndCondition() = runTest {
            var triggered = false

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.And(
                    TriggerCondition.BatteryLevel(minLevel = 80),
                    TriggerCondition.NetworkType("WIFI")
                ),
                action = { triggered = true }
            )

            automationPlugin.evaluateTriggerConditions(
                batteryLevel = 50,
                networkType = "WIFI"
            )

            assertThat(triggered).isFalse()
        }
    }

    @Nested
    @DisplayName("Event Propagation Tests")
    inner class EventPropagationTests {

        @Test
        @DisplayName("Should propagate trigger events to listeners")
        fun testEventPropagation() = runTest {
            val events = mutableListOf<TriggerEvent>()

            automationPlugin.observeTriggerEvents().collect { event ->
                events.add(event)
            }

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.Always,
                action = {}
            )

            assertThat(events).isNotEmpty()
        }

        @Test
        @DisplayName("Should handle multiple event listeners")
        fun testMultipleEventListeners() = runTest {
            val listener1Events = mutableListOf<TriggerEvent>()
            val listener2Events = mutableListOf<TriggerEvent>()

            automationPlugin.observeTriggerEvents().collect { event ->
                listener1Events.add(event)
                listener2Events.add(event)
            }

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.Always,
                action = {}
            )

            assertThat(listener1Events.size).isEqualTo(listener2Events.size)
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle trigger action failure gracefully")
        fun testTriggerActionFailure() = runTest {
            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.Always,
                action = { throw RuntimeException("Action failed") }
            )

            automationPlugin.executeTrigger("trigger_id")

            coVerify { 
                logger.e(any(), match { it.contains("failed") }, any()) 
            }
        }

        @Test
        @DisplayName("Should continue executing other triggers on failure")
        fun testContinueOnFailure() = runTest {
            var trigger2Executed = false

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.Always,
                action = { throw RuntimeException("Fail") }
            )

            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.Always,
                action = { trigger2Executed = true }
            )

            automationPlugin.executeAllTriggers()

            assertThat(trigger2Executed).isTrue()
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle many triggers efficiently")
        fun testManyTriggers() = runTest {
            val triggerCount = 100
            val triggers = mutableListOf<String>()

            repeat(triggerCount) { i ->
                val triggerId = automationPlugin.registerTrigger(
                    type = TriggerType.MANUAL,
                    condition = TriggerCondition.Always,
                    action = {}
                )
                triggers.add(triggerId)
            }

            assertThat(triggers).hasSize(triggerCount)
            assertThat(automationPlugin.listTriggers()).hasSize(triggerCount)
        }

        @Test
        @DisplayName("Should evaluate conditions quickly")
        fun testConditionEvaluationSpeed() = runTest {
            automationPlugin.registerTrigger(
                type = TriggerType.MANUAL,
                condition = TriggerCondition.And(
                    TriggerCondition.BatteryLevel(minLevel = 80),
                    TriggerCondition.NetworkType("WIFI"),
                    TriggerCondition.StorageAvailable(minBytes = 1024L * 1024L * 1024L)
                ),
                action = {}
            )

            val startTime = System.currentTimeMillis()
            repeat(1000) {
                automationPlugin.evaluateTriggerConditions(
                    batteryLevel = 85,
                    networkType = "WIFI",
                    storageAvailable = 2L * 1024L * 1024L * 1024L
                )
            }
            val duration = System.currentTimeMillis() - startTime

            assertThat(duration).isLessThan(1000L)
        }
    }
}
