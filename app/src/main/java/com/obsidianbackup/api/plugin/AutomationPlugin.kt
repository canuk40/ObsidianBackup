// api/plugin/AutomationPlugin.kt
package com.obsidianbackup.api.plugin

import kotlinx.coroutines.flow.Flow

/**
 * Plugin that provides automation triggers and actions
 * Examples: Tasker integration, webhooks, custom triggers
 */
interface AutomationPlugin : ObsidianBackupPlugin {

    /**
     * Get supported trigger types
     */
    fun getSupportedTriggers(): List<TriggerType>

    /**
     * Get supported action types
     */
    fun getSupportedActions(): List<ActionType>

    /**
     * Register a trigger
     */
    suspend fun registerTrigger(trigger: AutomationTrigger): PluginResult<TriggerId>

    /**
     * Unregister a trigger
     */
    suspend fun unregisterTrigger(triggerId: TriggerId): PluginResult<Unit>

    /**
     * Execute an action
     */
    suspend fun executeAction(action: AutomationAction): PluginResult<ActionResult>

    /**
     * Observe trigger events
     */
    fun observeTriggerEvents(): Flow<TriggerEvent>
}

// Use plain data classes to avoid requiring kotlinx.serialization opt-in for JsonElement
data class TriggerType(
    val id: String,
    val name: String,
    val description: String,
    val configurationSchemaJson: String? = null // JSON schema as string (optional)
)

data class ActionType(
    val id: String,
    val name: String,
    val description: String,
    val configurationSchemaJson: String? = null
)

data class AutomationTrigger(
    val type: String,
    val configuration: Map<String, Any> = emptyMap(),
    val actions: List<AutomationAction> = emptyList()
)

data class AutomationAction(
    val type: String,
    val configuration: Map<String, Any> = emptyMap()
)

@JvmInline
value class TriggerId(val value: String)

data class TriggerEvent(
    val triggerId: TriggerId,
    val timestamp: Long,
    val payload: Map<String, Any> = emptyMap()
)

data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val output: Map<String, Any> = emptyMap()
)
