// plugins/api/PluginCapability.kt
package com.obsidianbackup.plugins.api

sealed class PluginCapability {
    // Backup Engine Capabilities
    object IncrementalBackup : PluginCapability()
    object EncryptionSupport : PluginCapability()
    object CompressionSupport : PluginCapability()

    // Cloud Provider Capabilities
    object MultiRegionSupport : PluginCapability()
    object BandwidthThrottling : PluginCapability()
    object ClientSideEncryption : PluginCapability()

    // Automation Capabilities
    object BackgroundExecution : PluginCapability()
    object ScheduledExecution : PluginCapability()
    object SystemEventHooks : PluginCapability()
    object NetworkAwareness : PluginCapability()

    // Export Capabilities
    object StreamingExport : PluginCapability()
    object BatchExport : PluginCapability()
    object CustomFormatSupport : PluginCapability()
}
