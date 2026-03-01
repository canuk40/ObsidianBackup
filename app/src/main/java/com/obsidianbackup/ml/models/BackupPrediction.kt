// ml/models/BackupPrediction.kt
package com.obsidianbackup.ml.models

import com.obsidianbackup.model.AppId
import java.time.LocalDateTime

/**
 * Represents a predicted backup opportunity
 */
data class BackupPrediction(
    val id: String,
    val predictedTime: LocalDateTime,
    val confidence: Float,
    val estimatedMinutesUntil: Long,
    val suggestedAppIds: List<AppId>,
    val estimatedSizeMb: Long,
    val suggestsCloudSync: Boolean,
    val reason: String
)
