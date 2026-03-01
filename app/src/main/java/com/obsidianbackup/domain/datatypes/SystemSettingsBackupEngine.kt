package com.obsidianbackup.domain.datatypes

import com.obsidianbackup.rootcore.shell.ShellExecutor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System settings backup and restore engine (root-only).
 *
 * Backs up Settings.System, Settings.Secure, and Settings.Global values
 * using the `settings` shell command. This captures device configuration that
 * standard backup agents miss: display settings, input methods, lock screen
 * preferences, developer options, etc.
 *
 * Root is required because `settings put secure/global` requires WRITE_SECURE_SETTINGS.
 */
@Singleton
class SystemSettingsBackupEngine @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val json: Json
) {
    companion object {
        private const val TAG = "[SystemSettings]"

        // Settings namespaces
        const val NAMESPACE_SYSTEM = "system"
        const val NAMESPACE_SECURE = "secure"
        const val NAMESPACE_GLOBAL = "global"

        // Keys to skip on restore (device-specific or dangerous)
        private val SKIP_ON_RESTORE = setOf(
            "android_id",
            "bluetooth_address",
            "device_name",
            "lock_screen_owner_info",
            "lockscreen.password_type",
            "lockscreen.password_salt",
            "install_non_market_apps",
            "adb_enabled",
            "development_settings_enabled",
            "verifier_verify_adb_installs",
            "wifi_on"
        )

        // Common useful settings to always backup
        val RECOMMENDED_SYSTEM_KEYS = listOf(
            "screen_brightness", "screen_brightness_mode",
            "screen_off_timeout", "font_scale",
            "accelerometer_rotation", "user_rotation",
            "haptic_feedback_enabled", "sound_effects_enabled",
            "vibrate_when_ringing", "notification_sound",
            "ringtone", "alarm_alert",
            "dtmf_tone", "lockscreen_sounds_enabled",
            "time_12_24"
        )

        val RECOMMENDED_SECURE_KEYS = listOf(
            "default_input_method", "enabled_input_methods",
            "long_press_timeout", "accessibility_enabled",
            "enabled_accessibility_services",
            "location_mode", "location_providers_allowed",
            "nfc_payment_default_component",
            "spell_checker_enabled", "show_ime_with_hard_keyboard",
            "night_display_activated", "night_display_auto_mode",
            "night_display_color_temperature",
            "assistant", "voice_interaction_service",
            "doze_enabled", "doze_pulse_on_pick_up",
            "camera_double_tap_power_gesture_disabled",
            "system_navigation_keys_enabled"
        )

        val RECOMMENDED_GLOBAL_KEYS = listOf(
            "animator_duration_scale", "transition_animation_scale",
            "window_animation_scale", "always_finish_activities",
            "stay_on_while_plugged_in", "auto_time",
            "auto_time_zone", "data_roaming",
            "mobile_data", "wifi_sleep_policy",
            "power_sounds_enabled", "dock_sounds_enabled",
            "charging_sounds_enabled", "usb_mass_storage_enabled"
        )
    }

    @Serializable
    data class SettingEntry(
        val namespace: String,
        val key: String,
        val value: String
    )

    @Serializable
    data class SystemSettingsBackupData(
        val settings: List<SettingEntry>,
        val androidVersion: Int = 0,
        val deviceModel: String = "",
        val backupTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Backup all settings from all three namespaces.
     * Uses `settings list` to enumerate, then `settings get` for each value.
     */
    suspend fun backup(outputFile: File): Result<SystemSettingsBackupData> = runCatching {
        val allSettings = mutableListOf<SettingEntry>()

        for (namespace in listOf(NAMESPACE_SYSTEM, NAMESPACE_SECURE, NAMESPACE_GLOBAL)) {
            val settings = backupNamespace(namespace)
            allSettings.addAll(settings)
            Timber.d("$TAG $namespace: ${settings.size} settings")
        }

        // Get device info for restore compatibility check
        val versionResult = shellExecutor.executeRoot("getprop ro.build.version.sdk")
        val modelResult = shellExecutor.executeRoot("getprop ro.product.model")

        val data = SystemSettingsBackupData(
            settings = allSettings,
            androidVersion = versionResult.stdout.trim().toIntOrNull() ?: 0,
            deviceModel = modelResult.stdout.trim()
        )

        outputFile.writeText(json.encodeToString(data))
        Timber.d("$TAG Total: ${allSettings.size} settings backed up")
        data
    }

    /**
     * Backup only recommended/safe settings (smaller, more portable).
     */
    suspend fun backupRecommended(outputFile: File): Result<SystemSettingsBackupData> = runCatching {
        val allSettings = mutableListOf<SettingEntry>()

        for (key in RECOMMENDED_SYSTEM_KEYS) {
            getSettingValue(NAMESPACE_SYSTEM, key)?.let {
                allSettings.add(SettingEntry(NAMESPACE_SYSTEM, key, it))
            }
        }
        for (key in RECOMMENDED_SECURE_KEYS) {
            getSettingValue(NAMESPACE_SECURE, key)?.let {
                allSettings.add(SettingEntry(NAMESPACE_SECURE, key, it))
            }
        }
        for (key in RECOMMENDED_GLOBAL_KEYS) {
            getSettingValue(NAMESPACE_GLOBAL, key)?.let {
                allSettings.add(SettingEntry(NAMESPACE_GLOBAL, key, it))
            }
        }

        val versionResult = shellExecutor.executeRoot("getprop ro.build.version.sdk")
        val modelResult = shellExecutor.executeRoot("getprop ro.product.model")

        val data = SystemSettingsBackupData(
            settings = allSettings,
            androidVersion = versionResult.stdout.trim().toIntOrNull() ?: 0,
            deviceModel = modelResult.stdout.trim()
        )

        outputFile.writeText(json.encodeToString(data))
        Timber.d("$TAG Recommended: ${allSettings.size} settings backed up")
        data
    }

    /**
     * Restore settings from backup. Skips device-specific and dangerous keys.
     *
     * @param dryRun If true, returns what would change without applying
     * @param onlyRecommended If true, only restores keys in RECOMMENDED lists
     */
    suspend fun restore(
        inputFile: File,
        dryRun: Boolean = false,
        onlyRecommended: Boolean = false
    ): Result<RestoreReport> = runCatching {
        val data = json.decodeFromString<SystemSettingsBackupData>(inputFile.readText())
        val allRecommended = RECOMMENDED_SYSTEM_KEYS + RECOMMENDED_SECURE_KEYS + RECOMMENDED_GLOBAL_KEYS

        var applied = 0
        var skipped = 0
        val changes = mutableListOf<String>()

        for (entry in data.settings) {
            if (SKIP_ON_RESTORE.contains(entry.key)) {
                skipped++
                continue
            }
            if (onlyRecommended && entry.key !in allRecommended) {
                skipped++
                continue
            }

            // Check current value to avoid unnecessary writes
            val current = getSettingValue(entry.namespace, entry.key)
            if (current == entry.value) {
                skipped++
                continue
            }

            val change = "${entry.namespace}/${entry.key}: $current → ${entry.value}"
            changes.add(change)

            if (!dryRun) {
                val result = shellExecutor.executeRoot("settings put ${entry.namespace} ${entry.key} '${entry.value}'")
                if (result.success) applied++ else skipped++
            } else {
                applied++
            }
        }

        val report = RestoreReport(applied = applied, skipped = skipped, changes = changes, dryRun = dryRun)
        Timber.d("$TAG Restore: applied=$applied, skipped=$skipped, dryRun=$dryRun")
        report
    }

    data class RestoreReport(
        val applied: Int,
        val skipped: Int,
        val changes: List<String>,
        val dryRun: Boolean
    )

    /**
     * Diff a backup file against current device settings.
     */
    suspend fun diff(inputFile: File): Result<List<String>> = runCatching {
        val data = json.decodeFromString<SystemSettingsBackupData>(inputFile.readText())
        val diffs = mutableListOf<String>()

        for (entry in data.settings) {
            val current = getSettingValue(entry.namespace, entry.key)
            if (current != entry.value) {
                diffs.add("${entry.namespace}/${entry.key}: current=$current backup=${entry.value}")
            }
        }

        Timber.d("$TAG Diff found ${diffs.size} differences")
        diffs
    }

    private suspend fun backupNamespace(namespace: String): List<SettingEntry> {
        val result = shellExecutor.executeRoot("settings list $namespace")
        if (!result.success) return emptyList()

        return result.stdout.lines()
            .filter { it.contains("=") }
            .mapNotNull { line ->
                val eqIndex = line.indexOf('=')
                if (eqIndex > 0) {
                    val key = line.substring(0, eqIndex).trim()
                    val value = line.substring(eqIndex + 1).trim()
                    if (key.isNotBlank() && value != "null") {
                        SettingEntry(namespace, key, value)
                    } else null
                } else null
            }
    }

    private suspend fun getSettingValue(namespace: String, key: String): String? {
        val result = shellExecutor.executeRoot("settings get $namespace $key")
        return if (result.success && result.stdout.trim() != "null") {
            result.stdout.trim()
        } else null
    }
}
