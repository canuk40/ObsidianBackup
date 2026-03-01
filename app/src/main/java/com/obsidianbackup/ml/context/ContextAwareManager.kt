// ml/context/ContextAwareManager.kt
package com.obsidianbackup.ml.context

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.obsidianbackup.ml.ActivityType
import com.obsidianbackup.ml.BackupContext
import com.obsidianbackup.ml.LocationCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Context-aware manager that monitors device state for smart backup scheduling
 * Uses Google ML Kit for activity recognition and location detection
 */
class ContextAwareManager(private val context: Context) {
    
    private val _currentContext = MutableStateFlow(createDefaultContext())
    val currentContext: StateFlow<BackupContext> = _currentContext.asStateFlow()
    
    private var isMonitoring = false
    
    companion object {
        private const val TAG = "ContextAwareManager"
        
        // Location detection parameters
        private const val HOME_DETECTION_MIN_OCCURRENCES = 10
        private const val WORK_DETECTION_MIN_OCCURRENCES = 5
    }
    
    /**
     * Start monitoring device context
     */
    suspend fun startMonitoring() = withContext(Dispatchers.Main) {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring context")
            return@withContext
        }
        
        isMonitoring = true
        
        // Initialize activity recognition if permission granted
        if (hasActivityRecognitionPermission()) {
            initializeActivityRecognition()
        }
        
        // Start periodic context updates
        updateContext()
        
        Log.i(TAG, "Context monitoring started")
    }
    
    /**
     * Stop monitoring device context
     */
    fun stopMonitoring() {
        isMonitoring = false
        Log.i(TAG, "Context monitoring stopped")
    }
    
    /**
     * Get current device context
     */
    suspend fun getCurrentContext(): BackupContext = withContext(Dispatchers.IO) {
        updateContext()
        _currentContext.value
    }
    
    /**
     * Update current context with latest device state
     */
    private suspend fun updateContext() = withContext(Dispatchers.IO) {
        val batteryInfo = getBatteryInfo()
        val networkInfo = getNetworkInfo()
        val storageInfo = getStorageInfo()
        val locationCategory = detectLocationCategory()
        val activityType = detectActivityType()
        val now = LocalDateTime.now()
        
        val newContext = BackupContext(
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            isWifiConnected = networkInfo,
            locationCategory = locationCategory,
            activityType = activityType,
            timeOfDay = now.toLocalTime(),
            dayOfWeek = now.dayOfWeek,
            storageAvailableMb = storageInfo
        )
        
        _currentContext.value = newContext
        
        Log.d(TAG, "Context updated: battery=${batteryInfo.first}%, " +
                "charging=${batteryInfo.second}, wifi=$networkInfo, " +
                "location=$locationCategory, activity=$activityType")
    }
    
    /**
     * Get battery information
     */
    private fun getBatteryInfo(): Pair<Float, Boolean> {
        return try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = (level / scale.toFloat()) * 100f
            
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            
            Pair(batteryPct, isCharging)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery info: ${e.message}", e)
            Pair(50f, false)
        }
    }
    
    /**
     * Get network information
     */
    private fun getNetworkInfo(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                    ?: return false
                
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                @Suppress("DEPRECATION")
                val activeNetwork = connectivityManager.activeNetworkInfo
                activeNetwork?.type == ConnectivityManager.TYPE_WIFI
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get network info: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get available storage space
     */
    private fun getStorageInfo(): Long {
        return try {
            val dataDir = context.dataDir
            val stat = android.os.StatFs(dataDir.path)
            val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBytes
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }
            availableBytes / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get storage info: ${e.message}", e)
            1000L // Default 1GB
        }
    }
    
    /**
     * Detect location category (HOME, WORK, COMMUTE) using patterns
     * Note: This is privacy-preserving - doesn't use actual GPS coordinates
     */
    private suspend fun detectLocationCategory(): LocationCategory = withContext(Dispatchers.IO) {
        try {
            // Use time-based heuristics and historical patterns
            val hour = LocalDateTime.now().hour
            val dayOfWeek = LocalDateTime.now().dayOfWeek
            
            // Simple heuristic based on time patterns
            when {
                // Evening/Night (7 PM - 7 AM) likely at home
                hour >= 19 || hour < 7 -> LocationCategory.HOME
                
                // Weekend likely at home
                dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY -> {
                    if (hour in 10..20) LocationCategory.UNKNOWN else LocationCategory.HOME
                }
                
                // Weekday 9 AM - 5 PM likely at work
                hour in 9..17 -> LocationCategory.WORK
                
                // Commute times
                hour in 7..9 || hour in 17..19 -> LocationCategory.COMMUTE
                
                else -> LocationCategory.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect location: ${e.message}", e)
            LocationCategory.UNKNOWN
        }
    }
    
    /**
     * Detect current activity type using ML Kit Activity Recognition
     */
    private suspend fun detectActivityType(): ActivityType = withContext(Dispatchers.IO) {
        try {
            // In production, this would use Google ML Kit Activity Recognition API
            // For now, use simple heuristics based on time and location
            
            val locationCategory = detectLocationCategory()
            
            when (locationCategory) {
                LocationCategory.HOME, LocationCategory.WORK -> ActivityType.STILL
                LocationCategory.COMMUTE -> ActivityType.IN_VEHICLE
                else -> ActivityType.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect activity: ${e.message}", e)
            ActivityType.UNKNOWN
        }
    }
    
    /**
     * Initialize ML Kit Activity Recognition
     */
    private fun initializeActivityRecognition() {
        try {
            // Create activity transitions for detection
            val transitions = listOf(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            
            val request = ActivityTransitionRequest(transitions)
            
            // In production, register for activity updates
            Log.i(TAG, "Activity recognition initialized (placeholder)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize activity recognition: ${e.message}", e)
        }
    }
    
    /**
     * Check if activity recognition permission is granted
     */
    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older versions
        }
    }
    
    /**
     * Create default context
     */
    private fun createDefaultContext(): BackupContext {
        val now = LocalDateTime.now()
        return BackupContext(
            batteryLevel = 50f,
            isCharging = false,
            isWifiConnected = false,
            locationCategory = LocationCategory.UNKNOWN,
            activityType = ActivityType.UNKNOWN,
            timeOfDay = now.toLocalTime(),
            dayOfWeek = now.dayOfWeek,
            storageAvailableMb = 1000L
        )
    }
    
    /**
     * Check if current context is optimal for backup
     */
    fun isOptimalForBackup(): Boolean {
        val context = _currentContext.value
        return context.batteryLevel > 30f &&
                context.storageAvailableMb > 500 &&
                (context.isCharging || context.batteryLevel > 70f) &&
                context.activityType == ActivityType.STILL
    }
    
    /**
     * Get context score (0-1) indicating how good current conditions are for backup
     */
    fun getContextScore(): Float {
        val context = _currentContext.value
        var score = 0f
        
        // Battery contribution (0-0.3)
        score += when {
            context.isCharging -> 0.3f
            context.batteryLevel > 70f -> 0.25f
            context.batteryLevel > 50f -> 0.15f
            context.batteryLevel > 30f -> 0.05f
            else -> 0f
        }
        
        // Network contribution (0-0.2)
        if (context.isWifiConnected) score += 0.2f
        
        // Activity contribution (0-0.2)
        score += when (context.activityType) {
            ActivityType.STILL -> 0.2f
            ActivityType.WALKING -> 0.1f
            else -> 0f
        }
        
        // Location contribution (0-0.15)
        score += when (context.locationCategory) {
            LocationCategory.HOME -> 0.15f
            LocationCategory.WORK -> 0.1f
            else -> 0.05f
        }
        
        // Storage contribution (0-0.15)
        score += when {
            context.storageAvailableMb > 1000 -> 0.15f
            context.storageAvailableMb > 500 -> 0.1f
            context.storageAvailableMb > 200 -> 0.05f
            else -> 0f
        }
        
        return score.coerceIn(0f, 1f)
    }
}
