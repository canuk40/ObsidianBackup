package com.obsidianbackup.domain.usecase

import com.obsidianbackup.billing.FeatureGateService
import com.obsidianbackup.data.repository.SettingsRepository
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.FeatureId
import com.obsidianbackup.storage.BackupCatalog
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces backup retention and storage limit policies by pruning old snapshots
 * from the catalog according to the user's configured rules.
 *
 * Called after each successful backup and can be triggered manually from the UI.
 */
@Singleton
class RetentionEnforcementUseCase @Inject constructor(
    private val backupCatalog: BackupCatalog,
    private val settingsRepository: SettingsRepository,
    private val featureGateService: FeatureGateService
) {
    companion object {
        private const val TAG = "[Retention]"
    }

    data class RetentionResult(
        val deletedCount: Int,
        val freedBytes: Long,
        val reason: String
    )

    /**
     * Run all active retention policies and return a summary of what was pruned.
     * Custom retention policies (count/age/storage) require an Enterprise subscription.
     */
    suspend fun enforce(): RetentionResult {
        // Domain-layer gate: custom retention requires Enterprise
        if (!featureGateService.checkAccess(FeatureId.CUSTOM_RETENTION)) {
            Timber.tag(TAG).w("Custom retention enforcement skipped — requires Enterprise subscription")
            return RetentionResult(0, 0L, "Custom retention requires an Enterprise subscription")
        }

        val mode = settingsRepository.retentionMode.first()
        val keepCount = settingsRepository.backupKeepCount.first()
        val retentionDays = settingsRepository.backupRetentionDays.first()
        val limitMb = settingsRepository.storageLimitMb.first()

        var totalDeleted = 0
        var totalFreed = 0L
        val reasons = mutableListOf<String>()

        val snapshots = backupCatalog.getAllBackupsSync()
            .sortedByDescending { it.timestamp }

        // --- Count-based retention ---
        if ((mode == "COUNT" || mode == "BOTH") && keepCount > 0) {
            val toDelete = snapshots.drop(keepCount)
            if (toDelete.isNotEmpty()) {
                toDelete.forEach { snap ->
                    backupCatalog.deleteSnapshot(snap.id)
                    totalFreed += snap.totalSize
                    totalDeleted++
                    Timber.d("$TAG Deleted snapshot ${snap.id.value} (count policy, kept $keepCount)")
                }
                reasons.add("count > $keepCount")
            }
        }

        // --- Age-based retention ---
        if ((mode == "DAYS" || mode == "BOTH") && retentionDays > 0) {
            val cutoffMs = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000
            // Re-fetch after possible count deletes
            val remaining = backupCatalog.getAllBackupsSync()
                .sortedByDescending { it.timestamp }
            val toDelete = remaining.filter { it.timestamp < cutoffMs }
            if (toDelete.isNotEmpty()) {
                toDelete.forEach { snap ->
                    backupCatalog.deleteSnapshot(snap.id)
                    totalFreed += snap.totalSize
                    totalDeleted++
                    Timber.d("$TAG Deleted snapshot ${snap.id.value} (age policy, older than $retentionDays days)")
                }
                reasons.add("older than $retentionDays days")
            }
        }

        // --- Storage limit enforcement ---
        if (limitMb > 0) {
            val limitBytes = limitMb * 1024L * 1024L
            val afterPrune = backupCatalog.getAllBackupsSync()
                .sortedByDescending { it.timestamp }
            var usedBytes = afterPrune.sumOf { it.totalSize }

            if (usedBytes > limitBytes) {
                // Delete oldest first until under limit
                val candidates = afterPrune.reversed() // oldest first
                for (snap in candidates) {
                    if (usedBytes <= limitBytes) break
                    backupCatalog.deleteSnapshot(snap.id)
                    usedBytes -= snap.totalSize
                    totalFreed += snap.totalSize
                    totalDeleted++
                    Timber.d("$TAG Deleted snapshot ${snap.id.value} (storage limit ${limitMb}MB)")
                }
                reasons.add("storage limit ${limitMb}MB")
            }
        }

        val reason = if (reasons.isEmpty()) "No pruning needed" else "Pruned: ${reasons.joinToString(", ")}"
        Timber.i("$TAG Enforcement complete: deleted=$totalDeleted, freed=${totalFreed / 1024 / 1024}MB")
        return RetentionResult(totalDeleted, totalFreed, reason)
    }

    /** Dry-run: returns how many snapshots would be deleted without actually deleting. */
    suspend fun preview(): RetentionResult {
        val mode = settingsRepository.retentionMode.first()
        val keepCount = settingsRepository.backupKeepCount.first()
        val retentionDays = settingsRepository.backupRetentionDays.first()
        val limitMb = settingsRepository.storageLimitMb.first()

        val snapshots = backupCatalog.getAllBackupsSync()
            .sortedByDescending { it.timestamp }

        val toDelete = mutableSetOf<BackupId>()

        if ((mode == "COUNT" || mode == "BOTH") && keepCount > 0) {
            snapshots.drop(keepCount).forEach { toDelete.add(it.id) }
        }

        if ((mode == "DAYS" || mode == "BOTH") && retentionDays > 0) {
            val cutoffMs = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000
            snapshots.filter { it.timestamp < cutoffMs }.forEach { toDelete.add(it.id) }
        }

        if (limitMb > 0) {
            val limitBytes = limitMb * 1024L * 1024L
            var usedBytes = snapshots.sumOf { it.totalSize }
            if (usedBytes > limitBytes) {
                for (snap in snapshots.reversed()) {
                    if (usedBytes <= limitBytes) break
                    toDelete.add(snap.id)
                    usedBytes -= snap.totalSize
                }
            }
        }

        val freedBytes = snapshots.filter { it.id in toDelete }.sumOf { it.totalSize }
        return RetentionResult(toDelete.size, freedBytes, "Preview only — no changes made")
    }
}
