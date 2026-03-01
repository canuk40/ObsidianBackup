package com.obsidianbackup.community

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-app feedback system for bug reports and feature requests
 */
@Singleton
class FeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger,
    private val analyticsManager: AnalyticsManager
) {
    
    private val _feedbackList = MutableStateFlow<List<FeedbackItem>>(emptyList())
    val feedbackList: StateFlow<List<FeedbackItem>> = _feedbackList.asStateFlow()
    
    private val _submissionStatus = MutableStateFlow<SubmissionStatus>(SubmissionStatus.Idle)
    val submissionStatus: StateFlow<SubmissionStatus> = _submissionStatus.asStateFlow()
    
    suspend fun submitFeedback(
        type: FeedbackType,
        title: String,
        description: String,
        email: String? = null,
        attachLogs: Boolean = false,
        deviceInfo: DeviceInfo? = null
    ): Result<Unit> {
        return try {
            _submissionStatus.value = SubmissionStatus.Submitting
            
            val feedback = FeedbackItem(
                id = generateFeedbackId(),
                type = type,
                title = title,
                description = description,
                email = email,
                timestamp = System.currentTimeMillis(),
                deviceInfo = deviceInfo ?: collectDeviceInfo(),
                logsAttached = attachLogs
            )
            
            // Store locally
            val currentList = _feedbackList.value.toMutableList()
            currentList.add(0, feedback)
            _feedbackList.value = currentList
            
            // Log to analytics (no PII)
            analyticsManager.logEvent("feedback_submitted", mapOf(
                "type" to type.name,
                "has_email" to (email != null),
                "has_logs" to attachLogs
            ))
            
            logger.i("FeedbackManager", "Feedback submitted: ${feedback.id}")
            
            _submissionStatus.value = SubmissionStatus.Success
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e("FeedbackManager", "Failed to submit feedback", e)
            _submissionStatus.value = SubmissionStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    fun resetSubmissionStatus() {
        _submissionStatus.value = SubmissionStatus.Idle
    }
    
    private fun generateFeedbackId(): String {
        return "FB_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun collectDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            sdkInt = android.os.Build.VERSION.SDK_INT,
            appVersion = getAppVersion()
        )
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

data class FeedbackItem(
    val id: String,
    val type: FeedbackType,
    val title: String,
    val description: String,
    val email: String?,
    val timestamp: Long,
    val deviceInfo: DeviceInfo,
    val logsAttached: Boolean,
    val status: FeedbackStatus = FeedbackStatus.Submitted
)

enum class FeedbackType {
    BUG_REPORT,
    FEATURE_REQUEST,
    IMPROVEMENT,
    OTHER
}

enum class FeedbackStatus {
    Submitted,
    UnderReview,
    Resolved,
    Closed
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val appVersion: String
)

sealed class SubmissionStatus {
    object Idle : SubmissionStatus()
    object Submitting : SubmissionStatus()
    object Success : SubmissionStatus()
    data class Error(val message: String) : SubmissionStatus()
}
