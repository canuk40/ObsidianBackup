// health/HealthDataExporter.kt
package com.obsidianbackup.health

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.security.audit.SecurityAuditLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataExporter @Inject constructor(
    private val context: Context,
    private val securityAuditLogger: SecurityAuditLogger,
    private val logger: ObsidianLogger
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val backupDir: File by lazy {
        File(context.filesDir, "health_data").apply { mkdirs() }
    }

    private val masterKeyAlias: String by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    companion object {
        private const val TAG = "HealthDataExporter"
        private const val SYSTEM_USER_ID = "system"
    }

    private fun writeEncrypted(file: File, content: String) {
        if (file.exists()) file.delete()
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        encryptedFile.openFileOutput().use { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    private fun readEncrypted(file: File): String {
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        return encryptedFile.openFileInput().use { it.readBytes().toString(Charsets.UTF_8) }
    }

    suspend fun exportSteps(format: ExportFormat): String = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        ""
    }

    suspend fun importSteps(filePath: String): List<StepsData> = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        emptyList()
    }

    suspend fun exportHeartRate(format: ExportFormat): String = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        ""
    }

    suspend fun importHeartRate(filePath: String): List<HeartRateData> = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        emptyList()
    }

    suspend fun exportSleep(format: ExportFormat): String = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        ""
    }

    suspend fun importSleep(filePath: String): List<SleepData> = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        emptyList()
    }

    suspend fun exportWorkouts(format: ExportFormat): String = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        ""
    }

    suspend fun importWorkouts(filePath: String): List<WorkoutData> = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        emptyList()
    }

    suspend fun exportNutrition(format: ExportFormat): String = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        ""
    }

    suspend fun importNutrition(filePath: String): List<NutritionData> = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        emptyList()
    }

    suspend fun exportBodyMeasurements(format: ExportFormat): String = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        ""
    }

    suspend fun importBodyMeasurements(filePath: String): Triple<List<WeightData>, List<HeightData>, List<BodyFatData>> =
        withContext(Dispatchers.IO) {
            // Health Connect removed — pending org account setup
            // This feature will be re-enabled in a future release
            Triple(emptyList(), emptyList(), emptyList())
        }

    suspend fun exportAllToJSON(
        dataTypes: Set<HealthDataType>,
        outputPath: String
    ) = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        logger.w(TAG, "exportAllToJSON called but Health Connect is removed")
    }

    suspend fun exportAllToCSV(
        dataTypes: Set<HealthDataType>,
        outputPath: String
    ) = withContext(Dispatchers.IO) {
        // Health Connect removed — pending org account setup
        // This feature will be re-enabled in a future release
        logger.w(TAG, "exportAllToCSV called but Health Connect is removed")
    }
}

@Serializable
data class StepsData(val count: Long, val startTime: String, val endTime: String, val metadata: String)

@Serializable
data class HeartRateData(val samples: List<HeartRateSample>, val startTime: String, val endTime: String)

@Serializable
data class HeartRateSample(val bpm: Long, val time: String)

@Serializable
data class SleepData(
    val title: String?,
    val notes: String?,
    val startTime: String,
    val endTime: String,
    val stages: List<SleepStageData>
)

@Serializable
data class SleepStageData(val stage: Int, val startTime: String, val endTime: String)

@Serializable
data class WorkoutData(
    val exerciseType: Int,
    val title: String?,
    val notes: String?,
    val startTime: String,
    val endTime: String
)

@Serializable
data class NutritionData(
    val name: String?,
    val mealType: Int,
    val energy: Double?,
    val protein: Double?,
    val carbohydrates: Double?,
    val fat: Double?,
    val time: String
)

@Serializable
data class BodyMeasurementsData(
    val weights: List<WeightData>,
    val heights: List<HeightData>,
    val bodyFats: List<BodyFatData>
)

@Serializable
data class WeightData(val kilograms: Double, val time: String)

@Serializable
data class HeightData(val meters: Double, val time: String)

@Serializable
data class BodyFatData(val percentage: Double, val time: String)

val ExportFormat.extension: String
    get() = when (this) {
        ExportFormat.JSON -> "json"
        ExportFormat.CSV -> "csv"
    }
