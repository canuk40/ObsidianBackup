package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.plugins.api.PluginMetadata

interface Plugin {
    val id: String
    val metadata: PluginMetadata
    fun onLoad()
    fun onUnload()
}
