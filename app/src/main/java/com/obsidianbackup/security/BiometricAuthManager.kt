// security/BiometricAuthManager.kt
package com.obsidianbackup.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.crypto.Cipher
import kotlin.coroutines.resume

/**
 * Central biometric authentication handler for ObsidianBackup
 * 
 * Manages biometric authentication with StrongBox KeyMint integration
 * and provides fallback to PIN/Pattern authentication.
 * 
 * Features:
 * - BiometricPrompt integration
 * - StrongBox hardware-backed authentication
 * - Crypto-based authentication for key unlock
 * - Fallback to device credentials
 * - Comprehensive error handling
 */
class BiometricAuthManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    companion object {
        private const val BIOMETRIC_STRONG: Int = BiometricManager.Authenticators.BIOMETRIC_STRONG
        private const val BIOMETRIC_WEAK: Int = BiometricManager.Authenticators.BIOMETRIC_WEAK
        private const val DEVICE_CREDENTIAL: Int = BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        // Default timeout for user authentication (30 seconds)
        const val AUTH_VALIDITY_DURATION_SECONDS = 30
    }

    /**
     * Check biometric availability and enrollment status
     */
    fun getBiometricCapability(): BiometricCapability {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricCapability.Available(isStrongBoxAvailable())
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricCapability.NotAvailable(BiometricError.NO_HARDWARE)
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricCapability.NotAvailable(BiometricError.HW_UNAVAILABLE)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricCapability.NotEnrolled
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricCapability.NotAvailable(BiometricError.SECURITY_UPDATE_REQUIRED)
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                BiometricCapability.NotAvailable(BiometricError.UNSUPPORTED)
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                BiometricCapability.NotAvailable(BiometricError.UNKNOWN)
            }
            else -> BiometricCapability.NotAvailable(BiometricError.UNKNOWN)
        }
    }

    /**
     * Check if device credential (PIN/Pattern/Password) authentication is available
     */
    fun canAuthenticateWithDeviceCredential(): Boolean {
        return biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if StrongBox Keymaster is available on this device
     */
    private fun isStrongBoxAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else {
            false
        }
    }

    /**
     * Authenticate user with biometric prompt
     * 
     * @param activity FragmentActivity required for BiometricPrompt
     * @param title Title shown in biometric prompt
     * @param subtitle Subtitle shown in biometric prompt
     * @param description Description shown in biometric prompt
     * @param allowDeviceCredential Allow fallback to PIN/Pattern/Password
     * @param cipher Optional Cipher for crypto-based authentication (key unlock)
     * @return BiometricResult indicating success or failure
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        allowDeviceCredential: Boolean = true,
        cipher: Cipher? = null
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .apply {
                if (allowDeviceCredential) {
                    setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                } else {
                    setAllowedAuthenticators(BIOMETRIC_STRONG)
                    setNegativeButtonText("Cancel")
                }
            }
            .build()

        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) {
                    continuation.resume(
                        BiometricResult.Success(
                            cipher = result.cryptoObject?.cipher,
                            authenticationType = result.authenticationType
                        )
                    )
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) {
                    continuation.resume(
                        BiometricResult.Error(
                            errorCode = errorCode,
                            message = errString.toString(),
                            isPermanent = isPermanentError(errorCode)
                        )
                    )
                }
            }

            override fun onAuthenticationFailed() {
                // Called when biometric is valid but not recognized
                // Don't complete continuation - allow user to retry
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, authCallback)

        // Register cancellation handler
        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }

        // Show prompt with or without cipher
        try {
            if (cipher != null) {
                biometricPrompt.authenticate(
                    promptInfo,
                    BiometricPrompt.CryptoObject(cipher)
                )
            } else {
                biometricPrompt.authenticate(promptInfo)
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(
                    BiometricResult.Error(
                        errorCode = BiometricPrompt.ERROR_VENDOR,
                        message = e.message ?: "Authentication initialization failed",
                        isPermanent = false
                    )
                )
            }
        }
    }

    /**
     * Authenticate for sensitive operations (backup, restore, settings)
     */
    suspend fun authenticateForOperation(
        activity: FragmentActivity,
        operation: SensitiveOperation,
        cipher: Cipher? = null
    ): BiometricResult {
        val capability = getBiometricCapability()
        
        return when (capability) {
            is BiometricCapability.Available -> {
                authenticate(
                    activity = activity,
                    title = operation.title,
                    subtitle = operation.subtitle,
                    description = operation.description,
                    allowDeviceCredential = true,
                    cipher = cipher
                )
            }
            is BiometricCapability.NotEnrolled -> {
                BiometricResult.Error(
                    errorCode = BiometricPrompt.ERROR_NO_BIOMETRICS,
                    message = "No biometric enrolled. Please set up biometric authentication in device settings.",
                    isPermanent = true
                )
            }
            is BiometricCapability.NotAvailable -> {
                BiometricResult.Error(
                    errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    message = "Biometric authentication not available: ${capability.error.message}",
                    isPermanent = true
                )
            }
        }
    }

    /**
     * Handle authentication errors with appropriate user guidance
     */
    fun getErrorGuidance(result: BiometricResult.Error): String {
        return when (result.errorCode) {
            BiometricPrompt.ERROR_CANCELED -> {
                "Authentication canceled"
            }
            BiometricPrompt.ERROR_USER_CANCELED -> {
                "Authentication canceled by user"
            }
            BiometricPrompt.ERROR_LOCKOUT -> {
                "Too many attempts. Please try again in 30 seconds."
            }
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                "Biometric authentication is locked. Please use your device PIN/Pattern."
            }
            BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                "No biometric enrolled. Go to Settings > Security to set up biometric authentication."
            }
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                "Biometric hardware not available on this device."
            }
            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                "Authentication canceled"
            }
            BiometricPrompt.ERROR_TIMEOUT -> {
                "Authentication timeout. Please try again."
            }
            BiometricPrompt.ERROR_NO_SPACE -> {
                "Not enough storage for biometric authentication. Please free up space."
            }
            BiometricPrompt.ERROR_VENDOR -> {
                "Device-specific error: ${result.message}"
            }
            else -> {
                "Authentication failed: ${result.message}"
            }
        }
    }

    /**
     * Check if error is permanent and requires user action
     */
    private fun isPermanentError(errorCode: Int): Boolean {
        return when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_SPACE -> true
            else -> false
        }
    }

    /**
     * Check if key is invalidated due to biometric changes
     */
    fun isKeyInvalidated(exception: Exception): Boolean {
        return exception is KeyPermanentlyInvalidatedException
    }

    /**
     * Check if operation requires user authentication
     */
    fun requiresAuthentication(exception: Exception): Boolean {
        return exception is UserNotAuthenticatedException
    }
}

/**
 * Biometric capability states
 */
sealed class BiometricCapability {
    data class Available(val hasStrongBox: Boolean) : BiometricCapability()
    data object NotEnrolled : BiometricCapability()
    data class NotAvailable(val error: BiometricError) : BiometricCapability()
}

/**
 * Biometric error types
 */
enum class BiometricError(val message: String) {
    NO_HARDWARE("No biometric hardware available"),
    HW_UNAVAILABLE("Biometric hardware unavailable"),
    SECURITY_UPDATE_REQUIRED("Security update required"),
    UNSUPPORTED("Biometric authentication not supported"),
    UNKNOWN("Unknown biometric status")
}

/**
 * Biometric authentication result
 */
sealed class BiometricResult {
    data class Success(
        val cipher: Cipher?,
        val authenticationType: Int
    ) : BiometricResult()
    
    data class Error(
        val errorCode: Int,
        val message: String,
        val isPermanent: Boolean
    ) : BiometricResult()
}

/**
 * Predefined sensitive operations
 */
enum class SensitiveOperation(
    val title: String,
    val subtitle: String?,
    val description: String?
) {
    BACKUP(
        title = "Authenticate to Backup",
        subtitle = "Biometric required",
        description = "Authenticate to create encrypted backup"
    ),
    RESTORE(
        title = "Authenticate to Restore",
        subtitle = "Biometric required",
        description = "Authenticate to restore from encrypted backup"
    ),
    SETTINGS_CHANGE(
        title = "Authenticate to Change Settings",
        subtitle = "Security settings",
        description = "Authenticate to modify security settings"
    ),
    DELETE_BACKUP(
        title = "Authenticate to Delete",
        subtitle = "Destructive action",
        description = "Authenticate to delete backup"
    ),
    EXPORT_DATA(
        title = "Authenticate to Export",
        subtitle = "Data export",
        description = "Authenticate to export sensitive data"
    )
}
