package com.obsidianbackup.deeplink

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Handles authentication for deep link actions that require security verification
 */
@Singleton
class DeepLinkAuthenticator @Inject constructor(
    private val context: Context
) {
    
    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Determine if the given action requires authentication
     */
    fun requiresAuthentication(action: DeepLinkAction): Boolean {
        return when (action) {
            is DeepLinkAction.StartBackup -> false // Backup is safe to trigger
            is DeepLinkAction.RestoreSnapshot -> true // Restore modifies device state
            is DeepLinkAction.ConnectCloudProvider -> true // Involves credentials
            is DeepLinkAction.OpenAppDetails -> false
            is DeepLinkAction.OpenAutomation -> false
            is DeepLinkAction.OpenBackups -> false
            is DeepLinkAction.OpenCloudSettings -> false
            is DeepLinkAction.OpenDashboard -> false
            is DeepLinkAction.OpenLogs -> false
            is DeepLinkAction.OpenSettings -> false
            is DeepLinkAction.OpenSettingsScreen -> {
                // Only security settings require auth
                action.screen == SettingsScreen.SECURITY
            }
            is DeepLinkAction.Invalid -> false
        }
    }
    
    /**
     * Authenticate the user using biometric or device credential
     * Returns true if authentication succeeds
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        action: DeepLinkAction,
        config: DeepLinkAuthConfig = DeepLinkAuthConfig()
    ): Boolean = suspendCancellableCoroutine { continuation ->
        
        if (!isBiometricAvailable()) {
            // If biometric is not available, allow the action (fallback)
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Don't resume here, allow retry
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle(getAuthenticationMessage(action))
            .setAllowedAuthenticators(getAuthenticators(config))
            .apply {
                if (!config.allowDeviceCredential) {
                    setNegativeButtonText("Cancel")
                }
            }
            .build()
        
        continuation.invokeOnCancellation {
            // Cleanup if coroutine is cancelled
        }
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    private fun getAuthenticationMessage(action: DeepLinkAction): String {
        return when (action) {
            is DeepLinkAction.RestoreSnapshot -> "Confirm restore operation"
            is DeepLinkAction.ConnectCloudProvider -> "Confirm cloud connection"
            is DeepLinkAction.OpenSettingsScreen -> "Access security settings"
            else -> "Authenticate to continue"
        }
    }
    
    private fun getAuthenticators(config: DeepLinkAuthConfig): Int {
        return if (config.allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
    }
    
    /**
     * Get a user-friendly description of authentication status
     */
    fun getAuthenticationStatusMessage(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric authentication available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric credentials enrolled"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometric authentication not supported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Biometric status unknown"
            else -> "Biometric authentication status unknown"
        }
    }
}
