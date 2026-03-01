// storage/BackupCatalog.kt
package com.obsidianbackup.storage

import android.content.Context
import android.util.Base64
import androidx.room.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.migrations.DatabaseMigrations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import javax.crypto.KeyGenerator

// Room Database Entities - Optimized with indexes
@Entity(
    tableName = "snapshots",
    indices = [
        Index(value = ["timestamp"], name = "idx_snapshot_timestamp"),
        Index(value = ["baseSnapshotId"], name = "idx_snapshot_base"),
        Index(value = ["isIncremental"], name = "idx_snapshot_incremental")
    ]
)
data class SnapshotEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val description: String?,
    val totalSize: Long,
    val compressionRatio: Float,
    val encrypted: Boolean,
    val verified: Boolean,
    val permissionMode: String,
    val deviceInfoJson: String,
    val appsJson: String,
    val componentsJson: String,
    val checksumsJson: String,
    val encryptionAlgorithm: String? = null,
    val encryptionKeyId: String? = null,
    val encryptionIv: ByteArray? = null,
    val merkleRootHash: String? = null,
    val baseSnapshotId: String? = null,
    val isIncremental: Boolean = false
)

@Dao
interface SnapshotDao {
    // Optimized: Added LIMIT for pagination support
    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getSnapshotsPaged(limit: Int, offset: Int): Flow<List<SnapshotEntity>>
    
    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    // Optimized: Use indexed column
    @Query("SELECT * FROM snapshots WHERE id = :id LIMIT 1")
    suspend fun getSnapshotById(id: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)
    
    // Optimized: Batch insert for better performance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<SnapshotEntity>)

    @Delete
    suspend fun deleteSnapshot(snapshot: SnapshotEntity)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: String)
    
    // Optimized: Batch delete
    @Query("DELETE FROM snapshots WHERE id IN (:ids)")
    suspend fun deleteSnapshotsByIds(ids: List<String>)

    @Query("SELECT SUM(totalSize) FROM snapshots")
    suspend fun getTotalBackupSize(): Long?

    @Query("SELECT COUNT(*) FROM snapshots")
    suspend fun getSnapshotCount(): Int

    // Optimized: Uses index on baseSnapshotId
    @Query("SELECT * FROM snapshots WHERE baseSnapshotId = :baseSnapshotId")
    suspend fun getIncrementalSnapshots(baseSnapshotId: String): List<SnapshotEntity>

    // Optimized: Uses index on isIncremental
    @Query("SELECT * FROM snapshots WHERE isIncremental = 0 ORDER BY timestamp DESC")
    suspend fun getFullSnapshots(): List<SnapshotEntity>
    
    // Optimized: Query only necessary fields for list display
    @Query("SELECT id, timestamp, description, totalSize, encrypted FROM snapshots ORDER BY timestamp DESC")
    suspend fun getSnapshotSummaries(): List<SnapshotSummary>
    
    // Optimized: Date range query using indexed timestamp
    @Query("SELECT * FROM snapshots WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getSnapshotsByDateRange(startTime: Long, endTime: Long): List<SnapshotEntity>
}

// Lightweight data class for list displays - reduces memory usage
data class SnapshotSummary(
    val id: String,
    val timestamp: Long,
    val description: String?,
    val totalSize: Long,
    val encrypted: Boolean
)

@Database(
    entities = [
        SnapshotEntity::class,
        AppBackupEntity::class,
        SettingsEntity::class,
        BackupScheduleEntity::class,
        LogEntity::class,
        AppLabelEntity::class,
        AppLabelAssignment::class,
        BlacklistEntity::class,
        BackupNoteEntity::class,
        ProtectedBackupEntity::class,
        BackupProfileEntity::class
    ],
    version = 9  // Incremented for backup profiles
)
abstract class BackupDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun appBackupDao(): AppBackupDao
    abstract fun settingsDao(): SettingsDao
    abstract fun scheduleDao(): BackupScheduleDao
    abstract fun logDao(): LogDao
    abstract fun labelDao(): LabelDao
    abstract fun profileDao(): BackupProfileDao

    companion object {
        @Volatile
        private var INSTANCE: BackupDatabase? = null

        fun getInstance(context: Context): BackupDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): BackupDatabase {
            // 1. Load SQLCipher native library
            // SOURCE: https://github.com/sqlcipher/sqlcipher-android#usage
            System.loadLibrary("sqlcipher")

            // 2. Retrieve or generate a persistent passphrase stored in EncryptedSharedPreferences
            // SOURCE: https://github.com/Lenz-K/android-encrypted-room-database-example
            val passphrase = getOrCreatePassphrase(context)

            // 3. Create SQLCipher SupportOpenHelperFactory
            // SOURCE: https://github.com/sqlcipher/sqlcipher-android#room-integration
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                BackupDatabase::class.java,
                "titan_backup.db"
            )
                .openHelperFactory(factory)          // ← THIS IS THE ONLY LINE THAT CHANGES
                .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade()
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        /**
         * Generate a 256-bit AES key on first run, store in EncryptedSharedPreferences,
         * and retrieve on subsequent runs. The key is backed by the Android Keystore.
         *
         * SOURCE: https://github.com/Lenz-K/android-encrypted-room-database-example
         * SOURCE: https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
         */
        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            val prefs = EncryptedSharedPreferences.create(
                "backup_catalog_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val key = "db_passphrase"
            val existing = prefs.getString(key, null)
            if (existing != null) {
                return Base64.decode(existing, Base64.DEFAULT)
            }

            // Generate new 256-bit AES passphrase
            // SOURCE: https://github.com/Lenz-K/android-encrypted-room-database-example#passphrase-generation
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val newPassphrase = keyGenerator.generateKey().encoded

            prefs.edit()
                .putString(key, Base64.encodeToString(newPassphrase, Base64.DEFAULT))
                .apply()

            return newPassphrase
        }
        
        // Clear instance for testing
        internal fun clearInstance() {
            INSTANCE = null
        }
    }
}

// Backup Catalog Manager
class BackupCatalog(context: Context, private val backupRootPath: String) {

    private val database = BackupDatabase.getInstance(context)

    private val snapshotDao = database.snapshotDao()
    private val json = Json { prettyPrint = true }
    
    /**
     * Get LogDao for logging operations
     */
    fun getLogDao(): LogDao = database.logDao()

    /**
     * Get LabelDao for label, blacklist, note, and protection operations
     */
    fun getLabelDao(): LabelDao = database.labelDao()

    /**
     * Get ProfileDao for backup profile operations
     */
    fun getProfileDao(): BackupProfileDao = database.profileDao()

    /**
     * Get AppBackupDao for app-level backup operations
     */
    fun getAppBackupDao(): AppBackupDao = database.appBackupDao()

    /**
     * Get ScheduleDao for schedule operations
     */
    fun getScheduleDao(): BackupScheduleDao = database.scheduleDao()

    /**
     * Save snapshot metadata to catalog
     */
    suspend fun saveSnapshot(metadata: BackupMetadata) {
        val entity = SnapshotEntity(
            id = metadata.snapshotId.value,
            timestamp = metadata.timestamp,
            description = metadata.description,
            totalSize = metadata.totalSize,
            compressionRatio = calculateCompressionRatio(metadata),
            encrypted = metadata.encrypted,
            verified = false,
            permissionMode = metadata.permissionMode,
            deviceInfoJson = json.encodeToString(metadata.deviceInfo),
            appsJson = json.encodeToString(metadata.apps.map { it.value }),
            componentsJson = json.encodeToString(metadata.components.map { it.name }),
            checksumsJson = json.encodeToString(metadata.checksums),
            merkleRootHash = metadata.merkleRootHash
        )

        snapshotDao.insertSnapshot(entity)

        // Also save as JSON file for portability
        val metadataFile = File(backupRootPath, "${metadata.snapshotId.value}/metadata.json")
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(json.encodeToString(metadata))
    }

    /**
     * Get snapshot metadata
     */
    suspend fun getSnapshot(id: BackupId): BackupMetadata? {
        val entity = snapshotDao.getSnapshotById(id.value) ?: return null

        return BackupMetadata(
            snapshotId = BackupId(entity.id),
            timestamp = entity.timestamp,
            description = entity.description,
            apps = json.decodeFromString<List<String>>(entity.appsJson).map { AppId(it) },
            components = json.decodeFromString<List<String>>(entity.componentsJson)
                .mapNotNull { try { BackupComponent.valueOf(it) } catch (e: IllegalArgumentException) { null } }.toSet(),
            compressionLevel = 6, // Default
            encrypted = entity.encrypted,
            permissionMode = entity.permissionMode,
            deviceInfo = json.decodeFromString(entity.deviceInfoJson),
            totalSize = entity.totalSize,
            checksums = json.decodeFromString(entity.checksumsJson),
            merkleRootHash = entity.merkleRootHash
        )
    }
    
    /**
     * Get snapshot metadata (alias for getSnapshot)
     */
    suspend fun getSnapshotMetadata(id: BackupId): BackupMetadata? = getSnapshot(id)
    
    /**
     * Get backup metadata (alias for getSnapshot)
     */
    suspend fun getBackupMetadata(id: BackupId): com.obsidianbackup.model.BackupMetadata? {
        val metadata = getSnapshot(id) ?: return null
        
        // Convert storage.BackupMetadata to model.BackupMetadata
        return com.obsidianbackup.model.BackupMetadata(
            snapshotId = metadata.snapshotId,
            timestamp = metadata.timestamp,
            description = metadata.description ?: "",
            appIds = metadata.apps,
            components = metadata.components,
            compressionLevel = metadata.compressionLevel,
            encrypted = metadata.encrypted,
            totalSize = metadata.totalSize,
            deviceInfo = metadata.deviceInfo
        )
    }

    /**
     * Get all snapshots
     */
    fun getAllSnapshots(): Flow<List<BackupSnapshot>> {
        return snapshotDao.getAllSnapshots().map { entities: List<SnapshotEntity> ->
            entities.map { entity ->
                BackupSnapshot(
                    id = BackupId(entity.id),
                    timestamp = entity.timestamp,
                    description = entity.description,
                    apps = try {
                        json.decodeFromString<List<String>>(entity.appsJson).map { pkgName ->
                            AppInfo(
                                appId = AppId(pkgName),
                                packageName = pkgName,
                                appName = pkgName.substringAfterLast('.'),
                                versionName = "",
                                versionCode = 0L,
                                isSystemApp = false,
                                dataSize = 0L,
                                apkSize = 0L,
                                lastUpdateTime = 0L
                            )
                        }
                    } catch (_: Exception) { emptyList() },
                    totalSize = entity.totalSize,
                    compressionRatio = entity.compressionRatio,
                    encrypted = entity.encrypted,
                    verified = entity.verified,
                    permissionMode = entity.permissionMode,
                    deviceInfo = json.decodeFromString(entity.deviceInfoJson)
                )
            }
        }
    }
    
    /**
     * Get all snapshots synchronously (for non-Flow contexts)
     */
    suspend fun getAllBackupsSync(): List<BackupSnapshot> {
        return getAllSnapshots().first()
    }

    /**
     * Delete snapshot from catalog
     */
    suspend fun deleteSnapshot(id: BackupId) {
        snapshotDao.deleteSnapshotById(id.value)
    }
    
    /**
     * Delete backup from catalog (alias for deleteSnapshot)
     */
    suspend fun deleteBackup(id: BackupId) {
        deleteSnapshot(id)
    }

    /**
     * Mark snapshot as verified
     */
    suspend fun markVerified(id: BackupId, verified: Boolean) {
        val entity = snapshotDao.getSnapshotById(id.value) ?: return
        snapshotDao.insertSnapshot(entity.copy(verified = verified))
    }

    /**
     * Get total backup size
     */
    suspend fun getTotalBackupSize(): Long {
        return snapshotDao.getTotalBackupSize() ?: 0L
    }

    /**
     * Get snapshot count
     */
    suspend fun getSnapshotCount(): Int {
        return snapshotDao.getSnapshotCount()
    }

    /**
     * Get last full backup containing a specific app
     */
    suspend fun getLastFullBackupForApp(appId: AppId): BackupId? {
        val fullSnapshots = snapshotDao.getFullSnapshots()
        
        for (snapshot in fullSnapshots) {
            val apps = json.decodeFromString<List<String>>(snapshot.appsJson)
            if (apps.contains(appId.value)) {
                return BackupId(snapshot.id)
            }
        }
        
        return null
    }

    /**
     * Import snapshot from JSON file
     */
    suspend fun importSnapshot(metadataFile: File): BackupMetadata? {
        return try {
            val jsonText = metadataFile.readText()
            val metadata = json.decodeFromString<BackupMetadata>(jsonText)
            saveSnapshot(metadata)
            metadata
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get snapshots that contain a specific app (for history trimming).
     */
    suspend fun getSnapshotsForApp(appId: String): List<SnapshotEntity> {
        val appBackupDao = database.appBackupDao()
        val snapshotIds = appBackupDao.getSnapshotIdsForApp(appId)
        return snapshotIds.mapNotNull { snapshotDao.getSnapshotById(it) }
    }

    /**
     * Get all unique app IDs that have been backed up.
     */
    suspend fun getAllBackedUpAppIds(): List<String> {
        val appBackupDao = database.appBackupDao()
        return appBackupDao.getAllDistinctAppIds()
    }

    /**
     * Delete snapshot by string ID.
     */
    suspend fun deleteSnapshot(snapshotId: String) {
        snapshotDao.deleteSnapshotById(snapshotId)
    }

    /**
     * Get snapshot directory on disk
     */
    fun getSnapshotDirectory(id: BackupId): File {
        return File(backupRootPath, id.value)
    }

    /**
     * Mark snapshot as synced to a cloud provider (writes a lightweight marker file)
     */
    suspend fun markSyncedToCloud(id: BackupId, providerId: String, timestamp: Long) {
        try {
            val dir = getSnapshotDirectory(id)
            if (!dir.exists()) dir.mkdirs()
            val marker = File(dir, ".synced_$providerId")
            marker.writeText(timestamp.toString())
        } catch (_: Exception) {
            // ignore write failures for marker
        }
    }

    /**
     * Return list of snapshot IDs that have not been synced to any configured provider
     * For now, consider a snapshot pending if it lacks any .synced_* files
     */
    suspend fun getPendingCloudSync(): List<BackupId> {
        val allSnapshots = getAllSnapshots().first()
        return allSnapshots.filter { snapshot ->
            val dir = getSnapshotDirectory(snapshot.id)
            val hasMarker = dir.listFiles()?.any { it.name.startsWith(".synced_") } == true
            !hasMarker
        }.map { it.id }
    }

    private fun calculateCompressionRatio(metadata: BackupMetadata): Float {
        // Calculate based on original vs compressed size
        // For now, return estimated value
        return 0.65f
    }
}

// Serializable metadata model
@Serializable
data class BackupMetadata(
    val snapshotId: @Serializable(with = BackupIdSerializer::class) BackupId,
    val timestamp: Long,
    val description: String?,
    val apps: List<@Serializable(with = AppIdSerializer::class) AppId>,
    val components: Set<BackupComponent>,
    val compressionLevel: Int,
    val encrypted: Boolean,
    val permissionMode: String,
    val deviceInfo: DeviceInfo,
    val totalSize: Long,
    val checksums: Map<String, String>,
    val path: String = "",
    val merkleRootHash: String? = null
)

// Custom serializers for value classes
object BackupIdSerializer : kotlinx.serialization.KSerializer<BackupId> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("BackupId", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: BackupId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder) = BackupId(decoder.decodeString())
}

object AppIdSerializer : kotlinx.serialization.KSerializer<AppId> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("AppId", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: AppId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder) = AppId(decoder.decodeString())
}
