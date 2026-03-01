// plugins/api/PluginApiVersion.kt
package com.obsidianbackup.plugins.api

@JvmInline
value class PluginApiVersion(val version: Int) {
    val major: Int get() = version shr 16
    val minor: Int get() = version and 0xFFFF

    companion object {
        val V1_0 = PluginApiVersion((1 shl 16) or 0)
        val CURRENT = V1_0
    }
}

fun pluginApiVersion(major: Int, minor: Int): PluginApiVersion {
    require(major in 0..65535 && minor in 0..65535)
    return PluginApiVersion((major shl 16) or minor)
}
