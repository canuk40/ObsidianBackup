package com.obsidianbackup.domain.datatypes

import com.obsidianbackup.rootcore.shell.ShellExecutor
import com.obsidianbackup.rootcore.shell.ShellResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wi-Fi network configuration backup and restore via root.
 *
 * Android 8+: configs stored in /data/misc/apexdata/com.android.wifi/WifiConfigStore.xml
 *   (or /data/misc/wifi/WifiConfigStore.xml on some devices)
 * Android 7-: configs in /data/misc/wifi/wpa_supplicant.conf
 *
 * Also supports `cmd wifi` approach on Android 10+ to list/add saved networks.
 */
@Singleton
class WifiBackupEngine @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val json: Json
) {
    companion object {
        private const val TAG = "[WifiBackup]"

        // Modern Android paths (Android 8+)
        private val WIFI_CONFIG_PATHS = listOf(
            "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml",
            "/data/misc/wifi/WifiConfigStore.xml"
        )
        // Legacy path (Android 7 and below)
        private const val WPA_SUPPLICANT_PATH = "/data/misc/wifi/wpa_supplicant.conf"
    }

    @Serializable
    data class WifiNetwork(
        val ssid: String,
        val securityType: String = "UNKNOWN",
        val psk: String? = null,
        val hidden: Boolean = false,
        val priority: Int = 0
    )

    @Serializable
    data class WifiBackupData(
        val configXml: String? = null,
        val wpaSupplicant: String? = null,
        val parsedNetworks: List<WifiNetwork> = emptyList(),
        val configSourcePath: String? = null,
        val backupTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Backup Wi-Fi configuration files directly via root.
     * Copies the raw config file + parses network names for display.
     */
    suspend fun backup(outputFile: File): Result<WifiBackupData> = runCatching {
        // Try modern WifiConfigStore.xml first
        for (path in WIFI_CONFIG_PATHS) {
            val check = shellExecutor.executeRoot("test -f $path && echo EXISTS")
            if (check.success && check.stdout.contains("EXISTS")) {
                val catResult = shellExecutor.executeRoot("cat $path")
                if (catResult.success) {
                    val networks = parseWifiConfigStoreXml(catResult.stdout)
                    val data = WifiBackupData(
                        configXml = catResult.stdout,
                        parsedNetworks = networks,
                        configSourcePath = path
                    )
                    outputFile.writeText(json.encodeToString(data))
                    Timber.d("$TAG Backed up ${networks.size} Wi-Fi networks from $path")
                    return@runCatching data
                }
            }
        }

        // Fallback to legacy wpa_supplicant.conf
        val wpaResult = shellExecutor.executeRoot("cat $WPA_SUPPLICANT_PATH")
        if (wpaResult.success) {
            val networks = parseWpaSupplicant(wpaResult.stdout)
            val data = WifiBackupData(
                wpaSupplicant = wpaResult.stdout,
                parsedNetworks = networks,
                configSourcePath = WPA_SUPPLICANT_PATH
            )
            outputFile.writeText(json.encodeToString(data))
            Timber.d("$TAG Backed up ${networks.size} Wi-Fi networks from wpa_supplicant.conf")
            return@runCatching data
        }

        // Last resort: try `cmd wifi list-networks` (Android 11+)
        val cmdResult = shellExecutor.executeRoot("cmd wifi list-networks")
        val networks = if (cmdResult.success) parseCmdWifiOutput(cmdResult.stdout) else emptyList()
        val data = WifiBackupData(parsedNetworks = networks)
        outputFile.writeText(json.encodeToString(data))
        Timber.d("$TAG Backed up ${networks.size} Wi-Fi networks via cmd wifi")
        data
    }

    /**
     * Restore Wi-Fi configuration by writing back the config file via root.
     * Requires Wi-Fi service restart after restore.
     */
    suspend fun restore(inputFile: File): Result<Int> = runCatching {
        val data = json.decodeFromString<WifiBackupData>(inputFile.readText())
        val targetPath = data.configSourcePath

        if (data.configXml != null && targetPath != null) {
            val tmpFile = "/data/local/tmp/wifi_restore_${System.currentTimeMillis()}.xml"
            shellExecutor.executeRoot("cat > $tmpFile << 'OBSIDIAN_EOF'\n${data.configXml}\nOBSIDIAN_EOF")
            shellExecutor.executeRoot("cp $tmpFile $targetPath")
            shellExecutor.executeRoot("chmod 660 $targetPath")
            shellExecutor.executeRoot("chown system:wifi $targetPath")
            shellExecutor.executeRoot("rm -f $tmpFile")

            // Restart Wi-Fi to apply
            restartWifi()
            Timber.d("$TAG Restored Wi-Fi config to $targetPath")
            return@runCatching data.parsedNetworks.size
        }

        if (data.wpaSupplicant != null) {
            val tmpFile = "/data/local/tmp/wpa_restore_${System.currentTimeMillis()}.conf"
            shellExecutor.executeRoot("cat > $tmpFile << 'OBSIDIAN_EOF'\n${data.wpaSupplicant}\nOBSIDIAN_EOF")
            shellExecutor.executeRoot("cp $tmpFile $WPA_SUPPLICANT_PATH")
            shellExecutor.executeRoot("chmod 660 $WPA_SUPPLICANT_PATH")
            shellExecutor.executeRoot("chown system:wifi $WPA_SUPPLICANT_PATH")
            shellExecutor.executeRoot("rm -f $tmpFile")

            restartWifi()
            Timber.d("$TAG Restored wpa_supplicant.conf")
            return@runCatching data.parsedNetworks.size
        }

        Timber.w("$TAG No Wi-Fi config data to restore")
        0
    }

    private suspend fun restartWifi() {
        shellExecutor.executeRoot("svc wifi disable")
        shellExecutor.executeRoot("sleep 1")
        shellExecutor.executeRoot("svc wifi enable")
    }

    /**
     * Parse WifiConfigStore.xml for SSID names. Extracts <string name="SSID">"NetworkName"</string>.
     */
    private fun parseWifiConfigStoreXml(xml: String): List<WifiNetwork> {
        val networks = mutableListOf<WifiNetwork>()
        val ssidRegex = """<string name="SSID">&quot;(.+?)&quot;</string>""".toRegex()
        val configTypeRegex = """<byte-array name="AllowedKeyManagement"[^>]*>(\w+)</byte-array>""".toRegex()

        // Simple XML parsing — each <WifiConfiguration> block
        val blocks = xml.split("<WifiConfiguration>").drop(1)
        for (block in blocks) {
            val ssidMatch = ssidRegex.find(block)
            if (ssidMatch != null) {
                val ssid = ssidMatch.groupValues[1]
                val hidden = block.contains("""<boolean name="HiddenSSID" value="true"""")
                networks.add(WifiNetwork(ssid = ssid, hidden = hidden))
            }
        }
        return networks
    }

    /**
     * Parse legacy wpa_supplicant.conf for network blocks.
     */
    private fun parseWpaSupplicant(conf: String): List<WifiNetwork> {
        val networks = mutableListOf<WifiNetwork>()
        val ssidRegex = """ssid="(.+?)"""".toRegex()
        val pskRegex = """psk="(.+?)"""".toRegex()
        val keyMgmtRegex = """key_mgmt=(\S+)""".toRegex()

        val blocks = conf.split("network={").drop(1)
        for (block in blocks) {
            val ssid = ssidRegex.find(block)?.groupValues?.get(1) ?: continue
            val psk = pskRegex.find(block)?.groupValues?.get(1)
            val keyMgmt = keyMgmtRegex.find(block)?.groupValues?.get(1) ?: "NONE"
            val hidden = block.contains("scan_ssid=1")

            networks.add(
                WifiNetwork(
                    ssid = ssid,
                    securityType = keyMgmt,
                    psk = psk,
                    hidden = hidden
                )
            )
        }
        return networks
    }

    /**
     * Parse `cmd wifi list-networks` output (Android 11+).
     */
    private fun parseCmdWifiOutput(output: String): List<WifiNetwork> {
        return output.lines()
            .drop(1) // Skip header
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2) {
                    WifiNetwork(ssid = parts.drop(1).joinToString(" "))
                } else null
            }
    }
}
