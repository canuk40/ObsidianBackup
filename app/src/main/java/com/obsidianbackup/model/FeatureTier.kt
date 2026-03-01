package com.obsidianbackup.model

/**
 * Feature tiers — retained for internal feature categorisation only.
 * No billing is enforced in this FOSS build; FeatureGateService grants
 * all features unconditionally.
 */
enum class FeatureTier {
    FREE, PRO, TEAM, ENTERPRISE;

    val isSubscription: Boolean
        get() = this != FREE
}

/**
 * Feature identifiers for access control
 */
enum class FeatureId(
    val displayName: String,
    val description: String,
    val minimumTier: FeatureTier
) {
    // Free tier features
    BASIC_BACKUP(
        displayName = "Basic Backup",
        description = "Backup apps and data",
        minimumTier = FeatureTier.FREE
    ),
    BASIC_RESTORE(
        displayName = "Basic Restore",
        description = "Restore backed up apps",
        minimumTier = FeatureTier.FREE
    ),
    
    // Pro tier features
    INCREMENTAL_BACKUPS(
        displayName = "Incremental Backups",
        description = "Only backup changed files",
        minimumTier = FeatureTier.PRO
    ),
    CLOUD_SYNC(
        displayName = "Cloud Sync",
        description = "Sync to cloud storage providers",
        minimumTier = FeatureTier.PRO
    ),
    ENCRYPTION(
        displayName = "Encryption",
        description = "AES-256 encryption for backups",
        minimumTier = FeatureTier.PRO
    ),
    RESTORE_SIMULATION(
        displayName = "Restore Simulation",
        description = "Test restores without applying",
        minimumTier = FeatureTier.PRO
    ),
    AUTOMATION(
        displayName = "Automation",
        description = "Scheduled and event-based backups",
        minimumTier = FeatureTier.PRO
    ),
    PARALLEL_OPERATIONS(
        displayName = "Parallel Operations",
        description = "Faster backups with parallel processing",
        minimumTier = FeatureTier.PRO
    ),
    EXPORTABLE_LOGS(
        displayName = "Exportable Logs",
        description = "Export detailed backup logs",
        minimumTier = FeatureTier.PRO
    ),
    GAMING_BACKUPS(
        displayName = "Gaming Backups",
        description = "Backup emulator data, saves, and ROMs",
        minimumTier = FeatureTier.PRO
    ),
    HEALTH_DATA_SYNC(
        displayName = "Health Data Sync",
        description = "Sync Health Connect data",
        minimumTier = FeatureTier.PRO
    ),
    ML_SCHEDULING(
        displayName = "Smart Scheduling",
        description = "ML-powered backup scheduling",
        minimumTier = FeatureTier.PRO
    ),
    PLUGIN_DISCOVERY(
        displayName = "Plugin Discovery",
        description = "Discover and install third-party plugins",
        minimumTier = FeatureTier.PRO
    ),
    SYNCTHING_SYNC(
        displayName = "Syncthing Sync",
        description = "Peer-to-peer device sync",
        minimumTier = FeatureTier.PRO
    ),
    ADVANCED_COMPRESSION(
        displayName = "Advanced Compression",
        description = "LZMA, Brotli, and custom formats",
        minimumTier = FeatureTier.PRO
    ),
    POST_QUANTUM_CRYPTO(
        displayName = "Post-Quantum Encryption",
        description = "Future-proof cryptographic algorithms",
        minimumTier = FeatureTier.PRO
    ),
    UNLIMITED_PROFILES(
        displayName = "Unlimited Profiles",
        description = "Create unlimited backup profiles (Free: 3 max)",
        minimumTier = FeatureTier.PRO
    ),
    UNLIMITED_BATCH_SIZE(
        displayName = "Unlimited Batch Size",
        description = "Backup unlimited apps at once (Free: 10 max)",
        minimumTier = FeatureTier.PRO
    ),
    TASKER_INTEGRATION(
        displayName = "Tasker Integration",
        description = "Automate backups with Tasker",
        minimumTier = FeatureTier.PRO
    ),
    
    // Team tier features
    DEVICE_TO_DEVICE_MIGRATION(
        displayName = "Device Migration",
        description = "Transfer data between devices",
        minimumTier = FeatureTier.TEAM
    ),
    MULTI_DEVICE_MANAGEMENT(
        displayName = "Multi-Device Management",
        description = "Manage backups across multiple devices",
        minimumTier = FeatureTier.TEAM
    ),
    SHARED_BACKUPS(
        displayName = "Shared Backups",
        description = "Share backups with team members",
        minimumTier = FeatureTier.TEAM
    ),
    PRIORITY_RESTORE(
        displayName = "Priority Restore",
        description = "Faster restore with priority queues",
        minimumTier = FeatureTier.TEAM
    ),
    
    // Enterprise features
    CUSTOM_RETENTION(
        displayName = "Custom Retention",
        description = "Custom backup retention policies",
        minimumTier = FeatureTier.ENTERPRISE
    ),
    API_ACCESS(
        displayName = "API Access",
        description = "Programmatic access via REST API",
        minimumTier = FeatureTier.ENTERPRISE
    ),
    ADVANCED_AUDIT_LOGGING(
        displayName = "Advanced Audit Logging",
        description = "Comprehensive audit trails and compliance logs",
        minimumTier = FeatureTier.ENTERPRISE
    ),
    CUSTOM_PLUGIN_API(
        displayName = "Custom Plugin API",
        description = "Build and deploy custom enterprise plugins",
        minimumTier = FeatureTier.ENTERPRISE
    ),
    MDM_INTEGRATION(
        displayName = "MDM Integration",
        description = "Mobile Device Management integration",
        minimumTier = FeatureTier.ENTERPRISE
    ),
    DEDICATED_SUPPORT(
        displayName = "Dedicated Support",
        description = "24/7 priority support",
        minimumTier = FeatureTier.ENTERPRISE
    );
}

/**
 * Feature availability checker with tier-based access control.
 * Subscription gates are always enforced regardless of build type.
 */
class FeatureFlags {
    fun isFeatureAvailable(feature: FeatureId, tier: FeatureTier): Boolean {
        return when {
            tier == FeatureTier.ENTERPRISE -> true // Enterprise has all features
            tier.ordinal >= feature.minimumTier.ordinal -> true
            else -> false
        }
    }

    fun getUnavailableReason(feature: FeatureId, currentTier: FeatureTier): String {
        return when {
            isFeatureAvailable(feature, currentTier) -> ""
            feature.minimumTier == FeatureTier.ENTERPRISE ->
                "Contact us for Enterprise access"
            else ->
                "Upgrade to ${feature.minimumTier.displayName} to unlock this feature"
        }
    }

    fun getFeaturesByTier(tier: FeatureTier): List<FeatureId> {
        return FeatureId.values().filter { isFeatureAvailable(it, tier) }
    }
}
