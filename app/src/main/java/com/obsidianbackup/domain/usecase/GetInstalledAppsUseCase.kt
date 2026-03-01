// domain/usecase/GetInstalledAppsUseCase.kt
package com.obsidianbackup.domain.usecase

import com.obsidianbackup.data.repository.AppRepository
import com.obsidianbackup.model.AppInfo
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(includeSystemApps: Boolean = false): List<AppInfo> {
        return appRepository.scanInstalledApps(includeSystemApps)
    }
}
