package com.obsidianbackup.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the execution of deep link actions
 * Coordinates between parsing, authentication, security validation, and navigation
 * 
 * SECURITY: All deep links undergo origin validation before processing.
 * Custom scheme (obsidianbackup://) links are verified against trusted app signatures.
 */
@Singleton
class DeepLinkHandler @Inject constructor(
    private val context: Context,
    private val parser: DeepLinkParser,
    private val authenticator: DeepLinkAuthenticator,
    private val router: DeepLinkRouter,
    private val analytics: DeepLinkAnalytics,
    private val securityValidator: DeepLinkSecurityValidator,
    private val logger: ObsidianLogger
) {
    
    /**
     * Handle a deep link URI
     * Returns true if the deep link was handled successfully
     * 
     * SECURITY: Validates origin before processing to prevent URI interception
     */
    suspend fun handleDeepLink(
        uri: Uri,
        activity: FragmentActivity,
        authConfig: DeepLinkAuthConfig = DeepLinkAuthConfig(),
        callingPackage: String? = null
    ): DeepLinkResult = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        
        try {
            logger.i(TAG, "Processing deep link: $uri (caller: $callingPackage)")
            
            // Step 0: Validate origin (security check)
            val validationResult = securityValidator.validateDeepLinkOrigin(uri, callingPackage)
            if (!validationResult.allowed) {
                logger.w(TAG, "SECURITY: Deep link rejected - ${validationResult.reason}")
                val result = DeepLinkResult.Error(
                    reason = "Security validation failed: ${validationResult.reason}",
                    originalUri = uri
                )
                analytics.trackFailure(uri, DeepLinkAction.Invalid("Security check failed"), validationResult.reason)
                return@withContext result
            }
            
            logger.d(TAG, "Deep link origin validated: ${validationResult.reason}")
            
            // Step 1: Parse the URI
            val action = parser.parse(uri)
            
            if (action is DeepLinkAction.Invalid) {
                val result = DeepLinkResult.Error(action.reason, uri)
                analytics.trackFailure(uri, action, action.reason)
                return@withContext result
            }
            
            logger.d(TAG, "Parsed action: ${action.javaClass.simpleName}")
            
            // Step 2: Check if authentication is required
            val requiresAuth = authenticator.requiresAuthentication(action)
            var authenticated = false
            
            if (requiresAuth) {
                logger.d(TAG, "Authentication required for action")
                authenticated = authenticator.authenticate(activity, action, authConfig)
                
                if (!authenticated) {
                    val result = DeepLinkResult.AuthenticationRequired(
                        action = action,
                        reason = "User authentication failed or cancelled"
                    )
                    analytics.trackFailure(uri, action, "Authentication failed")
                    return@withContext result
                }
                
                logger.d(TAG, "Authentication successful")
            }
            
            // Step 3: Execute the action via router
            val routeResult = router.route(action, activity)
            
            if (!routeResult.success) {
                val result = DeepLinkResult.Error(
                    reason = routeResult.errorMessage ?: "Failed to execute action",
                    originalUri = uri
                )
                analytics.trackFailure(uri, action, result.reason)
                return@withContext result
            }
            
            // Step 4: Track success
            val duration = System.currentTimeMillis() - startTime
            analytics.trackSuccess(uri, action, authenticated, duration)
            
            logger.i(TAG, "Deep link handled successfully: ${action.javaClass.simpleName}")
            
            DeepLinkResult.Success(
                action = action,
                metadata = mapOf(
                    "durationMs" to duration.toString(),
                    "authenticated" to authenticated.toString(),
                    "callingPackage" to (callingPackage ?: "unknown"),
                    "signatureVerified" to validationResult.signatureVerified.toString()
                )
            )
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to handle deep link", e)
            
            val result = DeepLinkResult.Error(
                reason = "Unexpected error: ${e.message}",
                originalUri = uri
            )
            
            analytics.trackDeepLinkEvent(
                DeepLinkEvent(
                    action = "error",
                    success = false,
                    errorReason = e.message,
                    metadata = mapOf(
                        "uri" to uri.toString(),
                        "callingPackage" to (callingPackage ?: "unknown")
                    )
                )
            )
            
            result
        }
    }
    
    /**
     * Handle a deep link from an Intent
     * Extracts calling package information for security validation
     */
    suspend fun handleIntent(intent: Intent, activity: FragmentActivity): DeepLinkResult {
        val uri = intent.data
        
        if (uri == null) {
            logger.w(TAG, "Intent has no data URI")
            return DeepLinkResult.Error("No URI in intent")
        }
        
        // Extract calling package for security validation
        val callingPackage = extractCallingPackage(intent, activity)
        
        // Log caller info for audit
        logger.i(TAG, "Deep link from intent: caller=$callingPackage")
        
        return handleDeepLink(uri, activity, callingPackage = callingPackage)
    }
    
    /**
     * Extract calling package from Intent using multiple detection methods
     */
    private fun extractCallingPackage(intent: Intent, activity: FragmentActivity): String? {
        // Method 1: Activity.getCallingActivity()
        activity.callingActivity?.packageName?.let { return it }
        
        // Method 2: Intent extras
        intent.getStringExtra("source_package")?.let { return it }
        intent.getStringExtra("calling_package")?.let { return it }
        
        // Method 3: Referrer
        activity.referrer?.host?.let { return it }
        
        return null
    }
    
    /**
     * Validate a deep link without executing it
     * Useful for testing or previewing
     */
    fun validateDeepLink(uri: Uri): DeepLinkValidationResult {
        val action = parser.parse(uri)
        
        return when (action) {
            is DeepLinkAction.Invalid -> {
                DeepLinkValidationResult(
                    valid = false,
                    action = null,
                    errorMessage = action.reason
                )
            }
            else -> {
                DeepLinkValidationResult(
                    valid = true,
                    action = action,
                    requiresAuth = authenticator.requiresAuthentication(action)
                )
            }
        }
    }
    
    /**
     * Generate a deep link URI for an action
     */
    fun generateDeepLink(action: DeepLinkAction): Uri? {
        return router.generateUri(action)
    }
    
    companion object {
        private const val TAG = "DeepLinkHandler"
    }
}

/**
 * Result of deep link validation
 */
data class DeepLinkValidationResult(
    val valid: Boolean,
    val action: DeepLinkAction?,
    val requiresAuth: Boolean = false,
    val errorMessage: String? = null
)
