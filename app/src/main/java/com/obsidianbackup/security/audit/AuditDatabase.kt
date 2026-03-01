// security/audit/AuditDatabase.kt
package com.obsidianbackup.security.audit

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * HIPAA-Compliant Encrypted Audit Database
 * 
 * Security Features:
 * - SQLCipher encryption (AES-256)
 * - Separate database from main app data (audit isolation)
 * - Encrypted with passphrase from Android Keystore
 * - Immutable logs (append-only)
 * 
 * Database Location:
 * - App-private storage (/data/data/com.obsidianbackup/databases/)
 * - NOT accessible to other apps
 * - NOT on external storage
 * 
 * HIPAA Compliance:
 * - 45 CFR § 164.312(b) - Audit Controls
 * - 45 CFR § 164.312(a)(2)(iv) - Encryption/Decryption
 */
@Database(
    entities = [AuditLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AuditDatabase : RoomDatabase() {
    
    abstract fun auditLogDao(): AuditLogDao
    
    companion object {
        private const val DATABASE_NAME = "obsidian_audit.db"
        
        @Volatile
        private var INSTANCE: AuditDatabase? = null
        
        /**
         * Get encrypted audit database instance
         * 
         * @param context Application context
         * @param passphrase Database encryption passphrase (from Keystore)
         * @return Encrypted AuditDatabase instance
         */
        fun getInstance(context: Context, passphrase: CharArray): AuditDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }
        
        /**
         * Build encrypted database with SQLCipher
         * 
         * @param context Application context
         * @param passphrase Encryption passphrase
         * @return AuditDatabase instance
         */
        private fun buildDatabase(context: Context, passphrase: CharArray): AuditDatabase {
            // SQLCipher factory for AES-256 encryption
            val factory = SupportOpenHelperFactory(String(passphrase).toByteArray(Charsets.UTF_8))
            
            return Room.databaseBuilder(
                context.applicationContext,
                AuditDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                // CRITICAL: Never allow main thread queries for security database
                // HIPAA § 164.312(b) — audit logs must never be silently wiped.
                // No fallback. If schema changes without a migration, the app will throw
                // IllegalStateException at runtime, which is far better than silent data loss.
                .build()
        }
        
        /**
         * Close database connection and clear instance
         * 
         * Use only during app shutdown or testing
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
