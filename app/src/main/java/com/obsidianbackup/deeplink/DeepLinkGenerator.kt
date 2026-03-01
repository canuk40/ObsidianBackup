package com.obsidianbackup.deeplink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast

/**
 * Utility class for generating and sharing deep links
 */
object DeepLinkGenerator {
    
    /**
     * Generate a backup deep link
     */
    fun generateBackupLink(
        packages: List<String>? = null,
        includeData: Boolean = true,
        includeApk: Boolean = true
    ): String {
        return buildString {
            append("obsidianbackup://backup")
            
            val params = mutableListOf<String>()
            packages?.let { 
                params.add("packages=${it.joinToString(",")}")
            }
            if (!includeData) params.add("includeData=false")
            if (!includeApk) params.add("includeApk=false")
            
            if (params.isNotEmpty()) {
                append("?${params.joinToString("&")}")
            }
        }
    }
    
    /**
     * Generate a restore deep link
     */
    fun generateRestoreLink(
        snapshotId: String,
        packages: List<String>? = null
    ): String {
        return buildString {
            append("obsidianbackup://restore?snapshot=$snapshotId")
            
            packages?.let {
                append("&packages=${it.joinToString(",")}")
            }
        }
    }
    
    /**
     * Generate a settings deep link
     */
    fun generateSettingsLink(screen: SettingsScreen? = null): String {
        return if (screen == null || screen == SettingsScreen.MAIN) {
            "obsidianbackup://settings"
        } else {
            "obsidianbackup://settings/${screen.name.lowercase()}"
        }
    }
    
    /**
     * Generate a cloud connection deep link
     */
    fun generateCloudConnectLink(
        provider: CloudProvider,
        autoConnect: Boolean = false
    ): String {
        return buildString {
            append("obsidianbackup://cloud/connect?provider=${provider.name.lowercase()}")
            if (autoConnect) {
                append("&autoConnect=true")
            }
        }
    }
    
    /**
     * Generate an app details deep link
     */
    fun generateAppDetailsLink(packageName: String): String {
        return "obsidianbackup://app?package=$packageName"
    }
    
    /**
     * Generate a navigation deep link
     */
    fun generateNavigationLink(screen: String): String {
        return "obsidianbackup://$screen"
    }
    
    /**
     * Copy deep link to clipboard
     */
    fun copyToClipboard(context: Context, deepLink: String, label: String = "Deep Link") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, deepLink)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(context, "Deep link copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Share deep link via Android share sheet
     */
    fun shareDeepLink(context: Context, deepLink: String, title: String = "Share Deep Link") {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, deepLink)
            putExtra(android.content.Intent.EXTRA_TITLE, title)
        }
        
        context.startActivity(android.content.Intent.createChooser(intent, title))
    }
    
    /**
     * Generate QR code data for deep link
     * Returns the URI string that can be encoded into a QR code
     */
    fun generateQRCodeData(action: DeepLinkAction): String {
        return when (action) {
            is DeepLinkAction.StartBackup -> generateBackupLink(
                packages = action.packageNames,
                includeData = action.includeData,
                includeApk = action.includeApk
            )
            
            is DeepLinkAction.RestoreSnapshot -> generateRestoreLink(
                snapshotId = action.snapshotId,
                packages = action.packageNames
            )
            
            is DeepLinkAction.OpenSettings -> generateSettingsLink()
            
            is DeepLinkAction.OpenSettingsScreen -> generateSettingsLink(action.screen)
            
            is DeepLinkAction.ConnectCloudProvider -> generateCloudConnectLink(
                provider = action.provider,
                autoConnect = action.autoConnect
            )
            
            is DeepLinkAction.OpenAppDetails -> generateAppDetailsLink(action.packageName)
            
            is DeepLinkAction.OpenDashboard -> "obsidianbackup://dashboard"
            is DeepLinkAction.OpenBackups -> "obsidianbackup://backups"
            is DeepLinkAction.OpenAutomation -> "obsidianbackup://automation"
            is DeepLinkAction.OpenLogs -> "obsidianbackup://logs"
            is DeepLinkAction.OpenCloudSettings -> "obsidianbackup://cloud/settings"
            
            is DeepLinkAction.Invalid -> throw IllegalArgumentException("Cannot generate QR code for invalid action")
        }
    }
    
    /**
     * Convert custom scheme to HTTPS App Link
     */
    fun toAppLink(customUri: String): String {
        return customUri.replace("obsidianbackup://", "https://obsidianbackup.app/")
    }
    
    /**
     * Create a shareable text with multiple formats
     */
    fun createShareableText(action: DeepLinkAction, includeAppLink: Boolean = true): String {
        val qrData = generateQRCodeData(action)
        
        return buildString {
            appendLine("Open in ObsidianBackup:")
            appendLine()
            appendLine("Direct link:")
            appendLine(qrData)
            
            if (includeAppLink) {
                appendLine()
                appendLine("Web link:")
                appendLine(toAppLink(qrData))
            }
        }
    }
    
    /**
     * Validate a generated deep link
     */
    fun validateGeneratedLink(deepLink: String): Boolean {
        return try {
            val uri = Uri.parse(deepLink)
            uri.scheme in listOf("obsidianbackup", "https") &&
            (uri.host?.isEmpty() == true || uri.host == "obsidianbackup.app")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Add source tracking parameter to deep link
     */
    fun addSourceTracking(deepLink: String, source: String): String {
        val uri = Uri.parse(deepLink)
        return uri.buildUpon()
            .appendQueryParameter("source", source)
            .build()
            .toString()
    }
    
    /**
     * Generate HTML anchor tag for email/web
     */
    fun generateHtmlLink(action: DeepLinkAction, linkText: String, useAppLink: Boolean = false): String {
        val uri = if (useAppLink) {
            toAppLink(generateQRCodeData(action))
        } else {
            generateQRCodeData(action)
        }
        
        return """<a href="$uri">$linkText</a>"""
    }
    
    /**
     * Generate markdown link
     */
    fun generateMarkdownLink(action: DeepLinkAction, linkText: String, useAppLink: Boolean = false): String {
        val uri = if (useAppLink) {
            toAppLink(generateQRCodeData(action))
        } else {
            generateQRCodeData(action)
        }
        
        return "[$linkText]($uri)"
    }
}

/**
 * Extension functions for easy deep link generation
 */

fun DeepLinkAction.toDeepLink(): String {
    return DeepLinkGenerator.generateQRCodeData(this)
}

fun DeepLinkAction.toAppLink(): String {
    return DeepLinkGenerator.toAppLink(this.toDeepLink())
}

fun DeepLinkAction.copyToClipboard(context: Context) {
    DeepLinkGenerator.copyToClipboard(context, this.toDeepLink())
}

fun DeepLinkAction.share(context: Context) {
    val shareText = DeepLinkGenerator.createShareableText(this)
    DeepLinkGenerator.shareDeepLink(context, shareText, "Share ObsidianBackup Link")
}
