// sync/SyncthingApiClient.kt
package com.obsidianbackup.sync

import android.util.Log
import com.obsidianbackup.sync.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REST API client for Syncthing
 * 
 * Communicates with Syncthing's REST API for all operations.
 * Supports both embedded Syncthing and external Syncthing instances.
 */
@Singleton
class SyncthingApiClient @Inject constructor() {
    companion object {
        private const val TAG = "SyncthingApiClient"
        private const val DEFAULT_TIMEOUT_MS = 30000
    }

    private var apiUrl: String = "http://127.0.0.1:8384"
    private var apiKey: String = ""
    private var syncthingProcess: Process? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Start Syncthing native process
     */
    suspend fun start(homeDir: String, apiKey: String, guiAddress: String) = 
        withContext(Dispatchers.IO) {
            this@SyncthingApiClient.apiKey = apiKey
            this@SyncthingApiClient.apiUrl = "http://$guiAddress"
            
            // Start Syncthing process
            val command = arrayOf(
                "sh", "-c",
                "export STHOMEDIR='$homeDir' && " +
                "export STGUIADDRESS='$guiAddress' && " +
                "export STGUIAPIKEY='$apiKey' && " +
                "syncthing -no-browser -no-restart -logflags=0"
            )
            
            try {
                syncthingProcess = Runtime.getRuntime().exec(command)
                Log.i(TAG, "Syncthing process started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Syncthing process", e)
                throw e
            }
        }

    /**
     * Shutdown Syncthing
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        try {
            post("/rest/system/shutdown", "")
            syncthingProcess?.destroy()
            syncthingProcess = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }

    /**
     * Ping Syncthing API to check if it's available
     */
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = get("/rest/system/ping")
            response.contains("pong")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get device ID
     */
    suspend fun getDeviceId(): String = withContext(Dispatchers.IO) {
        val response = get("/rest/system/status")
        val status = json.decodeFromString<SystemStatus>(response)
        status.myID
    }

    /**
     * Get all configured devices
     */
    suspend fun getDevices(): List<SyncthingDevice> = withContext(Dispatchers.IO) {
        val response = get("/rest/config/devices")
        json.decodeFromString(response)
    }

    /**
     * Add a device
     */
    suspend fun addDevice(device: SyncthingDevice) = withContext(Dispatchers.IO) {
        val deviceJson = json.encodeToString(device)
        post("/rest/config/devices", deviceJson)
    }

    /**
     * Remove a device
     */
    suspend fun removeDevice(deviceId: String) = withContext(Dispatchers.IO) {
        delete("/rest/config/devices/$deviceId")
    }

    /**
     * Get all configured folders
     */
    suspend fun getFolders(): List<SyncthingFolder> = withContext(Dispatchers.IO) {
        val response = get("/rest/config/folders")
        json.decodeFromString(response)
    }

    /**
     * Add a folder
     */
    suspend fun addFolder(folder: SyncthingFolder) = withContext(Dispatchers.IO) {
        val folderJson = json.encodeToString(folder)
        post("/rest/config/folders", folderJson)
    }

    /**
     * Remove a folder
     */
    suspend fun removeFolder(folderId: String) = withContext(Dispatchers.IO) {
        delete("/rest/config/folders/$folderId")
    }

    /**
     * Pause a folder
     */
    suspend fun pauseFolder(folderId: String) = withContext(Dispatchers.IO) {
        post("/rest/db/pause?folder=$folderId", "")
    }

    /**
     * Resume a folder
     */
    suspend fun resumeFolder(folderId: String) = withContext(Dispatchers.IO) {
        post("/rest/db/resume?folder=$folderId", "")
    }

    /**
     * Get system status
     */
    suspend fun getSystemStatus(): SystemStatus = withContext(Dispatchers.IO) {
        val response = get("/rest/system/status")
        json.decodeFromString(response)
    }

    /**
     * Get folder completion
     */
    suspend fun getFolderCompletion(): FolderCompletion = withContext(Dispatchers.IO) {
        try {
            val response = get("/rest/db/completion")
            json.decodeFromString(response)
        } catch (e: Exception) {
            // Return empty if not available
            FolderCompletion(emptyMap(), 0, 0)
        }
    }

    /**
     * Get connections
     */
    suspend fun getConnections(): ConnectionInfo = withContext(Dispatchers.IO) {
        try {
            val response = get("/rest/system/connections")
            json.decodeFromString(response)
        } catch (e: Exception) {
            ConnectionInfo(emptyMap(), 0, 0)
        }
    }

    /**
     * Get conflicts
     */
    suspend fun getConflicts(): List<ConflictFile> = withContext(Dispatchers.IO) {
        try {
            val response = get("/rest/db/conflicts")
            json.decodeFromString(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get discovered devices
     */
    suspend fun getDiscoveredDevices(): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        try {
            val response = get("/rest/system/discovery")
            json.decodeFromString(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Enable/disable global discovery
     */
    suspend fun setGlobalDiscovery(enabled: Boolean) = withContext(Dispatchers.IO) {
        post("/rest/config/options/globalAnnounceEnabled", enabled.toString())
    }

    /**
     * Enable/disable local discovery
     */
    suspend fun setLocalDiscovery(enabled: Boolean) = withContext(Dispatchers.IO) {
        post("/rest/config/options/localAnnounceEnabled", enabled.toString())
    }

    // HTTP helper methods

    private fun get(endpoint: String): String {
        val url = URL("$apiUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-API-Key", apiKey)
            connection.connectTimeout = DEFAULT_TIMEOUT_MS
            connection.readTimeout = DEFAULT_TIMEOUT_MS
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                return response
            } else {
                throw Exception("HTTP error: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun post(endpoint: String, body: String): String {
        val url = URL("$apiUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("X-API-Key", apiKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = DEFAULT_TIMEOUT_MS
            connection.readTimeout = DEFAULT_TIMEOUT_MS
            
            if (body.isNotEmpty()) {
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(body)
                writer.flush()
                writer.close()
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || 
                responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                return reader.use { it.readText() }
            } else {
                throw Exception("HTTP error: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun delete(endpoint: String) {
        val url = URL("$apiUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("X-API-Key", apiKey)
            connection.connectTimeout = DEFAULT_TIMEOUT_MS
            connection.readTimeout = DEFAULT_TIMEOUT_MS
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && 
                responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                throw Exception("HTTP error: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
}
