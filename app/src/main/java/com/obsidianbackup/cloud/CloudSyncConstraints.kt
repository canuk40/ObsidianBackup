package com.obsidianbackup.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud sync constraints — per-schedule network and power requirements.
 *
 * Validates whether current device conditions satisfy the sync constraints
 * before initiating cloud upload/download operations.
 */
@Singleton
class CloudSyncConstraints @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "[CloudConstraints]"
    }

    enum class NetworkType {
        ANY,        // Wi-Fi, cellular, or any network
        WIFI_ONLY,  // Only on Wi-Fi (no metered)
        UNMETERED   // Any unmetered network
    }

    data class SyncConstraint(
        val networkType: NetworkType = NetworkType.WIFI_ONLY,
        val requireCharging: Boolean = false,
        val minBatteryPercent: Int = 15,
        val requireIdle: Boolean = false,
        val maxRetries: Int = 3,
        val retryDelayMs: Long = 60_000
    )

    data class ConstraintCheckResult(
        val satisfied: Boolean,
        val unsatisfiedReasons: List<String> = emptyList()
    )

    /**
     * Check if current conditions satisfy the given sync constraints.
     */
    fun checkConstraints(constraint: SyncConstraint): ConstraintCheckResult {
        val reasons = mutableListOf<String>()

        // Network check
        if (!isNetworkSatisfied(constraint.networkType)) {
            reasons.add(when (constraint.networkType) {
                NetworkType.WIFI_ONLY -> "Wi-Fi required but not connected"
                NetworkType.UNMETERED -> "Unmetered network required but on metered"
                NetworkType.ANY -> "No network available"
            })
        }

        // Charging check
        if (constraint.requireCharging && !isCharging()) {
            reasons.add("Device must be charging")
        }

        // Battery check
        val batteryLevel = getBatteryLevel()
        if (batteryLevel in 0 until constraint.minBatteryPercent) {
            reasons.add("Battery too low: $batteryLevel% (min: ${constraint.minBatteryPercent}%)")
        }

        val result = ConstraintCheckResult(reasons.isEmpty(), reasons)
        if (!result.satisfied) {
            Timber.d("$TAG Constraints not met: ${reasons.joinToString(", ")}")
        }
        return result
    }

    /**
     * Check if Wi-Fi is connected.
     */
    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Check if any network is available.
     */
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Check if current network is metered (e.g., cellular data).
     */
    fun isMetered(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        return cm.isActiveNetworkMetered
    }

    fun isCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isNetworkSatisfied(required: NetworkType): Boolean {
        return when (required) {
            NetworkType.ANY -> isNetworkAvailable()
            NetworkType.WIFI_ONLY -> isWifiConnected()
            NetworkType.UNMETERED -> isNetworkAvailable() && !isMetered()
        }
    }
}
