// model/FeatureTier.kt
package com.titanbackup.model

enum class FeatureTier {
    FREE,
    PRO
}

enum class FeatureId {
    BASIC_BACKUP,
    BASIC_RESTORE,
    INCREMENTAL_BACKUP,
    CLOUD_SYNC,
    ENCRYPTION,
    AUTOMATION,
    EXPORTABLE_LOGS,
    DEVICE_TO_DEVICE_MIGRATION,
    BATCH_OPERATIONS,
    ADVANCED_COMPRESSION,
    SCHEDULED_BACKUPS,
    MULTIPLE_BACKUP_DESTINATIONS
}