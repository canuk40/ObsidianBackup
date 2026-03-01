package com.obsidianbackup.domain.backup

import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.LabelDao
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Batch filter that applies labels, blacklist, and size limits before backup.
 * Integrates label, blacklist, and size-limit filtering into the backup flow.
 */
@Singleton
class BatchFilter @Inject constructor(
    private val labelDao: LabelDao,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "BatchFilter"
        const val DEFAULT_MAX_DATA_SIZE_MB = 500L // Skip apps with data > 500MB in batch
    }

    data class FilterConfig(
        val labelIds: List<String> = emptyList(),
        val respectBlacklist: Boolean = true,
        val maxDataSizeMb: Long = DEFAULT_MAX_DATA_SIZE_MB,
        val onlyUpdated: Boolean = false
    )

    data class FilterResult(
        val included: List<String>,
        val excluded: Map<String, String> // appId -> reason
    )

    /**
     * Filter a list of app IDs based on labels, blacklist, and size limits.
     *
     * @param allAppIds all candidate app IDs
     * @param config filtering configuration
     * @param appDataSizes map of appId to data size in bytes (optional, for size filtering)
     */
    suspend fun filter(
        allAppIds: List<String>,
        config: FilterConfig = FilterConfig(),
        appDataSizes: Map<String, Long> = emptyMap()
    ): FilterResult {
        val excluded = mutableMapOf<String, String>()
        var candidates = allAppIds.toMutableList()

        // 1. Label filter — only include apps with matching labels
        if (config.labelIds.isNotEmpty()) {
            val labelAppIds = config.labelIds.flatMap { labelId ->
                labelDao.getAppIdsForLabelSync(labelId)
            }.toSet()

            val beforeCount = candidates.size
            candidates = candidates.filter { it in labelAppIds }.toMutableList()
            val removedByLabel = beforeCount - candidates.size
            if (removedByLabel > 0) {
                Timber.d("$TAG Label filter removed $removedByLabel apps")
            }
        }

        // 2. Blacklist filter
        if (config.respectBlacklist) {
            val hiddenIds = labelDao.getHiddenAppIds().toSet()
            val apkOnlyIds = labelDao.getApkOnlyAppIds().toSet()

            candidates.forEach { appId ->
                if (appId in hiddenIds) {
                    excluded[appId] = "Blacklisted (HIDE)"
                }
            }
            candidates = candidates.filter { it !in hiddenIds }.toMutableList()
        }

        // 3. Size limit filter
        if (config.maxDataSizeMb > 0 && appDataSizes.isNotEmpty()) {
            val maxBytes = config.maxDataSizeMb * 1024 * 1024
            candidates.forEach { appId ->
                val size = appDataSizes[appId] ?: 0
                if (size > maxBytes) {
                    excluded[appId] = "Data too large (${size / (1024 * 1024)} MB > ${config.maxDataSizeMb} MB)"
                }
            }
            candidates = candidates.filter { appId ->
                val size = appDataSizes[appId] ?: 0
                size <= maxBytes
            }.toMutableList()
        }

        Timber.i("$TAG Filter result: ${candidates.size} included, ${excluded.size} excluded")
        return FilterResult(included = candidates, excluded = excluded)
    }
}
