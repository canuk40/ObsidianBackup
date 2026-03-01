// security/WebViewSecurityManager.kt
package com.obsidianbackup.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.*
import android.net.http.SslError
import com.obsidianbackup.logging.ObsidianLogger

/**
 * WebView Security Manager for XSS Prevention
 * Implements OWASP MASVS-CODE requirements
 * 
 * Features:
 * - XSS prevention
 * - Content Security Policy enforcement
 * - JavaScript interface security
 * - Safe WebView configuration
 * - HTTPS-only loading
 */
class WebViewSecurityManager(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "WebViewSecurity"
        
        // Content Security Policy
        // Note: 'unsafe-inline' removed for security. If inline scripts/styles needed,
        // implement nonce-based CSP with unique nonces per page load.
        private const val CSP_HEADER = """
            default-src 'self';
            script-src 'self';
            style-src 'self';
            img-src 'self' data: https:;
            font-src 'self' data:;
            connect-src 'self' https:;
            frame-src 'none';
            object-src 'none';
            base-uri 'self';
            form-action 'self';
        """
        
        // Allowed URL schemes
        private val ALLOWED_SCHEMES = setOf("https", "file", "data")
        
        // JavaScript interface whitelist
        private val ALLOWED_JS_INTERFACES = setOf(
            "ObsidianBackupAPI"
        )
    }
    
    /**
     * Configure WebView with secure settings
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configureSecureWebView(
        webView: WebView,
        enableJavaScript: Boolean = false,
        allowedDomains: Set<String> = emptySet()
    ) {
        webView.settings.apply {
            // JavaScript (disabled by default for security)
            javaScriptEnabled = enableJavaScript
            
            // Disable potentially dangerous features
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            
            // Database and storage (disabled for security)
            databaseEnabled = false
            domStorageEnabled = false
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            
            // Geolocation disabled
            setGeolocationEnabled(false)
            
            // Mixed content (HTTPS only)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            
            // Disable save form data
            saveFormData = false
            savePassword = false
            
            // User agent (don't reveal too much)
            userAgentString = "ObsidianBackup/1.0"
            
            // Caching
            cacheMode = WebSettings.LOAD_NO_CACHE
            
            // Zoom controls
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        
        // Set secure WebViewClient
        webView.webViewClient = SecureWebViewClient(allowedDomains)
        
        // Set secure WebChromeClient
        webView.webChromeClient = SecureWebChromeClient()
        
        // Clear cache and history
        webView.clearCache(true)
        webView.clearHistory()
        
        logger.i(TAG, "WebView configured with secure settings")
    }
    
    /**
     * Sanitize HTML content to prevent XSS
     */
    fun sanitizeHtml(html: String): String {
        var sanitized = html
        
        // Remove script tags
        sanitized = sanitized.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
        
        // Remove inline event handlers
        sanitized = sanitized.replace(Regex("\\bon\\w+\\s*=\\s*['\"][^'\"]*['\"]", RegexOption.IGNORE_CASE), "")
        sanitized = sanitized.replace(Regex("\\bon\\w+\\s*=\\s*[^\\s>]*", RegexOption.IGNORE_CASE), "")
        
        // Remove javascript: protocol
        sanitized = sanitized.replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
        
        // Remove data: protocol for scripts
        sanitized = sanitized.replace(Regex("data:text/html", RegexOption.IGNORE_CASE), "")
        
        // Remove potentially dangerous tags
        val dangerousTags = listOf("iframe", "frame", "object", "embed", "applet", "meta", "link")
        for (tag in dangerousTags) {
            sanitized = sanitized.replace(
                Regex("<$tag[^>]*>.*?</$tag>", RegexOption.IGNORE_CASE),
                ""
            )
            sanitized = sanitized.replace(
                Regex("<$tag[^>]*/>", RegexOption.IGNORE_CASE),
                ""
            )
        }
        
        return sanitized
    }
    
    /**
     * Validate and sanitize URL
     */
    fun validateUrl(url: String): String? {
        try {
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme?.lowercase()
            
            // Check allowed schemes
            if (scheme !in ALLOWED_SCHEMES) {
                logger.w(TAG, "Blocked disallowed URL scheme: $scheme")
                return null
            }
            
            // For HTTPS URLs, additional validation
            if (scheme == "https") {
                val host = uri.host
                if (host.isNullOrEmpty()) {
                    logger.w(TAG, "Blocked URL with empty host: $url")
                    return null
                }
                
                // Check for suspicious patterns
                if (host.contains("..") || host.startsWith(".") || host.endsWith(".")) {
                    logger.w(TAG, "Blocked suspicious URL: $url")
                    return null
                }
            }
            
            return url
        } catch (e: Exception) {
            logger.e(TAG, "Failed to validate URL: $url", e)
            return null
        }
    }
    
    /**
     * Load HTML content safely
     */
    fun loadHtmlSafely(
        webView: WebView,
        html: String,
        baseUrl: String = "https://obsidianbackup.local"
    ) {
        val sanitizedHtml = sanitizeHtml(html)
        val htmlWithCSP = injectCSP(sanitizedHtml)
        
        webView.loadDataWithBaseURL(
            baseUrl,
            htmlWithCSP,
            "text/html",
            "UTF-8",
            null
        )
        
        logger.d(TAG, "Loaded sanitized HTML content")
    }
    
    /**
     * Inject Content Security Policy into HTML
     */
    private fun injectCSP(html: String): String {
        val cspMeta = """
            <meta http-equiv="Content-Security-Policy" content="$CSP_HEADER">
        """.trimIndent()
        
        // Try to inject in <head>
        val headRegex = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)
        if (headRegex.containsMatchIn(html)) {
            return headRegex.replace(html) { "${it.value}$cspMeta" }
        }
        
        // If no <head>, inject at the beginning
        return "$cspMeta$html"
    }
    
    /**
     * Add JavaScript interface securely
     */
    @SuppressLint("JavascriptInterface")
    fun addSecureJavaScriptInterface(
        webView: WebView,
        obj: Any,
        name: String
    ) {
        if (name !in ALLOWED_JS_INTERFACES) {
            throw SecurityException("JavaScript interface not whitelisted: $name")
        }
        
        // Validate that the object has @JavascriptInterface annotations
        val hasValidMethods = obj.javaClass.methods.any { method ->
            method.isAnnotationPresent(android.webkit.JavascriptInterface::class.java)
        }
        
        if (!hasValidMethods) {
            throw SecurityException("JavaScript interface must have @JavascriptInterface annotated methods")
        }
        
        webView.addJavascriptInterface(obj, name)
        logger.i(TAG, "Added JavaScript interface: $name")
    }
    
    /**
     * Execute JavaScript safely
     */
    fun evaluateJavaScriptSafely(
        webView: WebView,
        script: String,
        callback: ((String) -> Unit)? = null
    ) {
        // Validate script for dangerous patterns
        if (containsDangerousJavaScript(script)) {
            logger.w(TAG, "Blocked dangerous JavaScript execution")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script) { result ->
                callback?.invoke(result)
            }
        } else {
            @Suppress("DEPRECATION")
            webView.loadUrl("javascript:$script")
        }
    }
    
    /**
     * Check for dangerous JavaScript patterns
     */
    private fun containsDangerousJavaScript(script: String): Boolean {
        val dangerousPatterns = listOf(
            Regex("eval\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("Function\\s*\\(", RegexOption.IGNORE_CASE),
            Regex("setTimeout\\s*\\(\\s*['\"]", RegexOption.IGNORE_CASE),
            Regex("setInterval\\s*\\(\\s*['\"]", RegexOption.IGNORE_CASE),
            Regex("<script", RegexOption.IGNORE_CASE),
            Regex("document\\.write", RegexOption.IGNORE_CASE),
            Regex("innerHTML\\s*=", RegexOption.IGNORE_CASE)
        )
        
        return dangerousPatterns.any { it.containsMatchIn(script) }
    }
    
    /**
     * Secure WebViewClient
     */
    private inner class SecureWebViewClient(
        private val allowedDomains: Set<String>
    ) : WebViewClient() {
        
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString() ?: return true
            
            // Validate URL
            val validatedUrl = validateUrl(url)
            if (validatedUrl == null) {
                logger.w(TAG, "Blocked URL loading: $url")
                return true
            }
            
            // Check domain whitelist
            if (allowedDomains.isNotEmpty()) {
                val host = request.url.host
                if (host !in allowedDomains) {
                    logger.w(TAG, "Blocked non-whitelisted domain: $host")
                    return true
                }
            }
            
            return false
        }
        
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            // Never ignore SSL errors
            handler?.cancel()
            logger.e(TAG, "SSL error: ${error?.primaryError}")
        }
        
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                logger.e(TAG, "WebView error: ${error?.description}")
            }
        }
    }
    
    /**
     * Secure WebChromeClient
     */
    private inner class SecureWebChromeClient : WebChromeClient() {
        
        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            logger.d(TAG, "JavaScript alert blocked: $message")
            result?.cancel()
            return true
        }
        
        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            logger.d(TAG, "JavaScript confirm blocked: $message")
            result?.cancel()
            return true
        }
        
        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            logger.d(TAG, "JavaScript prompt blocked: $message")
            result?.cancel()
            return true
        }
        
        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            // Deny geolocation requests
            callback?.invoke(origin, false, false)
            logger.d(TAG, "Geolocation request denied for: $origin")
        }
    }
    
    /**
     * Clear all WebView data
     */
    fun clearWebViewData(webView: WebView) {
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        webView.clearSslPreferences()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        
        WebStorage.getInstance().deleteAllData()
        
        logger.i(TAG, "Cleared all WebView data")
    }
}
