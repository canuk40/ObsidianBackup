// data/repository/BackupProfileRepository.kt
package com.obsidianbackup.data.repository

import com.obsidianbackup.model.BackupProfile
import com.obsidianbackup.model.BackupProfileId
import com.obsidianbackup.storage.BackupProfileDao
import com.obsidianbackup.storage.toDomain
import com.obsidianbackup.storage.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for backup profile management
 */
interface BackupProfileRepository {
    suspend fun getProfile(profileId: BackupProfileId): BackupProfile?
    fun observeProfile(profileId: BackupProfileId): Flow<BackupProfile?>
    suspend fun getAllProfiles(): List<BackupProfile>
    fun observeAllProfiles(): Flow<List<BackupProfile>>
    suspend fun getEnabledProfiles(): List<BackupProfile>
    suspend fun getScheduledProfiles(): List<BackupProfile>
    suspend fun saveProfile(profile: BackupProfile)
    suspend fun updateProfile(profile: BackupProfile)
    suspend fun deleteProfile(profileId: BackupProfileId)
    suspend fun setEnabled(profileId: BackupProfileId, enabled: Boolean)
    suspend fun setScheduleEnabled(profileId: BackupProfileId, enabled: Boolean)
    suspend fun updateLastBackupTimestamp(profileId: BackupProfileId, timestamp: Long)
    suspend fun getProfileCount(): Int
    suspend fun getEnabledProfileCount(): Int
}

@Singleton
class BackupProfileRepositoryImpl @Inject constructor(
    private val profileDao: BackupProfileDao
) : BackupProfileRepository {
    
    private val tag = "BackupProfileRepo"
    
    override suspend fun getProfile(profileId: BackupProfileId): BackupProfile? {
        return try {
            profileDao.getProfile(profileId.value)?.toDomain()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to get profile: ${profileId.value}")
            null
        }
    }
    
    override fun observeProfile(profileId: BackupProfileId): Flow<BackupProfile?> {
        return profileDao.observeProfile(profileId.value).map { it?.toDomain() }
    }
    
    override suspend fun getAllProfiles(): List<BackupProfile> {
        return try {
            profileDao.getAllProfiles().map { it.toDomain() }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to get all profiles")
            emptyList()
        }
    }
    
    override fun observeAllProfiles(): Flow<List<BackupProfile>> {
        return profileDao.observeAllProfiles().map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun getEnabledProfiles(): List<BackupProfile> {
        return try {
            profileDao.getEnabledProfiles().map { it.toDomain() }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to get enabled profiles")
            emptyList()
        }
    }
    
    override suspend fun getScheduledProfiles(): List<BackupProfile> {
        return try {
            profileDao.getScheduledProfiles().map { it.toDomain() }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to get scheduled profiles")
            emptyList()
        }
    }
    
    override suspend fun saveProfile(profile: BackupProfile) {
        try {
            profileDao.insert(profile.toEntity())
            Timber.tag(tag).d("Saved profile: ${profile.name} (${profile.id.value})")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to save profile: ${profile.name}")
            throw e
        }
    }
    
    override suspend fun updateProfile(profile: BackupProfile) {
        try {
            val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
            profileDao.update(updatedProfile.toEntity())
            Timber.tag(tag).d("Updated profile: ${profile.name} (${profile.id.value})")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to update profile: ${profile.name}")
            throw e
        }
    }
    
    override suspend fun deleteProfile(profileId: BackupProfileId) {
        try {
            profileDao.delete(profileId.value)
            Timber.tag(tag).d("Deleted profile: ${profileId.value}")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to delete profile: ${profileId.value}")
            throw e
        }
    }
    
    override suspend fun setEnabled(profileId: BackupProfileId, enabled: Boolean) {
        try {
            profileDao.setEnabled(profileId.value, enabled)
            Timber.tag(tag).d("Set profile ${profileId.value} enabled: $enabled")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to set enabled state")
            throw e
        }
    }
    
    override suspend fun setScheduleEnabled(profileId: BackupProfileId, enabled: Boolean) {
        try {
            profileDao.setScheduleEnabled(profileId.value, enabled)
            Timber.tag(tag).d("Set profile ${profileId.value} schedule enabled: $enabled")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to set schedule enabled state")
            throw e
        }
    }
    
    override suspend fun updateLastBackupTimestamp(profileId: BackupProfileId, timestamp: Long) {
        try {
            profileDao.updateLastBackupTimestamp(profileId.value, timestamp)
            Timber.tag(tag).d("Updated last backup timestamp for ${profileId.value}")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to update last backup timestamp")
            throw e
        }
    }
    
    override suspend fun getProfileCount(): Int {
        return try {
            profileDao.getProfileCount()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to get profile count")
            0
        }
    }
    
    override suspend fun getEnabledProfileCount(): Int {
        return try {
            profileDao.getEnabledProfileCount()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Failed to get enabled profile count")
            0
        }
    }
}
