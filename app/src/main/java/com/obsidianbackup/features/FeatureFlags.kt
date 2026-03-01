// features/FeatureFlags.kt
package com.obsidianbackup.features

/**
 * Every runtime-toggleable feature in ObsidianBackup.
 *
 * These flags live independently of subscription tier checks (see FeatureId / FeatureGateService
 * for PRO gating). Use these to:
 *  - Disable a broken feature instantly without a code change
 *  - Gate experimental work behind a flag until it's ready
 *  - Run A/B experiments via remote config
 *
 * Toggling a flag here does NOT grant PRO access — that is controlled separately by
 * FeatureGateService and FeatureId subscription tier enforcement.
 */
enum class Feature {
    // ── Core backup engine ──────────────────────────────────────────────────
    PARALLEL_BACKUP,
    INCREMENTAL_BACKUP,
    MERKLE_VERIFICATION,
    SPLIT_APK_HANDLING,

    // ── Cloud & sync ────────────────────────────────────────────────────────
    CLOUD_SYNC,
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    S3_COMPATIBLE,
    SFTP,
    WEBDAV,
    SYNCTHING_SYNC,
    DECENTRALIZED_STORAGE,     // IPFS / Filecoin

    // ── Device migration ────────────────────────────────────────────────────
    WIFI_DIRECT_MIGRATION,

    // ── Advanced backup types ───────────────────────────────────────────────
    GAMING_BACKUP,             // Emulator saves, ROMs, game data
    HEALTH_CONNECT_SYNC,       // Health Connect / fitness data
    PLUGIN_SYSTEM,             // Third-party plugins and extensions

    // ── Scheduling & automation ─────────────────────────────────────────────
    SMART_SCHEDULING,          // ML-powered optimal scheduling
    TASKER_INTEGRATION,        // Trigger backups from Tasker / Automate
    AUTOMATION_RULES,          // Event-based and scheduled backup rules

    // ── Security ────────────────────────────────────────────────────────────
    BIOMETRIC_AUTH,
    STANDARD_ENCRYPTION,       // AES-256 encryption
    POST_QUANTUM_ENCRYPTION,   // Post-quantum / zero-knowledge crypto

    // ── Diagnostics & export ────────────────────────────────────────────────
    EXPORTABLE_LOGS,
    EXPORT_DIAGNOSTICS,
    EXPORT_SHELL_AUDIT_LOGS,

    // ── Storage & retention ─────────────────────────────────────────────────
    RETENTION_POLICIES,
    BUSYBOX_OPTIONS,

    // ── UI modes ────────────────────────────────────────────────────────────
    SIMPLIFIED_MODE,
    SPEEDRUN_MODE,

    // ── System integrations ─────────────────────────────────────────────────
    DEEP_LINKING,
    WIDGET_SUPPORT,            // Home screen quick-backup widget
    QUICK_SETTINGS_TILE,       // Quick Settings tile
}

interface RemoteConfig {
    suspend fun getBoolean(key: String): Boolean
    suspend fun getString(key: String): String
    suspend fun getInt(key: String): Int
}

// Simple implementation backed by SharedPreferences (or swap in Firebase Remote Config)
class SharedPreferencesRemoteConfig(
    private val sharedPreferences: android.content.SharedPreferences
) : RemoteConfig {

    override suspend fun getBoolean(key: String): Boolean =
        sharedPreferences.getBoolean(key, featureDefault(key))

    override suspend fun getString(key: String): String =
        sharedPreferences.getString(key, "") ?: ""

    override suspend fun getInt(key: String): Int =
        sharedPreferences.getInt(key, 0)

    private fun featureDefault(key: String): Boolean = when (key) {
        // Core — enabled by default
        "parallel_backup"           -> true
        "incremental_backup"        -> true
        "merkle_verification"       -> true
        "split_apk_handling"        -> true

        // Cloud — enabled by default; individual providers follow
        "cloud_sync"                -> true
        "google_drive"              -> true
        "dropbox"                   -> true
        "onedrive"                  -> true
        "s3_compatible"             -> true
        "sftp"                      -> true
        "webdav"                    -> true
        "syncthing_sync"            -> true
        "decentralized_storage"     -> false  // Experimental — disabled until stable

        // Migration
        "wifi_direct_migration"     -> false  // Requires hardware / pairing setup

        // Advanced backup types
        "gaming_backup"             -> true
        "health_connect_sync"       -> true
        "plugin_system"             -> true

        // Scheduling & automation
        "smart_scheduling"          -> true
        "tasker_integration"        -> true
        "automation_rules"          -> true

        // Security
        "biometric_auth"            -> true
        "standard_encryption"       -> true
        "post_quantum_encryption"   -> false  // Experimental

        // Diagnostics
        "exportable_logs"           -> true
        "export_diagnostics"        -> true
        "export_shell_audit_logs"   -> true

        // Storage
        "retention_policies"        -> true
        "busybox_options"           -> true

        // UI modes
        "simplified_mode"           -> true
        "speedrun_mode"             -> false  // Off until UX is finalized

        // System integrations
        "deep_linking"              -> true
        "widget_support"            -> true
        "quick_settings_tile"       -> true

        else -> false
    }
}

class FeatureFlagManager(
    private val remoteConfig: RemoteConfig
) {
    // In-memory overrides (survive the process but not reinstall)
    private val localOverrides = mutableMapOf<Feature, Boolean>()

    suspend fun isEnabled(feature: Feature): Boolean {
        localOverrides[feature]?.let { return it }
        return remoteConfig.getBoolean(feature.name.lowercase())
    }

    fun setLocalOverride(feature: Feature, enabled: Boolean) {
        localOverrides[feature] = enabled
    }

    fun clearLocalOverride(feature: Feature) {
        localOverrides.remove(feature)
    }

    fun getAllOverrides(): Map<Feature, Boolean> = localOverrides.toMap()
}

/** Human-readable description shown on the Feature Flags debug screen. */
fun Feature.description(): String = when (this) {
    Feature.PARALLEL_BACKUP         -> "Back up multiple apps simultaneously for faster throughput"
    Feature.INCREMENTAL_BACKUP      -> "Only back up files that changed since the last run"
    Feature.MERKLE_VERIFICATION     -> "Use Merkle trees for cryptographic integrity verification"
    Feature.SPLIT_APK_HANDLING      -> "Support split-APK backup and restore"

    Feature.CLOUD_SYNC              -> "Master switch — sync backups to cloud storage providers"
    Feature.GOOGLE_DRIVE            -> "Google Drive cloud provider"
    Feature.DROPBOX                 -> "Dropbox cloud provider"
    Feature.ONEDRIVE                -> "Microsoft OneDrive cloud provider"
    Feature.S3_COMPATIBLE           -> "S3-compatible storage (Backblaze B2, Wasabi, MinIO…)"
    Feature.SFTP                    -> "SFTP / SSH server storage"
    Feature.WEBDAV                  -> "WebDAV server storage (Nextcloud, ownCloud…)"
    Feature.SYNCTHING_SYNC          -> "Peer-to-peer device sync via Syncthing"
    Feature.DECENTRALIZED_STORAGE   -> "Decentralised storage via IPFS / Filecoin (experimental)"

    Feature.WIFI_DIRECT_MIGRATION   -> "Transfer backups directly between devices over Wi-Fi Direct"

    Feature.GAMING_BACKUP           -> "Back up emulator saves, ROMs, and game data"
    Feature.HEALTH_CONNECT_SYNC     -> "Back up Health Connect fitness and health data"
    Feature.PLUGIN_SYSTEM           -> "Discover and install third-party plugins"

    Feature.SMART_SCHEDULING        -> "ML-powered optimal backup scheduling"
    Feature.TASKER_INTEGRATION      -> "Trigger backups from Tasker, Automate, or Locale"
    Feature.AUTOMATION_RULES        -> "Event-based and scheduled backup rules"

    Feature.BIOMETRIC_AUTH          -> "Lock/unlock app and backups with fingerprint or face"
    Feature.STANDARD_ENCRYPTION     -> "AES-256-GCM encryption for backup archives"
    Feature.POST_QUANTUM_ENCRYPTION -> "Post-quantum / zero-knowledge encryption (experimental)"

    Feature.EXPORTABLE_LOGS         -> "Export detailed backup logs to a file"
    Feature.EXPORT_DIAGNOSTICS      -> "Bundle and export full diagnostics report"
    Feature.EXPORT_SHELL_AUDIT_LOGS -> "Export root shell audit trail"

    Feature.RETENTION_POLICIES      -> "Automatic pruning of old backups by age or count"
    Feature.BUSYBOX_OPTIONS         -> "BusyBox binary selection and management"

    Feature.SIMPLIFIED_MODE         -> "Simplified UI for less technical users"
    Feature.SPEEDRUN_MODE           -> "Ultra-compact UI optimised for speed (experimental)"

    Feature.DEEP_LINKING            -> "Handle obsidianbackup:// deep links"
    Feature.WIDGET_SUPPORT          -> "Home screen quick-backup widget"
    Feature.QUICK_SETTINGS_TILE     -> "Quick Settings tile for one-tap backup"
}
