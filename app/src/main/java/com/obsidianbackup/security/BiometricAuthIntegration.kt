// security/BiometricAuthIntegration.kt
package com.obsidianbackup.security

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import com.obsidianbackup.crypto.BiometricRequiredException
import com.obsidianbackup.crypto.EncryptionEngine
import kotlinx.coroutines.flow.first
import java.io.File
import javax.crypto.Cipher

/**
 * BiometricAuthIntegration - Helper class for integrating biometric authentication
 * with encryption operations in ObsidianBackup
 * 
 * This class provides high-level methods that combine BiometricAuthManager,
 * EncryptionEngine, and BiometricSettings to simplify biometric authentication
 * in your activities and fragments.
 */
class BiometricAuthIntegration(
    private val context: Context,
    private val encryptionEngine: EncryptionEngine = EncryptionEngine(),
    private val biometricAuthManager: BiometricAuthManager = BiometricAuthManager(context),
    private val biometricSettings: BiometricSettings = BiometricSettings(context)
) {

    /**
     * Check if biometric authentication should be used for a specific operation
     */
    suspend fun shouldUseBiometric(operation: SensitiveOperation): Boolean {
        val globalEnabled = biometricSettings.biometricEnabled.first()
        if (!globalEnabled) return false
        
        return when (operation) {
            SensitiveOperation.BACKUP -> biometricSettings.biometricForBackup.first()
            SensitiveOperation.RESTORE -> biometricSettings.biometricForRestore.first()
            SensitiveOperation.SETTINGS_CHANGE -> biometricSettings.biometricForSettings.first()
            SensitiveOperation.DELETE_BACKUP -> biometricSettings.biometricForDelete.first()
            SensitiveOperation.EXPORT_DATA -> biometricSettings.biometricForExport.first()
        }
    }

    /**
     * Perform backup with biometric authentication if required
     * 
     * @param activity Activity for showing biometric prompt
     * @param inputFile File to encrypt
     * @param outputFile Encrypted output file
     * @param keyId Unique key identifier
     * @return Result of encryption operation
     */
    suspend fun performBackupWithAuth(
        activity: FragmentActivity,
        inputFile: File,
        outputFile: File,
        keyId: String
    ): AuthResult {
        val requireBiometric = shouldUseBiometric(SensitiveOperation.BACKUP)
        
        // Generate key
        val keyAlias = encryptionEngine.generateKey(
            keyId = keyId,
            requireBiometric = requireBiometric,
            authValidityDuration = 30
        )
        
        // If biometric not required, encrypt directly
        if (!requireBiometric) {
            return try {
                val metadata = encryptionEngine.encryptFile(inputFile, outputFile, keyAlias)
                AuthResult.Success(data = metadata)
            } catch (e: Exception) {
                AuthResult.Failure(message = e.message ?: "Encryption failed")
            }
        }
        
        // Initialize cipher for biometric authentication
        val cipher = try {
            encryptionEngine.initCipherForEncryption(keyAlias)
        } catch (e: Exception) {
            return AuthResult.Failure(message = "Failed to initialize encryption: ${e.message}")
        }
        
        // Authenticate user
        when (val authResult = biometricAuthManager.authenticateForOperation(
            activity = activity,
            operation = SensitiveOperation.BACKUP,
            cipher = cipher
        )) {
            is BiometricResult.Success -> {
                return try {
                    val metadata = encryptionEngine.encryptFile(
                        inputFile = inputFile,
                        outputFile = outputFile,
                        keyAlias = keyAlias,
                        cipher = authResult.cipher
                    )
                    AuthResult.Success(data = metadata)
                } catch (e: Exception) {
                    AuthResult.Failure(message = e.message ?: "Encryption failed")
                }
            }
            is BiometricResult.Error -> {
                val guidance = biometricAuthManager.getErrorGuidance(authResult)
                return if (authResult.errorCode == androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS) {
                    AuthResult.NotEnrolled(message = guidance)
                } else {
                    AuthResult.Failure(message = guidance)
                }
            }
        }
    }

    /**
     * Perform restore with biometric authentication if required
     * 
     * @param activity Activity for showing biometric prompt
     * @param inputFile Encrypted file to decrypt
     * @param outputFile Decrypted output file
     * @param keyAlias Key alias used for encryption
     * @return Result of decryption operation
     */
    suspend fun performRestoreWithAuth(
        activity: FragmentActivity,
        inputFile: File,
        outputFile: File,
        keyAlias: String
    ): AuthResult {
        val requireBiometric = shouldUseBiometric(SensitiveOperation.RESTORE)
        
        // Check if key requires authentication
        val keyRequiresAuth = encryptionEngine.keyRequiresAuthentication(keyAlias)
        
        // If no authentication required, decrypt directly
        if (!keyRequiresAuth && !requireBiometric) {
            val result = encryptionEngine.decryptFile(inputFile, outputFile, keyAlias)
            return result.fold(
                onSuccess = { AuthResult.Success(data = true) },
                onFailure = { AuthResult.Failure(message = it.message ?: "Decryption failed") }
            )
        }
        
        // Read IV from encrypted file
        val iv = try {
            inputFile.inputStream().use { input ->
                ByteArray(12).apply { input.read(this) }
            }
        } catch (e: Exception) {
            return AuthResult.Failure(message = "Failed to read encrypted file: ${e.message}")
        }
        
        // Initialize cipher for biometric authentication
        val cipher = try {
            encryptionEngine.initCipherForDecryption(keyAlias, iv)
        } catch (e: BiometricRequiredException) {
            // Continue to show biometric prompt
            null
        } catch (e: Exception) {
            return AuthResult.Failure(message = "Failed to initialize decryption: ${e.message}")
        }
        
        // Authenticate user
        when (val authResult = biometricAuthManager.authenticateForOperation(
            activity = activity,
            operation = SensitiveOperation.RESTORE,
            cipher = cipher
        )) {
            is BiometricResult.Success -> {
                val result = encryptionEngine.decryptFile(
                    inputFile = inputFile,
                    outputFile = outputFile,
                    keyAlias = keyAlias,
                    cipher = authResult.cipher
                )
                return result.fold(
                    onSuccess = { AuthResult.Success(data = true) },
                    onFailure = { AuthResult.Failure(message = it.message ?: "Decryption failed") }
                )
            }
            is BiometricResult.Error -> {
                val guidance = biometricAuthManager.getErrorGuidance(authResult)
                return if (authResult.errorCode == androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS) {
                    AuthResult.NotEnrolled(message = guidance)
                } else {
                    AuthResult.Failure(message = guidance)
                }
            }
        }
    }

    /**
     * Perform settings change with biometric authentication if required
     * 
     * @param activity Activity for showing biometric prompt
     * @param action Action to perform after successful authentication
     * @return Result of operation
     */
    suspend fun performSettingsChangeWithAuth(
        activity: FragmentActivity,
        action: suspend () -> Unit
    ): AuthResult {
        val requireBiometric = shouldUseBiometric(SensitiveOperation.SETTINGS_CHANGE)
        
        if (!requireBiometric) {
            return try {
                action()
                AuthResult.Success(data = true)
            } catch (e: Exception) {
                AuthResult.Failure(message = e.message ?: "Operation failed")
            }
        }
        
        // Authenticate user
        when (val authResult = biometricAuthManager.authenticateForOperation(
            activity = activity,
            operation = SensitiveOperation.SETTINGS_CHANGE,
            cipher = null
        )) {
            is BiometricResult.Success -> {
                return try {
                    action()
                    AuthResult.Success(data = true)
                } catch (e: Exception) {
                    AuthResult.Failure(message = e.message ?: "Operation failed")
                }
            }
            is BiometricResult.Error -> {
                val guidance = biometricAuthManager.getErrorGuidance(authResult)
                return if (authResult.errorCode == androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS) {
                    AuthResult.NotEnrolled(message = guidance)
                } else {
                    AuthResult.Failure(message = guidance)
                }
            }
        }
    }

    /**
     * Check biometric enrollment and prompt user to enroll if needed
     * 
     * @param activity Activity for navigation
     * @return true if enrolled, false if not enrolled
     */
    fun checkEnrollmentAndPrompt(activity: FragmentActivity): EnrollmentStatus {
        return when (val capability = biometricAuthManager.getBiometricCapability()) {
            is BiometricCapability.Available -> {
                EnrollmentStatus.Enrolled(hasStrongBox = capability.hasStrongBox)
            }
            is BiometricCapability.NotEnrolled -> {
                EnrollmentStatus.NotEnrolled
            }
            is BiometricCapability.NotAvailable -> {
                EnrollmentStatus.NotAvailable(error = capability.error.message)
            }
        }
    }

    /**
     * Open system biometric enrollment settings
     */
    fun openEnrollmentSettings(activity: FragmentActivity) {
        try {
            val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to security settings if biometric enroll not available
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            activity.startActivity(intent)
        }
    }

    /**
     * Handle key invalidation (biometric changed)
     * 
     * @param keyAlias Key that was invalidated
     */
    fun handleKeyInvalidation(keyAlias: String) {
        // Delete invalidated key
        encryptionEngine.deleteKey(keyAlias)
        
        // User needs to re-setup encryption
        // This should be handled by the UI layer
    }
}

/**
 * Result of authentication operation
 */
sealed class AuthResult {
    data class Success<T>(val data: T) : AuthResult()
    data class Failure(val message: String) : AuthResult()
    data class NotEnrolled(val message: String) : AuthResult()
}

/**
 * Biometric enrollment status
 */
sealed class EnrollmentStatus {
    data class Enrolled(val hasStrongBox: Boolean) : EnrollmentStatus()
    data object NotEnrolled : EnrollmentStatus()
    data class NotAvailable(val error: String) : EnrollmentStatus()
}
