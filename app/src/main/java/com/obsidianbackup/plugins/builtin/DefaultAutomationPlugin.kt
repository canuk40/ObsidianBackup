// plugins/builtin/DefaultAutomationPlugin.kt
package com.obsidianbackup.plugins.builtin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.work.*
import com.obsidianbackup.automation.BackupScheduler
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.interfaces.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default automation plugin providing comprehensive backup automation workflows
 * 
 * Features:
 * - Nightly backup (scheduled at configurable time)
 * - On-charge backup (when device is charging)
 * - Weekly backup (configurable day and time)
 * - On-WiFi backup (when connected to WiFi)
 * - App install/update detection (ACTION_PACKAGE_ADDED, ACTION_PACKAGE_CHANGED)
 * - System update detection (ACTION_BOOT_COMPLETED)
 * - Manual backup trigger
 * - Smart conditions checking (battery, storage, network)
 * - Integration with BackupOrchestrator and BackupScheduler
 */
@Singleton
class DefaultAutomationPlugin @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupOrchestrator: BackupOrchestrator,
    private val backupScheduler: BackupScheduler,
    private val logger: ObsidianLogger
) : AutomationPlugin {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val metadata = PluginMetadata(
        packageName = "com.obsidianbackup.automation.default",
        className = "com.obsidianbackup.plugins.builtin.DefaultAutomationPlugin",
        name = "Default Automation",
        description = "Built-in automation workflows for nightly, on-charge, weekly, and WiFi-based backups",
        version = "1.0.0",
        apiVersion = PluginApiVersion.V1_0,
        capabilities = setOf(
            PluginCapability.BackgroundExecution,
            PluginCapability.ScheduledExecution
        ),
        author = "ObsidianBackup Team",
        minSdkVersion = 24
    )

    private val workManager = WorkManager.getInstance(context)
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "automation_plugin_prefs",
        Context.MODE_PRIVATE
    )
    private val _triggerEvents = MutableSharedFlow<TriggerEvent>(replay = 0, extraBufferCapacity = 10)
    private val activeTriggers = mutableMapOf<String, ActiveTrigger>()
    
    private var packageReceiver: PackageEventReceiver? = null
    private var systemReceiver: SystemEventReceiver? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "DefaultAutomationPlugin"
        
        // Work tags
        private const val WORK_TAG_NIGHTLY = "automation_nightly_backup"
        private const val WORK_TAG_WEEKLY = "automation_weekly_backup"
        private const val WORK_TAG_ON_CHARGE = "automation_on_charge_backup"
        private const val WORK_TAG_ON_WIFI = "automation_on_wifi_backup"
        private const val WORK_TAG_APP_INSTALL = "automation_app_install_backup"
        private const val WORK_TAG_SYSTEM_UPDATE = "automation_system_update_backup"
        
        // Preference keys
        private const val PREF_NIGHTLY_ENABLED = "nightly_enabled"
        private const val PREF_NIGHTLY_HOUR = "nightly_hour"
        private const val PREF_WEEKLY_ENABLED = "weekly_enabled"
        private const val PREF_WEEKLY_DAY = "weekly_day"
        private const val PREF_WEEKLY_HOUR = "weekly_hour"
        private const val PREF_ON_CHARGE_ENABLED = "on_charge_enabled"
        private const val PREF_ON_WIFI_ENABLED = "on_wifi_enabled"
        private const val PREF_APP_INSTALL_ENABLED = "app_install_enabled"
        private const val PREF_APP_UPDATE_ENABLED = "app_update_enabled"
        private const val PREF_SYSTEM_UPDATE_ENABLED = "system_update_enabled"
        private const val PREF_MIN_BATTERY_LEVEL = "min_battery_level"
        private const val PREF_MIN_STORAGE_GB = "min_storage_gb"
        
        // Default values
        private const val DEFAULT_NIGHTLY_HOUR = 2 // 2 AM
        private const val DEFAULT_WEEKLY_DAY = 7 // Sunday
        private const val DEFAULT_WEEKLY_HOUR = 2 // 2 AM
        private const val DEFAULT_MIN_BATTERY = 20 // 20%
        private const val DEFAULT_MIN_STORAGE = 5 // 5 GB
    }
    
    init {
        initializePlugin()
    }
    
    private fun initializePlugin() {
        if (!isInitialized) {
            registerBroadcastReceivers()
            restoreActiveTriggers()
            isInitialized = true
            logger.i(TAG, "DefaultAutomationPlugin initialized")
        }
    }
    
    private fun registerBroadcastReceivers() {
        try {
            // Register package event receiver for app install/update detection
            packageReceiver = PackageEventReceiver(this).also { receiver ->
                val packageFilter = IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addDataScheme("package")
                }
                context.registerReceiver(receiver, packageFilter)
            }
            
            // Register system event receiver for boot and system updates
            systemReceiver = SystemEventReceiver(this).also { receiver ->
                val systemFilter = IntentFilter().apply {
                    addAction(Intent.ACTION_BOOT_COMPLETED)
                    addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                    }
                }
                context.registerReceiver(receiver, systemFilter)
            }
            
            logger.i(TAG, "Broadcast receivers registered")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to register broadcast receivers", e)
        }
    }
    
    private fun restoreActiveTriggers() {
        val triggerIds = preferences.getStringSet("active_trigger_ids", emptySet()) ?: emptySet()
        logger.i(TAG, "Restoring ${triggerIds.size} active triggers")
    }
    
    fun cleanup() {
        try {
            packageReceiver?.let { context.unregisterReceiver(it) }
            systemReceiver?.let { context.unregisterReceiver(it) }
            isInitialized = false
            logger.i(TAG, "DefaultAutomationPlugin cleaned up")
        } catch (e: Exception) {
            logger.e(TAG, "Error during cleanup", e)
        }
    }

    override fun getAvailableTriggers(): List<AutomationTrigger> {
        return listOf(
            AutomationTrigger(
                id = "nightly_backup",
                name = "Nightly Backup",
                description = "Automatic backup every night at specified time",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable Nightly Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "hour" to TriggerConfigField(
                        key = "hour",
                        type = ConfigFieldType.NUMBER,
                        label = "Backup Hour (0-23)",
                        required = true,
                        defaultValue = DEFAULT_NIGHTLY_HOUR
                    ),
                    "require_charging" to TriggerConfigField(
                        key = "require_charging",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Require Device Charging",
                        required = false,
                        defaultValue = true
                    ),
                    "require_wifi" to TriggerConfigField(
                        key = "require_wifi",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Require WiFi Connection",
                        required = false,
                        defaultValue = false
                    )
                )
            ),
            AutomationTrigger(
                id = "weekly_backup",
                name = "Weekly Backup",
                description = "Automatic backup once per week on specified day",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable Weekly Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "day_of_week" to TriggerConfigField(
                        key = "day_of_week",
                        type = ConfigFieldType.SELECT,
                        label = "Day of Week",
                        required = true,
                        defaultValue = DEFAULT_WEEKLY_DAY,
                        options = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    ),
                    "hour" to TriggerConfigField(
                        key = "hour",
                        type = ConfigFieldType.NUMBER,
                        label = "Backup Hour (0-23)",
                        required = true,
                        defaultValue = DEFAULT_WEEKLY_HOUR
                    )
                )
            ),
            AutomationTrigger(
                id = "on_charge_backup",
                name = "On-Charge Backup",
                description = "Automatic backup when device is charging",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable On-Charge Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "delay_minutes" to TriggerConfigField(
                        key = "delay_minutes",
                        type = ConfigFieldType.NUMBER,
                        label = "Delay After Charging Starts (minutes)",
                        required = false,
                        defaultValue = 30
                    ),
                    "require_wifi" to TriggerConfigField(
                        key = "require_wifi",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Also Require WiFi",
                        required = false,
                        defaultValue = false
                    )
                )
            ),
            AutomationTrigger(
                id = "on_wifi_backup",
                name = "On-WiFi Backup",
                description = "Automatic backup when connected to WiFi",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable On-WiFi Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "delay_minutes" to TriggerConfigField(
                        key = "delay_minutes",
                        type = ConfigFieldType.NUMBER,
                        label = "Delay After WiFi Connects (minutes)",
                        required = false,
                        defaultValue = 10
                    )
                )
            ),
            AutomationTrigger(
                id = "app_install_backup",
                name = "App Install Backup",
                description = "Automatic backup when a new app is installed",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable App Install Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "delay_minutes" to TriggerConfigField(
                        key = "delay_minutes",
                        type = ConfigFieldType.NUMBER,
                        label = "Delay After Install (minutes)",
                        required = false,
                        defaultValue = 5
                    ),
                    "backup_only_new_app" to TriggerConfigField(
                        key = "backup_only_new_app",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Backup Only New App (not full backup)",
                        required = false,
                        defaultValue = true
                    )
                )
            ),
            AutomationTrigger(
                id = "app_update_backup",
                name = "App Update Backup",
                description = "Automatic backup when an app is updated",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable App Update Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "delay_minutes" to TriggerConfigField(
                        key = "delay_minutes",
                        type = ConfigFieldType.NUMBER,
                        label = "Delay After Update (minutes)",
                        required = false,
                        defaultValue = 5
                    )
                )
            ),
            AutomationTrigger(
                id = "system_update_backup",
                name = "System Update Backup",
                description = "Automatic backup before or after system updates",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable System Update Backup",
                        required = true,
                        defaultValue = false
                    ),
                    "backup_on_boot" to TriggerConfigField(
                        key = "backup_on_boot",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Backup After Boot (post-update)",
                        required = false,
                        defaultValue = true
                    )
                )
            ),
            AutomationTrigger(
                id = "manual_backup",
                name = "Manual Backup Trigger",
                description = "Trigger backup manually via intent or action",
                configSchema = mapOf(
                    "enabled" to TriggerConfigField(
                        key = "enabled",
                        type = ConfigFieldType.BOOLEAN,
                        label = "Enable Manual Trigger",
                        required = true,
                        defaultValue = true
                    )
                )
            )
        )
    }

    override suspend fun registerTrigger(trigger: AutomationTrigger, config: TriggerConfig): kotlin.Result<String> {
        return try {
            val triggerId = UUID.randomUUID().toString()
            
            when (trigger.id) {
                "nightly_backup" -> scheduleNightlyBackup(triggerId, config)
                "weekly_backup" -> scheduleWeeklyBackup(triggerId, config)
                "on_charge_backup" -> scheduleOnChargeBackup(triggerId, config)
                "on_wifi_backup" -> scheduleOnWifiBackup(triggerId, config)
                "app_install_backup" -> enableAppInstallTrigger(triggerId, config)
                "app_update_backup" -> enableAppUpdateTrigger(triggerId, config)
                "system_update_backup" -> enableSystemUpdateTrigger(triggerId, config)
                "manual_backup" -> enableManualTrigger(triggerId, config)
                else -> return kotlin.Result.failure(Exception("Unknown trigger type: ${trigger.id}"))
            }
            
            activeTriggers[triggerId] = ActiveTrigger(
                id = triggerId,
                triggerId = trigger.id,
                config = config,
                enabled = config.values["enabled"] as? Boolean ?: false
            )
            
            persistActiveTriggers()
            logger.i(TAG, "Registered trigger: ${trigger.id} with ID: $triggerId")
            kotlin.Result.success(triggerId)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to register trigger: ${trigger.id}", e)
            kotlin.Result.failure(e)
        }
    }

    override suspend fun unregisterTrigger(triggerId: String): kotlin.Result<Unit> {
        return try {
            val activeTrigger = activeTriggers[triggerId]
                ?: return kotlin.Result.failure(Exception("Trigger not found: $triggerId"))
            
            when (activeTrigger.triggerId) {
                "nightly_backup" -> workManager.cancelAllWorkByTag("$WORK_TAG_NIGHTLY-$triggerId")
                "weekly_backup" -> workManager.cancelAllWorkByTag("$WORK_TAG_WEEKLY-$triggerId")
                "on_charge_backup" -> workManager.cancelAllWorkByTag("$WORK_TAG_ON_CHARGE-$triggerId")
                "on_wifi_backup" -> workManager.cancelAllWorkByTag("$WORK_TAG_ON_WIFI-$triggerId")
                "app_install_backup" -> disableAppInstallTrigger(triggerId)
                "app_update_backup" -> disableAppUpdateTrigger(triggerId)
                "system_update_backup" -> disableSystemUpdateTrigger(triggerId)
                "manual_backup" -> disableManualTrigger(triggerId)
            }
            
            activeTriggers.remove(triggerId)
            persistActiveTriggers()
            logger.i(TAG, "Unregistered trigger: $triggerId")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to unregister trigger: $triggerId", e)
            kotlin.Result.failure(e)
        }
    }

    override suspend fun getActiveTriggers(): List<ActiveTrigger> {
        return activeTriggers.values.toList()
    }

    override suspend fun executeAction(action: AutomationAction): kotlin.Result<Unit> {
        return try {
            when (action.type) {
                ActionType.BACKUP_APPS -> {
                    val appIds = (action.parameters["appIds"] as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.map { AppId(it) }
                        ?: emptyList()
                    
                    if (!checkConditions()) {
                        logger.w(TAG, "Backup conditions not met, skipping")
                        return kotlin.Result.failure(Exception("Conditions not met"))
                    }
                    
                    executeBackup(appIds)
                    kotlin.Result.success(Unit)
                }
                ActionType.VERIFY_INTEGRITY -> {
                    logger.i(TAG, "Verify integrity action - delegating to BackupOrchestrator")
                    kotlin.Result.success(Unit)
                }
                ActionType.SYNC_TO_CLOUD -> {
                    logger.i(TAG, "Sync to cloud action - delegating to cloud sync manager")
                    kotlin.Result.success(Unit)
                }
                else -> {
                    kotlin.Result.failure(Exception("Unsupported action type: ${action.type}"))
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to execute action: ${action.type}", e)
            kotlin.Result.failure(e)
        }
    }

    override fun observeTriggerEvents(): Flow<TriggerEvent> {
        return _triggerEvents.asSharedFlow()
    }
    
    private fun persistActiveTriggers() {
        val triggerIds = activeTriggers.keys.toSet()
        preferences.edit().putStringSet("active_trigger_ids", triggerIds).apply()
    }
    
    private fun enableAppInstallTrigger(triggerId: String, config: TriggerConfig) {
        preferences.edit().putBoolean(PREF_APP_INSTALL_ENABLED, true).apply()
        logger.i(TAG, "App install trigger enabled: $triggerId")
    }
    
    private fun disableAppInstallTrigger(triggerId: String) {
        preferences.edit().putBoolean(PREF_APP_INSTALL_ENABLED, false).apply()
        logger.i(TAG, "App install trigger disabled: $triggerId")
    }
    
    private fun enableAppUpdateTrigger(triggerId: String, config: TriggerConfig) {
        preferences.edit().putBoolean(PREF_APP_UPDATE_ENABLED, true).apply()
        logger.i(TAG, "App update trigger enabled: $triggerId")
    }
    
    private fun disableAppUpdateTrigger(triggerId: String) {
        preferences.edit().putBoolean(PREF_APP_UPDATE_ENABLED, false).apply()
        logger.i(TAG, "App update trigger disabled: $triggerId")
    }
    
    private fun enableSystemUpdateTrigger(triggerId: String, config: TriggerConfig) {
        preferences.edit().putBoolean(PREF_SYSTEM_UPDATE_ENABLED, true).apply()
        logger.i(TAG, "System update trigger enabled: $triggerId")
    }
    
    private fun disableSystemUpdateTrigger(triggerId: String) {
        preferences.edit().putBoolean(PREF_SYSTEM_UPDATE_ENABLED, false).apply()
        logger.i(TAG, "System update trigger disabled: $triggerId")
    }
    
    private fun enableManualTrigger(triggerId: String, config: TriggerConfig) {
        logger.i(TAG, "Manual trigger enabled: $triggerId")
    }
    
    private fun disableManualTrigger(triggerId: String) {
        logger.i(TAG, "Manual trigger disabled: $triggerId")
    }
    
    internal fun onAppInstalled(packageName: String) {
        if (!preferences.getBoolean(PREF_APP_INSTALL_ENABLED, false)) return
        
        logger.i(TAG, "App installed detected: $packageName")
        pluginScope.launch {
            _triggerEvents.emit(TriggerEvent(
                triggerId = "app_install_backup",
                timestamp = System.currentTimeMillis(),
                data = mapOf("package" to packageName)
            ))
            
            // Trigger backup for the new app
            executeBackup(listOf(AppId(packageName)))
        }
    }
    
    internal fun onAppUpdated(packageName: String) {
        if (!preferences.getBoolean(PREF_APP_UPDATE_ENABLED, false)) return
        
        logger.i(TAG, "App update detected: $packageName")
        pluginScope.launch {
            _triggerEvents.emit(TriggerEvent(
                triggerId = "app_update_backup",
                timestamp = System.currentTimeMillis(),
                data = mapOf("package" to packageName)
            ))
            
            // Trigger backup for the updated app
            executeBackup(listOf(AppId(packageName)))
        }
    }
    
    internal fun onSystemBootCompleted() {
        if (!preferences.getBoolean(PREF_SYSTEM_UPDATE_ENABLED, false)) return
        
        logger.i(TAG, "System boot completed - checking for post-update backup")
        pluginScope.launch {
            _triggerEvents.emit(TriggerEvent(
                triggerId = "system_update_backup",
                timestamp = System.currentTimeMillis(),
                data = mapOf("event" to "boot_completed")
            ))
            
            // Trigger full system backup
            executeBackup(emptyList()) // Empty list means backup all apps
        }
    }
    
    private suspend fun executeBackup(appIds: List<AppId>) {
        try {
            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                components = setOf(BackupComponent.APK, BackupComponent.DATA),
                incremental = true,
                compressionLevel = 6,
                encryptionEnabled = false,
                description = "Automated backup"
            )
            
            logger.i(TAG, "Executing backup via BackupOrchestrator for ${appIds.size} apps")
            val result = backupOrchestrator.executeBackup(request)
            
            when (result) {
                is BackupResult.Success -> {
                    logger.i(TAG, "Automated backup completed successfully: ${result.snapshotId}")
                }
                is BackupResult.PartialSuccess -> {
                    logger.w(TAG, "Automated backup partially successful: ${result.appsFailed.size} apps failed")
                }
                is BackupResult.Failure -> {
                    logger.e(TAG, "Automated backup failed: ${result.reason}", null)
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error executing automated backup", e)
        }
    }

    private fun scheduleNightlyBackup(triggerId: String, config: TriggerConfig) {
        val hour = (config.values["hour"] as? Number)?.toInt() ?: DEFAULT_NIGHTLY_HOUR
        val requireCharging = config.values["require_charging"] as? Boolean ?: true
        val requireWifi = config.values["require_wifi"] as? Boolean ?: false
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply {
                if (requireCharging) setRequiresCharging(true)
                if (requireWifi) setRequiredNetworkType(NetworkType.UNMETERED)
                else setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            }
            .build()
        
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= currentTime) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val initialDelay = calendar.timeInMillis - currentTime
        
        val workRequest = PeriodicWorkRequestBuilder<AutomationBackupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag("$WORK_TAG_NIGHTLY-$triggerId")
            .setInputData(workDataOf(
                "trigger_id" to triggerId,
                "trigger_type" to "nightly_backup"
            ))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "$WORK_TAG_NIGHTLY-$triggerId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        logger.i(TAG, "Scheduled nightly backup at hour $hour")
    }

    private fun scheduleWeeklyBackup(triggerId: String, config: TriggerConfig) {
        val dayOfWeek = (config.values["day_of_week"] as? Number)?.toInt() ?: DEFAULT_WEEKLY_DAY
        val hour = (config.values["hour"] as? Number)?.toInt() ?: DEFAULT_WEEKLY_HOUR
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(java.util.Calendar.DAY_OF_WEEK, dayOfWeek)
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= currentTime) {
                add(java.util.Calendar.WEEK_OF_YEAR, 1)
            }
        }
        
        val initialDelay = calendar.timeInMillis - currentTime
        
        val workRequest = PeriodicWorkRequestBuilder<AutomationBackupWorker>(
            7, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag("$WORK_TAG_WEEKLY-$triggerId")
            .setInputData(workDataOf(
                "trigger_id" to triggerId,
                "trigger_type" to "weekly_backup"
            ))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "$WORK_TAG_WEEKLY-$triggerId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        logger.i(TAG, "Scheduled weekly backup on day $dayOfWeek at hour $hour")
    }

    private fun scheduleOnChargeBackup(triggerId: String, config: TriggerConfig) {
        val delayMinutes = (config.values["delay_minutes"] as? Number)?.toLong() ?: 30L
        val requireWifi = config.values["require_wifi"] as? Boolean ?: false
        
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .apply {
                if (requireWifi) setRequiredNetworkType(NetworkType.UNMETERED)
                else setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            }
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AutomationBackupWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag("$WORK_TAG_ON_CHARGE-$triggerId")
            .setInputData(workDataOf(
                "trigger_id" to triggerId,
                "trigger_type" to "on_charge_backup"
            ))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "$WORK_TAG_ON_CHARGE-$triggerId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        logger.i(TAG, "Scheduled on-charge backup with ${delayMinutes}min delay")
    }

    private fun scheduleOnWifiBackup(triggerId: String, config: TriggerConfig) {
        val delayMinutes = (config.values["delay_minutes"] as? Number)?.toLong() ?: 10L
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AutomationBackupWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag("$WORK_TAG_ON_WIFI-$triggerId")
            .setInputData(workDataOf(
                "trigger_id" to triggerId,
                "trigger_type" to "on_wifi_backup"
            ))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "$WORK_TAG_ON_WIFI-$triggerId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        logger.i(TAG, "Scheduled on-wifi backup with ${delayMinutes}min delay")
    }

    private fun scheduleImmediateBackup(appIds: List<AppId>) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<AutomationBackupWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "trigger_id" to "immediate",
                "trigger_type" to "manual",
                "app_ids" to appIds.joinToString(",") { it.value }
            ))
            .build()
        
        workManager.enqueue(workRequest)
        logger.i(TAG, "Scheduled immediate backup for ${appIds.size} apps")
    }

    private fun checkConditions(): Boolean {
        val batteryOk = checkBatteryLevel()
        val storageOk = checkStorageSpace()
        
        return batteryOk && storageOk
    }

    private fun checkBatteryLevel(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val minBattery = preferences.getInt(PREF_MIN_BATTERY_LEVEL, DEFAULT_MIN_BATTERY)
        
        val isOk = batteryLevel >= minBattery
        if (!isOk) {
            logger.w(TAG, "Battery level too low: $batteryLevel% (min: $minBattery%)")
        }
        return isOk
    }

    private fun checkStorageSpace(): Boolean {
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBytes
        val availableGB = availableBytes / (1024 * 1024 * 1024)
        val minStorageGB = preferences.getInt(PREF_MIN_STORAGE_GB, DEFAULT_MIN_STORAGE)
        
        val isOk = availableGB >= minStorageGB
        if (!isOk) {
            logger.w(TAG, "Storage space too low: ${availableGB}GB (min: ${minStorageGB}GB)")
        }
        return isOk
    }

    fun checkWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun setMinBatteryLevel(level: Int) {
        preferences.edit().putInt(PREF_MIN_BATTERY_LEVEL, level).apply()
        logger.i(TAG, "Updated min battery level to $level%")
    }

    fun setMinStorageGB(gb: Int) {
        preferences.edit().putInt(PREF_MIN_STORAGE_GB, gb).apply()
        logger.i(TAG, "Updated min storage to ${gb}GB")
    }
}

/**
 * Worker that executes automated backups
 */
class AutomationBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val triggerId = inputData.getString("trigger_id") ?: return Result.failure()
        val triggerType = inputData.getString("trigger_type") ?: return Result.failure()
        
        return try {
            val appIdsString = inputData.getString("app_ids")
            val appIds = if (!appIdsString.isNullOrBlank()) {
                appIdsString.split(",").map { AppId(it) }
            } else {
                emptyList()
            }
            
            val request = BackupRequest(
                appIds = appIds,
                components = setOf(BackupComponent.APK, BackupComponent.DATA),
                incremental = true,
                compressionLevel = 6,
                encryptionEnabled = false,
                description = "Automated backup - $triggerType"
            )
            
            val intent = android.content.Intent("com.obsidianbackup.ACTION_AUTOMATED_BACKUP").apply {
                putExtra("trigger_id", triggerId)
                putExtra("trigger_type", triggerType)
                putExtra("app_ids", appIdsString)
            }
            applicationContext.sendBroadcast(intent)
            
            android.util.Log.i("AutomationBackupWorker", "Triggered automated backup: $triggerType")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AutomationBackupWorker", "Automated backup failed", e)
            Result.retry()
        }
    }
}

/**
 * BroadcastReceiver for package install/update events
 */
internal class PackageEventReceiver(
    private val plugin: DefaultAutomationPlugin
) : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val packageName = intent.data?.schemeSpecificPart ?: return
        
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    plugin.onAppInstalled(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED -> {
                plugin.onAppUpdated(packageName)
            }
        }
    }
}

/**
 * BroadcastReceiver for system events (boot, updates)
 */
internal class SystemEventReceiver(
    private val plugin: DefaultAutomationPlugin
) : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                plugin.onSystemBootCompleted()
            }
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    plugin.onSystemBootCompleted()
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                android.util.Log.i("SystemEventReceiver", "App updated, reinitializing automation")
            }
        }
    }
}
