// model/Entitlements.kt
package com.titanbackup.model

class Entitlements(private val currentTier: FeatureTier) {

    fun isFeatureAvailable(feature: FeatureId): Boolean {
        return when (feature) {
            FeatureId.BASIC_BACKUP -> true
            FeatureId.BASIC_RESTORE -> true
            FeatureId.EXPORTABLE_LOGS -> true
            FeatureId.INCREMENTAL_BACKUP -> currentTier == FeatureTier.PRO
            FeatureId.CLOUD_SYNC -> currentTier == FeatureTier.PRO
            FeatureId.ENCRYPTION -> currentTier == FeatureTier.PRO
            FeatureId.AUTOMATION -> currentTier == FeatureTier.PRO
            FeatureId.DEVICE_TO_DEVICE_MIGRATION -> currentTier == FeatureTier.PRO
            FeatureId.BATCH_OPERATIONS -> currentTier == FeatureTier.PRO
            FeatureId.ADVANCED_COMPRESSION -> currentTier == FeatureTier.PRO
            FeatureId.SCHEDULED_BACKUPS -> currentTier == FeatureTier.PRO
            FeatureId.MULTIPLE_BACKUP_DESTINATIONS -> currentTier == FeatureTier.PRO
        }
    }

    fun getFeaturesList(tier: FeatureTier): List<FeatureId> {
        return FeatureId.values().filter {
            Entitlements(tier).isFeatureAvailable(it)
        }
    }
}