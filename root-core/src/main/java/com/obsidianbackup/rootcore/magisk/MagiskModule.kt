package com.obsidianbackup.rootcore.magisk

import kotlinx.serialization.Serializable

/**
 * Data class representing a Magisk module.
 *
 * Ported from ObsidianBox v31 production code.
 */
@Serializable
data class MagiskModule(
    val id: String,
    val name: String,
    val version: String = "",
    val author: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val isRemoved: Boolean = false,
    val path: String = ""
)
