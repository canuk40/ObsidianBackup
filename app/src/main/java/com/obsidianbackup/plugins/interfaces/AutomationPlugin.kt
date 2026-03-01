// plugins/interfaces/AutomationPlugin.kt
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow

interface AutomationPlugin {

    val metadata: PluginMetadata

    /**
     * Get available triggers
     */
    fun getAvailableTriggers(): List<AutomationTrigger>

    /**
     * Register a trigger
     */
    suspend fun registerTrigger(trigger: AutomationTrigger, config: TriggerConfig): kotlin.Result<String>

    /**
     * Unregister a trigger
     */
    suspend fun unregisterTrigger(triggerId: String): kotlin.Result<Unit>

    /**
     * Get active triggers
     */
    suspend fun getActiveTriggers(): List<ActiveTrigger>

    /**
     * Execute automation action
     */
    suspend fun executeAction(action: AutomationAction): kotlin.Result<Unit>

    /**
     * Observe trigger events
     */
    fun observeTriggerEvents(): Flow<TriggerEvent>
}

data class AutomationTrigger(
    val id: String,
    val name: String,
    val description: String,
    val configSchema: Map<String, TriggerConfigField>
)

data class TriggerConfigField(
    val key: String,
    val type: ConfigFieldType,
    val label: String,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val options: List<String> = emptyList()
)

data class TriggerConfig(
    val values: Map<String, Any>
)

data class ActiveTrigger(
    val id: String,
    val triggerId: String,
    val config: TriggerConfig,
    val enabled: Boolean = true
)

data class AutomationAction(
    val type: ActionType,
    val parameters: Map<String, Any>
)

enum class ActionType {
    BACKUP_APPS, RESTORE_SNAPSHOT, VERIFY_INTEGRITY, SYNC_TO_CLOUD
}

data class TriggerEvent(
    val triggerId: String,
    val timestamp: Long,
    val data: Map<String, Any>
)
