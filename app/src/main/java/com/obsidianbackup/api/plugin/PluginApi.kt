package com.obsidianbackup.api.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import java.io.File
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.BackupRequest
import com.obsidianbackup.model.BackupResult
import com.obsidianbackup.model.RestoreRequest
import com.obsidianbackup.model.RestoreResult

/**
 * Plugin API version follows semantic versioning
 * Major: Breaking changes
 * Minor: Backward-compatible additions
 * Patch: Bug fixes
 */
data class PluginApiVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) : Comparable<PluginApiVersion> {
    override fun compareTo(other: PluginApiVersion): Int {
        return when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            else -> patch.compareTo(other.patch)
        }
    }

    fun isCompatibleWith(required: PluginApiVersion): Boolean {
        // Same major version, plugin minor >= required minor
        return major == required.major && minor >= required.minor
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        val CURRENT = PluginApiVersion(1, 0, 0)
        val MIN_SUPPORTED = PluginApiVersion(1, 0, 0)

        fun parse(s: String): PluginApiVersion {
            val parts = s.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return PluginApiVersion(major, minor, patch)
        }
    }
}

/**
 * Base interface for all ObsidianBackup plugins
 */
interface ObsidianBackupPlugin {
    /** Unique plugin identifier (reverse domain notation) */
    val id: String

    /** Human-readable plugin name */
    val name: String

    /** Plugin version */
    val version: String

    /** Plugin author/organization */
    val author: String

    /** Plugin API version this plugin was built against */
    val apiVersion: PluginApiVersion

    /** Plugin description */
    val description: String

    /** Plugin capabilities */
    val capabilities: Set<PluginCapability>

    /** Initialization lifecycle - platform provides PluginContext */
    suspend fun initialize(context: PluginContext): PluginResult<Unit>

    /** Cleanup resources when plugin is unloaded */
    suspend fun cleanup()

    /** Optional: called when a backup starts */
    fun onBackupStart(context: BackupContext) {
        // Default implementation - plugins can override to receive event notifications
        // Called before backup operation begins
    }

    /** Optional: called when a backup completes */
    fun onBackupComplete(result: BackupResult) {
        // Default implementation - plugins can override to receive event notifications
        // Called after backup operation finishes (success or failure)
    }

    /** Optional: called when restore starts */
    fun onRestoreStart(context: RestoreContext) {
        // Default implementation - plugins can override to receive event notifications
        // Called before restore operation begins
    }

    /** Plugin configuration UI (optional) */
    fun provideConfigurationScreen(): (@Composable () -> Unit)? = null

    /** Optional: provide custom backup processor (to override or extend platform behavior) */
    fun provideBackupProcessor(): BackupProcessor? = null

    /** Optional: provide custom restore processor */
    fun provideRestoreProcessor(): RestoreProcessor? = null
}

/**
 * Context provided to plugins by ObsidianBackup platform
 */
interface PluginContext {
    /** Android application context */
    val applicationContext: Context

    /** Plugin's private storage directory */
    val pluginDataDir: File

    /** Logger scoped to this plugin */
    val logger: PluginLogger

    /** Access to catalog (read-only for most plugins) */
    val catalogReader: CatalogReader

    /** Secure preferences for plugin configuration */
    val preferences: PluginPreferences

    /** Platform version information */
    val platformVersion: String
}

/**
 * Simple structs for backup/restore lifecycle
 */
data class BackupContext(val snapshotId: BackupId, val apps: List<AppId>)

data class RestoreContext(val snapshotId: BackupId, val apps: List<AppId>)

/**
 * Plugin operation result
 */
sealed class PluginResult<out T> {
    data class Success<T>(val data: T) : PluginResult<T>()
    data class Error(val error: PluginError) : PluginResult<Nothing>()
}

data class PluginError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null
) {
    enum class ErrorCode {
        INITIALIZATION_FAILED,
        CAPABILITY_NOT_SUPPORTED,
        INVALID_CONFIGURATION,
        PERMISSION_DENIED,
        NETWORK_ERROR,
        STORAGE_ERROR,
        UNKNOWN
    }
}

// --- Processor interfaces ---
interface BackupProcessor {
    suspend fun process(backup: BackupRequest): BackupResult
}

interface RestoreProcessor {
    suspend fun process(restore: RestoreRequest): RestoreResult
}

// --- Minimal helper interfaces used by the Plugin API ---

/**
 * Logger scoped to a plugin. Platform will provide an implementation wired to the app logging system.
 */
interface PluginLogger {
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String)
}

/**
 * Read-only catalog reader used by plugins to inspect snapshots and metadata safely.
 */
interface CatalogReader {
    suspend fun listSnapshots(): List<SnapshotRef>
    suspend fun getSnapshotMetadata(id: String): String? // JSON representation
}

/**
 * Simple preferences store for plugin configuration.
 */
interface PluginPreferences {
    fun putString(key: String, value: String)
    fun getString(key: String, default: String? = null): String?
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun remove(key: String)
}

/**
 * A small reference model exposed by CatalogReader
 */
data class SnapshotRef(
    val id: String,
    val timestamp: Long,
    val description: String?
)
