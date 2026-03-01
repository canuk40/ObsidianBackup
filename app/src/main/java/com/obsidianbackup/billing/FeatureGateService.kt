package com.obsidianbackup.billing

import com.obsidianbackup.model.FeatureId
import com.obsidianbackup.model.FeatureTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS build: all features are unlocked for every user.
 * This stub replaces the Google Play Billing gating that existed in the
 * commercial version. No network calls, no subscriptions, no paywalls.
 */
@Singleton
class FeatureGateService @Inject constructor() {

    /** All features are always accessible in the FOSS build. */
    suspend fun checkAccess(featureId: FeatureId): Boolean = true

    /** Emits true for every feature. */
    fun isFeatureAvailable(featureId: FeatureId): Flow<Boolean> = flowOf(true)

    /** No feature ever requires an upgrade. */
    fun requiresUpgrade(featureId: FeatureId, currentTier: FeatureTier): Boolean = false

    /** Every feature is available at the FREE tier. */
    fun getMinimumTier(featureId: FeatureId): FeatureTier = FeatureTier.FREE

    /** All features are available to any tier. */
    fun getAvailableFeatures(tier: FeatureTier): List<FeatureId> = FeatureId.values().toList()

    /** No upgrade message — features are always available. */
    fun getUnavailableReason(featureId: FeatureId, currentTier: FeatureTier): String = ""
}
