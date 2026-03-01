// performance/BatteryOptimizationManager.kt
package com.obsidianbackup.performance

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages battery optimization settings and provides utilities for battery-efficient operations
 */
class BatteryOptimizationManager(private val context: Context) {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ?: throw IllegalStateException("PowerManager service not available")
    
    /**
     * Check if app is whitelisted from battery optimizations
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Not applicable for older versions
        }
    }
    
    /**
     * Request battery optimization exemption (will show system dialog)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestBatteryOptimizationExemption() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Check if device is in power save mode
     */
    fun isPowerSaveMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    /**
     * Check if device is in interactive mode (screen on)
     */
    fun isInteractive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }
    
    /**
     * Check if current conditions are optimal for background work
     */
    suspend fun isOptimalForBackgroundWork(): Boolean = withContext(Dispatchers.IO) {
        // Don't run heavy tasks in power save mode
        if (isPowerSaveMode()) return@withContext false
        
        // Check battery level
        val batteryLevel = getBatteryLevel()
        if (batteryLevel < MINIMUM_BATTERY_LEVEL) return@withContext false
        
        // Check if charging
        val isCharging = isCharging()
        
        // Optimal if charging or battery is high
        return@withContext isCharging || batteryLevel > OPTIMAL_BATTERY_LEVEL
    }
    
    /**
     * Get current battery level (0-100)
     */
    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100f).toInt()
        } else {
            100 // Assume full if can't determine
        }
    }
    
    /**
     * Check if device is charging
     */
    private fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        val status = batteryIntent?.getIntExtra(
            android.os.BatteryManager.EXTRA_STATUS,
            -1
        ) ?: -1
        
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
               status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }
    
    /**
     * Get thermal throttling state
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getThermalState(): Int {
        return powerManager.currentThermalStatus
    }
    
    /**
     * Check if device is too hot for intensive operations
     */
    fun isThermalThrottling(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val status = getThermalState()
            status >= PowerManager.THERMAL_STATUS_MODERATE
        } else {
            false
        }
    }
    
    companion object {
        private const val MINIMUM_BATTERY_LEVEL = 20
        private const val OPTIMAL_BATTERY_LEVEL = 50
        
        const val THERMAL_STATUS_NONE = PowerManager.THERMAL_STATUS_NONE
        const val THERMAL_STATUS_LIGHT = PowerManager.THERMAL_STATUS_LIGHT
        const val THERMAL_STATUS_MODERATE = PowerManager.THERMAL_STATUS_MODERATE
    }
}
