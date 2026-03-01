// plugins/api/PluginException.kt
package com.obsidianbackup.plugins.api

sealed class PluginException(message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    data class PluginLoadFailed(val packageName: String, override val cause: Throwable) :
        PluginException("Failed to load plugin: $packageName", cause)

    data class UnsupportedProvider(val providerId: String) :
        PluginException("Unsupported cloud provider: $providerId")

    data class InitializationFailed(override val cause: Throwable) :
        PluginException("Plugin initialization failed", cause)

    data class CapabilityNotSupported(val capability: String) :
        PluginException("Plugin capability not supported: $capability")

    data class VersionMismatch(val required: PluginApiVersion, val provided: PluginApiVersion) :
        PluginException("Plugin API version mismatch. Required: $required, Provided: $provided")

    data class ValidationFailed(val reasons: List<String>) :
        PluginException("Plugin validation failed: ${reasons.joinToString(", ")}")
}
