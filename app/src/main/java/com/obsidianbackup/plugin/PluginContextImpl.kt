package com.obsidianbackup.plugin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.obsidianbackup.api.plugin.CatalogReader
import com.obsidianbackup.api.plugin.PluginContext
import com.obsidianbackup.api.plugin.PluginLogger
import com.obsidianbackup.api.plugin.PluginPreferences
import com.obsidianbackup.api.plugin.SnapshotRef
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.model.BackupId
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class PluginContextImpl(
    override val applicationContext: Context,
    override val pluginDataDir: File,
    private val prefs: SharedPreferences,
    private val catalog: BackupCatalog? = null
) : PluginContext {

    override val logger: PluginLogger = object : PluginLogger {
        override fun i(tag: String, message: String) {
            Log.i("Plugin:$tag", message)
        }

        override fun w(tag: String, message: String) {
            Log.w("Plugin:$tag", message)
        }

        override fun e(tag: String, message: String, throwable: Throwable?) {
            Log.e("Plugin:$tag", message, throwable)
        }

        override fun d(tag: String, message: String) {
            Log.d("Plugin:$tag", message)
        }
    }

    override val catalogReader: CatalogReader = object : CatalogReader {
        override suspend fun listSnapshots(): List<SnapshotRef> {
            // If catalog is available, fetch snapshots and map to SnapshotRef
            val cat = catalog 
                ?: throw IllegalStateException("BackupCatalog not available - cannot query snapshots")
            
            val snaps = cat.getAllSnapshots() // Flow<List<BackupSnapshot>>
            val list = snaps.first()
            return list.map { s -> 
                SnapshotRef(
                    id = s.id.value, 
                    timestamp = s.timestamp, 
                    description = s.description
                ) 
            }
        }

        override suspend fun getSnapshotMetadata(id: String): String? {
            catalog?.let { cat ->
                val meta = cat.getSnapshot(com.obsidianbackup.model.BackupId(id))
                if (meta != null) {
                    return Json { prettyPrint = true }.encodeToString(meta)
                }
            }
            return null
        }
    }

    override val preferences: PluginPreferences = object : PluginPreferences {
        override fun putString(key: String, value: String) {
            prefs.edit().putString(key, value).apply()
        }

        override fun getString(key: String, default: String?): String? {
            return prefs.getString(key, default)
        }

        override fun putBoolean(key: String, value: Boolean) {
            prefs.edit().putBoolean(key, value).apply()
        }

        override fun getBoolean(key: String, default: Boolean): Boolean {
            return prefs.getBoolean(key, default)
        }

        override fun remove(key: String) {
            prefs.edit().remove(key).apply()
        }
    }

    override val platformVersion: String = "obsidianbackup-1.0"
}
