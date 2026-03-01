// performance/NetworkOptimizationManager.kt
package com.obsidianbackup.performance

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import com.obsidianbackup.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages network optimization including HTTP/2, connection pooling, and compression
 */
class NetworkOptimizationManager(private val context: Context) {
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: throw IllegalStateException("ConnectivityManager service not available")
    
    /**
     * Create an optimized OkHttpClient with HTTP/2, connection pooling, and caching
     */
    fun createOptimizedHttpClient(
        cacheSize: Long = DEFAULT_CACHE_SIZE,
        enableLogging: Boolean = false
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, cacheSize)
        
        val builder = OkHttpClient.Builder()
            // Enable HTTP/2 and connection pooling
            .connectionPool(ConnectionPool(
                maxIdleConnections = 5,
                keepAliveDuration = 5,
                TimeUnit.MINUTES
            ))
            // Timeouts optimized for mobile networks
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // Response caching
            .cache(cache)
            // Retry on connection failure
            .retryOnConnectionFailure(true)
        
        if (enableLogging && BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }
        
        // Add compression support
        builder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .build()
            chain.proceed(request)
        }
        
        return builder.build()
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        }
    }
    
    /**
     * Check if connected to WiFi
     */
    fun isWifiConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }
    
    /**
     * Check if connected to metered network (cellular data)
     */
    fun isMeteredConnection(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.isActiveNetworkMetered
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }
    
    /**
     * Get network bandwidth in kbps (estimate)
     */
    fun getNetworkBandwidth(): NetworkBandwidth {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkBandwidth.UNKNOWN
            val capabilities = connectivityManager.getNetworkCapabilities(network) 
                ?: return NetworkBandwidth.UNKNOWN
            
            val downlink = capabilities.linkDownstreamBandwidthKbps
            val uplink = capabilities.linkUpstreamBandwidthKbps
            
            return NetworkBandwidth(downlink, uplink)
        }
        
        return NetworkBandwidth.UNKNOWN
    }
    
    /**
     * Check if network conditions are optimal for large transfers
     */
    suspend fun isOptimalForLargeTransfer(): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext false
        
        // Prefer WiFi for large transfers
        if (isWifiConnected()) return@withContext true
        
        // Check if cellular has good bandwidth
        val bandwidth = getNetworkBandwidth()
        return@withContext bandwidth.downlinkKbps > MIN_BANDWIDTH_FOR_LARGE_TRANSFER
    }
    
    /**
     * Calculate optimal chunk size for file uploads based on network conditions
     */
    fun getOptimalChunkSize(): Long {
        val bandwidth = getNetworkBandwidth()
        
        return when {
            bandwidth.downlinkKbps > 10000 -> CHUNK_SIZE_FAST // 4MB
            bandwidth.downlinkKbps > 2000 -> CHUNK_SIZE_MEDIUM // 2MB
            bandwidth.downlinkKbps > 500 -> CHUNK_SIZE_SLOW // 1MB
            else -> CHUNK_SIZE_VERY_SLOW // 512KB
        }
    }
    
    data class NetworkBandwidth(
        val downlinkKbps: Int,
        val uplinkKbps: Int
    ) {
        companion object {
            val UNKNOWN = NetworkBandwidth(0, 0)
        }
    }
    
    companion object {
        private const val DEFAULT_CACHE_SIZE = 50L * 1024 * 1024 // 50MB
        private const val MIN_BANDWIDTH_FOR_LARGE_TRANSFER = 2000 // 2 Mbps
        
        const val CHUNK_SIZE_FAST = 4L * 1024 * 1024 // 4MB
        const val CHUNK_SIZE_MEDIUM = 2L * 1024 * 1024 // 2MB
        const val CHUNK_SIZE_SLOW = 1024L * 1024 // 1MB
        const val CHUNK_SIZE_VERY_SLOW = 512L * 1024 // 512KB
    }
}
