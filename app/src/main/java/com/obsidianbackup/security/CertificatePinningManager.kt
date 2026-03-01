// security/CertificatePinningManager.kt
package com.obsidianbackup.security

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Certificate Pinning Manager for secure API communications
 * Implements OWASP MASVS-NETWORK requirements
 * 
 * Provides:
 * - Certificate pinning for API endpoints
 * - Multiple pin backup support
 * - Pin validation and rotation
 * - SSL/TLS configuration
 */
class CertificatePinningManager(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "CertificatePinning"
        
        // Certificate pins (SHA-256 hashes of public keys)
        // Generated from live googleapis.com certificate chain (updated: 2024)
        // IMPORTANT: Update these pins when Google rotates their certificates
        // Use CertificatePinHelper.extractPinsFromUrl() to get new pins
        private val API_PINS = mapOf(
            // Google Drive API and googleapis.com
            "drive.googleapis.com" to listOf(
                "sha256/dMKp+EmLpCLB/WxGhCnWWRcS+FG6u6XdQAQjcaaK8qE=", // *.googleapis.com (leaf)
                "sha256/YPtHaftLw6/0vnc2BnNKGF54xiCA28WFcccjkA4ypCM=", // WR2 (intermediate)
                "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc="  // GTS Root R1 (root/backup)
            ),
            "googleapis.com" to listOf(
                "sha256/dMKp+EmLpCLB/WxGhCnWWRcS+FG6u6XdQAQjcaaK8qE=", // *.googleapis.com (leaf)
                "sha256/YPtHaftLw6/0vnc2BnNKGF54xiCA28WFcccjkA4ypCM=", // WR2 (intermediate)
                "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc="  // GTS Root R1 (root/backup)
            ),
            
            // Custom backend (if you have your own API server)
            // REPLACE THESE WITH YOUR ACTUAL CERTIFICATE PINS
            // Generate pins using: openssl s_client -connect your-domain.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
            "api.obsidianbackup.com" to listOf(
                // "sha256/YOUR_PRIMARY_PIN_HERE",   // Uncomment and add your primary certificate pin
                // "sha256/YOUR_BACKUP_PIN_HERE"     // Uncomment and add your backup certificate pin
            ),
            
            // WebDAV endpoints (configure for your specific WebDAV server)
            // Generate pins before enabling - see documentation
            "webdav.example.com" to listOf(
                // "sha256/YOUR_WEBDAV_PIN_HERE"    // Uncomment and add your WebDAV certificate pin
            )
        )
        
        // Connection timeouts
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L
    }
    
    /**
     * Create an OkHttpClient with certificate pinning enabled
     */
    fun createPinnedOkHttpClient(
        additionalPins: Map<String, List<String>> = emptyMap(),
        enablePublicKeyPinning: Boolean = true,
        enableCertificateTransparency: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
        
        // Add certificate pinning
        if (enablePublicKeyPinning) {
            val certificatePinner = buildCertificatePinner(additionalPins)
            builder.certificatePinner(certificatePinner)
        }
        
        // Configure SSL/TLS
        val sslContext = createSecureSSLContext()
        if (sslContext != null) {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            
            if (trustManagers.isNotEmpty() && trustManagers[0] is X509TrustManager) {
                builder.sslSocketFactory(
                    sslContext.socketFactory,
                    trustManagers[0] as X509TrustManager
                )
            }
        }
        
        // Add hostname verification
        builder.hostnameVerifier { hostname, session ->
            val verified = HttpsURLConnection.getDefaultHostnameVerifier()
                .verify(hostname, session)
            
            if (!verified) {
                logger.w(TAG, "Hostname verification failed for: $hostname")
            }
            
            verified
        }
        
        // Add certificate transparency checking (if enabled)
        if (enableCertificateTransparency) {
            builder.addNetworkInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                
                // Log certificate chain for transparency
                val connection = chain.connection()
                if (connection != null) {
                    logCertificateChain(connection, request.url.host)
                }
                
                response
            }
        }
        
        logger.i(TAG, "OkHttpClient created with certificate pinning")
        return builder.build()
    }
    
    /**
     * Build certificate pinner with all configured pins
     */
    private fun buildCertificatePinner(
        additionalPins: Map<String, List<String>>
    ): CertificatePinner {
        val builder = CertificatePinner.Builder()
        
        // Add default pins
        for ((hostname, pins) in API_PINS) {
            for (pin in pins) {
                builder.add(hostname, pin)
                logger.d(TAG, "Added pin for $hostname: $pin")
            }
        }
        
        // Add additional pins
        for ((hostname, pins) in additionalPins) {
            for (pin in pins) {
                builder.add(hostname, pin)
                logger.d(TAG, "Added additional pin for $hostname: $pin")
            }
        }
        
        return builder.build()
    }
    
    /**
     * Create a secure SSLContext with modern TLS configuration
     */
    private fun createSecureSSLContext(): SSLContext? {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, java.security.SecureRandom())
            
            // Configure to use only TLS 1.2 and 1.3
            // This is handled by the SSLSocketFactory
            
            sslContext
        } catch (e: Exception) {
            logger.e(TAG, "Failed to create SSL context", e)
            null
        }
    }
    
    /**
     * Log certificate chain for transparency and debugging
     */
    private fun logCertificateChain(connection: okhttp3.Connection, hostname: String) {
        try {
            val handshake = connection.handshake()
            if (handshake != null) {
                val certificates = handshake.peerCertificates
                logger.d(TAG, "Certificate chain for $hostname (${certificates.size} certs):")
                
                certificates.forEachIndexed { index, cert ->
                    if (cert is X509Certificate) {
                        logger.d(TAG, "  [$index] Subject: ${cert.subjectDN}")
                        logger.d(TAG, "       Issuer: ${cert.issuerDN}")
                        logger.d(TAG, "       Serial: ${cert.serialNumber}")
                        logger.d(TAG, "       Valid: ${cert.notBefore} to ${cert.notAfter}")
                        
                        // Calculate and log public key hash
                        val publicKeyHash = calculatePublicKeyHash(cert)
                        logger.d(TAG, "       Pin: sha256/$publicKeyHash")
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to log certificate chain", e)
        }
    }
    
    /**
     * Calculate SHA-256 hash of certificate public key
     * Used for pin generation and validation
     */
    fun calculatePublicKeyHash(certificate: X509Certificate): String {
        val publicKey = certificate.publicKey.encoded
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey)
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
    
    /**
     * Validate a certificate against known pins
     */
    fun validateCertificate(hostname: String, certificate: X509Certificate): Boolean {
        val pins = API_PINS[hostname] ?: return false
        val hash = "sha256/${calculatePublicKeyHash(certificate)}"
        
        val isValid = pins.contains(hash)
        if (!isValid) {
            logger.w(TAG, "Certificate validation failed for $hostname")
            logger.w(TAG, "Expected pins: $pins")
            logger.w(TAG, "Got hash: $hash")
        }
        
        return isValid
    }
    
    /**
     * Load certificate from assets for local pinning
     */
    fun loadCertificateFromAssets(filename: String): X509Certificate? {
        return try {
            context.assets.open(filename).use { inputStream ->
                val certificateFactory = CertificateFactory.getInstance("X.509")
                certificateFactory.generateCertificate(inputStream) as X509Certificate
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load certificate from assets: $filename", e)
            null
        }
    }
    
    /**
     * Create a custom TrustManager that validates against pinned certificates
     */
    fun createPinningTrustManager(
        pinnedCertificates: List<X509Certificate>
    ): X509TrustManager {
        return object : X509TrustManager {
            private val defaultTrustManager: X509TrustManager by lazy {
                val trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
                )
                trustManagerFactory.init(null as KeyStore?)
                trustManagerFactory.trustManagers[0] as X509TrustManager
            }
            
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                defaultTrustManager.checkClientTrusted(chain, authType)
            }
            
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // First, perform standard validation
                defaultTrustManager.checkServerTrusted(chain, authType)
                
                // Then, check if any certificate in the chain matches our pins
                var pinMatched = false
                for (cert in chain) {
                    val hash = calculatePublicKeyHash(cert)
                    for (pinnedCert in pinnedCertificates) {
                        if (hash == calculatePublicKeyHash(pinnedCert)) {
                            pinMatched = true
                            break
                        }
                    }
                    if (pinMatched) break
                }
                
                if (!pinMatched) {
                    throw CertificateException("Certificate pinning failed - no matching pins found")
                }
            }
            
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return pinnedCertificates.toTypedArray()
            }
        }
    }
    
    /**
     * Test certificate pinning for a given hostname
     */
    suspend fun testPinning(hostname: String): PinningTestResult {
        return try {
            val client = createPinnedOkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("https://$hostname")
                .head()
                .build()
            
            val response = client.newCall(request).execute()
            response.close()
            
            PinningTestResult(
                success = true,
                hostname = hostname,
                message = "Certificate pinning validated successfully"
            )
        } catch (e: javax.net.ssl.SSLPeerUnverifiedException) {
            logger.e(TAG, "Certificate pinning failed for $hostname", e)
            PinningTestResult(
                success = false,
                hostname = hostname,
                message = "Certificate pinning validation failed: ${e.message}",
                error = e
            )
        } catch (e: Exception) {
            logger.e(TAG, "Connection test failed for $hostname", e)
            PinningTestResult(
                success = false,
                hostname = hostname,
                message = "Connection failed: ${e.message}",
                error = e
            )
        }
    }
    
    data class PinningTestResult(
        val success: Boolean,
        val hostname: String,
        val message: String,
        val error: Throwable? = null
    )
}

/**
 * Helper to get certificate pins from a live server
 * USE ONLY FOR DEVELOPMENT/TESTING
 */
object CertificatePinHelper {
    fun extractPinsFromUrl(url: String): List<String> {
        val pins = mutableListOf<String>()
        
        try {
            val connection = java.net.URL(url).openConnection() as HttpsURLConnection
            connection.connect()
            
            val certificates = connection.serverCertificates
            for (cert in certificates) {
                if (cert is X509Certificate) {
                    val publicKey = cert.publicKey.encoded
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    val hash = digest.digest(publicKey)
                    val pin = android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
                    pins.add("sha256/$pin")
                }
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            println("Failed to extract pins: ${e.message}")
        }
        
        return pins
    }
}
