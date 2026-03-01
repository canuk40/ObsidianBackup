// plugins/api/PluginMetadata.kt
package com.obsidianbackup.plugins.api

import android.content.pm.PackageInfo

data class PluginMetadata(
    val packageName: String,
    val className: String,
    val name: String,
    val description: String,
    val version: String,
    val apiVersion: PluginApiVersion,
    val capabilities: Set<PluginCapability>,
    val author: String,
    val website: String? = null,
    val minSdkVersion: Int = 24,
    val signatureSha256: String? = null // For signed plugins
)
