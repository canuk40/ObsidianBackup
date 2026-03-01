package com.obsidianbackup.data.repository

import com.obsidianbackup.automation.BackupFrequency
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.storage.BackupScheduleDao
import com.obsidianbackup.storage.BackupScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class Schedule(
    val id: String,
    val name: String,
    val frequency: BackupFrequency,
    val hour: Int,
    val minute: Int,
    val appIds: List<AppId>,
    val components: Set<BackupComponent>,
    val requiresCharging: Boolean,
    val requiresWifi: Boolean,
    val enabled: Boolean,
    val lastRun: Long?,
    val nextRun: Long?,
    val createdAt: Long
)

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: BackupScheduleDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toSchedule() }
        }
    }

    fun getEnabledSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getEnabledSchedules().map { entities ->
            entities.map { it.toSchedule() }
        }
    }

    suspend fun getSchedule(id: String): Schedule? {
        return scheduleDao.getSchedule(id)?.toSchedule()
    }

    suspend fun createSchedule(
        name: String,
        frequency: BackupFrequency,
        hour: Int,
        minute: Int,
        appIds: List<AppId>,
        components: Set<BackupComponent>,
        requiresCharging: Boolean = true,
        requiresWifi: Boolean = false
    ): Schedule {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val nextRun = calculateNextRun(frequency, hour, minute)

        val schedule = Schedule(
            id = id,
            name = name,
            frequency = frequency,
            hour = hour,
            minute = minute,
            appIds = appIds,
            components = components,
            requiresCharging = requiresCharging,
            requiresWifi = requiresWifi,
            enabled = true,
            lastRun = null,
            nextRun = nextRun,
            createdAt = now
        )

        scheduleDao.insertSchedule(schedule.toEntity())
        return schedule
    }

    suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.updateSchedule(schedule.toEntity())
    }

    suspend fun deleteSchedule(id: String) {
        scheduleDao.deleteSchedule(id)
    }

    suspend fun setScheduleEnabled(id: String, enabled: Boolean) {
        scheduleDao.setScheduleEnabled(id, enabled)
    }

    suspend fun updateScheduleRunTimes(id: String, lastRun: Long, nextRun: Long) {
        scheduleDao.updateScheduleRunTimes(id, lastRun, nextRun)
    }

    suspend fun getDueSchedules(): List<Schedule> {
        val currentTime = System.currentTimeMillis()
        return scheduleDao.getDueSchedules(currentTime).map { it.toSchedule() }
    }

    private fun BackupScheduleEntity.toSchedule(): Schedule {
        val appIds = json.decodeFromString<List<String>>(appIdsJson).map { AppId(it) }
        val components = json.decodeFromString<List<String>>(componentsJson)
            .mapNotNull { try { BackupComponent.valueOf(it) } catch (e: IllegalArgumentException) { null } }.toSet()
        
        // Parse frequency and time from the frequency string
        val (freq, hour, minute) = parseFrequencyString(frequency)

        return Schedule(
            id = id,
            name = name,
            frequency = freq,
            hour = hour,
            minute = minute,
            appIds = appIds,
            components = components,
            requiresCharging = componentsJson.contains("CHARGING"),
            requiresWifi = componentsJson.contains("WIFI"),
            enabled = enabled,
            lastRun = lastRun,
            nextRun = nextRun,
            createdAt = createdAt
        )
    }

    private fun Schedule.toEntity(): BackupScheduleEntity {
        val appIdsJson = json.encodeToString(appIds.map { it.value })
        val componentsJson = json.encodeToString(
            components.map { it.name } + 
            listOfNotNull(
                if (requiresCharging) "CHARGING" else null,
                if (requiresWifi) "WIFI" else null
            )
        )
        val frequencyString = "${frequency.name}:$hour:$minute"

        return BackupScheduleEntity(
            id = id,
            name = name,
            frequency = frequencyString,
            enabled = enabled,
            appIdsJson = appIdsJson,
            componentsJson = componentsJson,
            lastRun = lastRun,
            nextRun = nextRun,
            createdAt = createdAt
        )
    }

    private fun parseFrequencyString(freqString: String): Triple<BackupFrequency, Int, Int> {
        val parts = freqString.split(":")
        return if (parts.size >= 3) {
            Triple(
                BackupFrequency.valueOf(parts[0]),
                parts[1].toIntOrNull() ?: 0,
                parts[2].toIntOrNull() ?: 0
            )
        } else {
            Triple(BackupFrequency.valueOf(parts[0]), 0, 0)
        }
    }

    private fun calculateNextRun(frequency: BackupFrequency, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            when (frequency) {
                BackupFrequency.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                BackupFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                BackupFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }
}
