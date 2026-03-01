package com.obsidianbackup.community

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages changelog display and version history
 */
@Singleton
class ChangelogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger
) {
    
    private val _changelog = MutableStateFlow<List<ChangelogEntry>>(getHardcodedChangelog())
    val changelog: StateFlow<List<ChangelogEntry>> = _changelog.asStateFlow()
    
    fun getChangelogForVersion(version: String): ChangelogEntry? {
        return _changelog.value.find { it.version == version }
    }
    
    fun getLatestChanges(count: Int = 5): List<ChangelogEntry> {
        return _changelog.value.take(count)
    }
    
    private fun getHardcodedChangelog(): List<ChangelogEntry> {
        return listOf(
            ChangelogEntry(
                version = "1.0.0",
                versionCode = 1,
                releaseDate = "2024-01-15",
                changes = listOf(
                    Change(ChangeType.FEATURE, "Initial release of ObsidianBackup"),
                    Change(ChangeType.FEATURE, "Support for local backups"),
                    Change(ChangeType.FEATURE, "Google Drive integration"),
                    Change(ChangeType.FEATURE, "WebDAV support"),
                    Change(ChangeType.FEATURE, "Automated backup scheduling"),
                    Change(ChangeType.FEATURE, "End-to-end encryption"),
                    Change(ChangeType.FEATURE, "Incremental backups with Merkle trees"),
                    Change(ChangeType.FEATURE, "Biometric authentication"),
                    Change(ChangeType.FEATURE, "Deep linking support"),
                    Change(ChangeType.FEATURE, "Widget for quick backups")
                ),
                highlights = "🎉 Welcome to ObsidianBackup! Your data, secured and backed up with military-grade encryption."
            ),
            ChangelogEntry(
                version = "0.9.0-beta",
                versionCode = 9,
                releaseDate = "2024-01-01",
                changes = listOf(
                    Change(ChangeType.FEATURE, "Beta program management"),
                    Change(ChangeType.FEATURE, "In-app feedback system"),
                    Change(ChangeType.FEATURE, "Privacy-respecting analytics"),
                    Change(ChangeType.IMPROVEMENT, "Improved backup performance"),
                    Change(ChangeType.BUGFIX, "Fixed cloud sync issues")
                ),
                highlights = "Beta release with community features"
            )
        )
    }
}

data class ChangelogEntry(
    val version: String,
    val versionCode: Int,
    val releaseDate: String,
    val changes: List<Change>,
    val highlights: String? = null
)

data class Change(
    val type: ChangeType,
    val description: String
)

enum class ChangeType {
    FEATURE,
    IMPROVEMENT,
    BUGFIX,
    SECURITY,
    DEPRECATED
}
