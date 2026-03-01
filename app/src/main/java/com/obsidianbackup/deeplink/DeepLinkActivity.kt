package com.obsidianbackup.deeplink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeepLinkActivity : FragmentActivity() {
    
    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler
    
    @Inject
    lateinit var securityValidator: DeepLinkSecurityValidator
    
    @Inject
    lateinit var logger: ObsidianLogger
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.i(TAG, "DeepLinkActivity started")
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        val uri = intent.data
        
        if (uri == null) {
            logger.w(TAG, "DeepLinkActivity called with no URI")
            showError("Invalid deep link")
            finish()
            return
        }
        
        logger.i(TAG, "Processing deep link: $uri")
        
        if (!verifyDeepLinkOrigin(intent)) {
            logger.w(TAG, "Deep link security validation failed")
            showSecurityError("This deep link was rejected for security reasons.")
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val result = deepLinkHandler.handleIntent(intent, this@DeepLinkActivity)
                
                when (result) {
                    is DeepLinkResult.Success -> {
                        logger.i(TAG, "Deep link handled successfully")
                        finish()
                    }
                    is DeepLinkResult.AuthenticationRequired -> {
                        logger.w(TAG, "Authentication failed: ${result.reason}")
                        showError("Authentication required: ${result.reason}")
                        finish()
                    }
                    is DeepLinkResult.Error -> {
                        logger.e(TAG, "Deep link error: ${result.reason}")
                        showError("Failed to process deep link: ${result.reason}")
                        finish()
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Exception handling deep link", e)
                showError("Error: ${e.message}")
                finish()
            }
        }
    }
    
    private fun verifyDeepLinkOrigin(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        val scheme = uri.scheme?.lowercase()
        val callingPackage = getCallingPackage(intent)
        val validationResult = securityValidator.validateDeepLinkOrigin(uri, callingPackage)
        
        if (!validationResult.allowed) {
            logger.w(TAG, "SECURITY: Deep link rejected - ${validationResult.reason}")
            logger.w(TAG, "SECURITY: URI=$uri, Caller=$callingPackage, Scheme=$scheme")
        }
        
        return validationResult.allowed
    }
    
    private fun getCallingPackage(intent: Intent): String? {
        callingActivity?.packageName?.let { return it }
        intent.getStringExtra("source_package")?.let { return it }
        intent.getStringExtra("calling_package")?.let { return it }
        intent.getStringExtra("referrer")?.let { return it }
        referrer?.host?.let { return it }
        
        if (intent.hasExtra("self_invoked") && intent.getBooleanExtra("self_invoked", false)) {
            return packageName
        }
        
        return null
    }
    
    private fun isHttpsScheme(uri: android.net.Uri?): Boolean {
        return uri?.scheme?.lowercase() == "https"
    }
    
    private fun showSecurityError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        logger.w(TAG, "SECURITY: User notified of rejected deep link")
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    companion object {
        private const val TAG = "DeepLinkActivity"
    }
}
