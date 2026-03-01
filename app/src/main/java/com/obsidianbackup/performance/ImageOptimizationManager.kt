// performance/ImageOptimizationManager.kt
package com.obsidianbackup.performance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.obsidianbackup.BuildConfig
import okhttp3.OkHttpClient
import java.io.File

/**
 * Manages image optimization including caching, compression, and efficient loading
 */
class ImageOptimizationManager(private val context: Context) {
    
    /**
     * Create an optimized ImageLoader with Coil
     */
    fun createOptimizedImageLoader(
        memoryCacheSize: Int = DEFAULT_MEMORY_CACHE_SIZE,
        diskCacheSize: Long = DEFAULT_DISK_CACHE_SIZE,
        okHttpClient: OkHttpClient? = null
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(memoryCacheSize / 100.0)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(diskCacheSize)
                    .build()
            }
            .apply {
                if (okHttpClient != null) {
                    okHttpClient(okHttpClient)
                }
                
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            // Respect memory constraints
            .respectCacheHeaders(false)
            // Default cache policy
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    
    /**
     * Calculate optimal sample size for bitmap loading
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight &&
                   halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Load bitmap efficiently with size constraints
     */
    fun decodeSampledBitmapFromFile(
        filePath: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, this)
            
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory than ARGB_8888
            inMutable = false
            
            BitmapFactory.decodeFile(filePath, this)
        }
    }
    
    /**
     * Compress bitmap to JPEG with quality optimization
     */
    fun compressBitmap(
        bitmap: Bitmap,
        outputFile: File,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): Boolean {
        return try {
            outputFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Calculate appropriate quality based on file size target
     */
    fun calculateOptimalQuality(
        originalSize: Long,
        targetSize: Long
    ): Int {
        val ratio = targetSize.toDouble() / originalSize.toDouble()
        
        return when {
            ratio >= 1.0 -> 100
            ratio >= 0.8 -> 90
            ratio >= 0.6 -> 80
            ratio >= 0.4 -> 70
            ratio >= 0.2 -> 60
            else -> 50
        }
    }
    
    /**
     * Get bitmap memory size in bytes
     */
    fun getBitmapSize(bitmap: Bitmap): Long {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            bitmap.allocationByteCount.toLong()
        } else {
            bitmap.byteCount.toLong()
        }
    }
    
    /**
     * Check if bitmap fits in memory
     */
    fun canLoadBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Boolean {
        val bytesPerPixel = when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ARGB_4444 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 4
        }
        
        val requiredMemory = (width * height * bytesPerPixel).toLong()
        val memoryManager = MemoryOptimizationManager(context)
        
        return memoryManager.hasEnoughMemoryFor(requiredMemory)
    }
    
    /**
     * Clear all image caches
     */
    fun clearImageCaches(imageLoader: ImageLoader) {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
    
    companion object {
        private const val DEFAULT_MEMORY_CACHE_SIZE = 25 // Percentage
        private const val DEFAULT_DISK_CACHE_SIZE = 100L * 1024 * 1024 // 100MB
        private const val DEFAULT_JPEG_QUALITY = 85
        
        // Standard image sizes for different use cases
        const val THUMBNAIL_SIZE = 256
        const val PREVIEW_SIZE = 512
        const val FULL_SIZE = 2048
    }
}
