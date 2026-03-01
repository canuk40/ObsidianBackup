// storage/migrations/DatabaseMigrations.kt
package com.obsidianbackup.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add encryption metadata columns
            database.execSQL("""
                ALTER TABLE snapshots
                ADD COLUMN encryption_algorithm TEXT DEFAULT 'none'
            """.trimIndent())

            database.execSQL("""
                ALTER TABLE snapshots
                ADD COLUMN encryption_key_id TEXT
            """.trimIndent())

            database.execSQL("""
                ALTER TABLE snapshots
                ADD COLUMN encryption_iv BLOB
            """.trimIndent())
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add Merkle tree root hash
            database.execSQL("""
                ALTER TABLE snapshots
                ADD COLUMN merkle_root_hash TEXT
            """.trimIndent())

            // Add incremental backup support
            database.execSQL("""
                ALTER TABLE snapshots
                ADD COLUMN base_snapshot_id TEXT
            """.trimIndent())

            database.execSQL("""
                ALTER TABLE snapshots
                ADD COLUMN is_incremental INTEGER DEFAULT 0
            """.trimIndent())

            // Create index for faster incremental lookups
            database.execSQL("""
                CREATE INDEX idx_base_snapshot
                ON snapshots(base_snapshot_id)
            """.trimIndent())
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add app-level backup metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS app_backups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    snapshot_id TEXT NOT NULL,
                    app_id TEXT NOT NULL,
                    apk_size INTEGER NOT NULL,
                    data_size INTEGER NOT NULL,
                    obb_size INTEGER DEFAULT 0,
                    external_size INTEGER DEFAULT 0,
                    backup_timestamp INTEGER NOT NULL,
                    components_json TEXT NOT NULL,
                    FOREIGN KEY(snapshot_id) REFERENCES snapshots(id) ON DELETE CASCADE
                )
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX idx_app_backups_snapshot
                ON app_backups(snapshot_id)
            """.trimIndent())
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add settings table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY NOT NULL,
                    value TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())

            // Add schedule table for automation
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS backup_schedules (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    frequency TEXT NOT NULL,
                    enabled INTEGER DEFAULT 1,
                    app_ids_json TEXT NOT NULL,
                    components_json TEXT NOT NULL,
                    last_run INTEGER,
                    next_run INTEGER,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }
    
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Performance optimization: Add indexes for common queries
            
            // Snapshots table indexes
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_snapshot_timestamp 
                ON snapshots(timestamp)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_snapshot_base 
                ON snapshots(baseSnapshotId)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_snapshot_incremental 
                ON snapshots(isIncremental)
            """.trimIndent())
            
            // App backups table indexes
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_app_backup_snapshot 
                ON app_backups(snapshot_id)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_app_backup_app_id 
                ON app_backups(app_id)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_app_backup_timestamp 
                ON app_backups(backup_timestamp)
            """.trimIndent())
        }
    }
    
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add logs table for application logging
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    operation_type TEXT NOT NULL,
                    level TEXT NOT NULL,
                    message TEXT NOT NULL,
                    details TEXT,
                    snapshot_id TEXT
                )
            """.trimIndent())
            
            // Add indexes for efficient log queries
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_log_timestamp 
                ON logs(timestamp)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_log_level 
                ON logs(level)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_log_operation 
                ON logs(operation_type)
            """.trimIndent())
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // App labels
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS app_labels (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_label_name ON app_labels(name)")

            // Label-to-app assignments (many-to-many)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS app_label_assignments (
                    app_id TEXT NOT NULL,
                    label_id TEXT NOT NULL,
                    PRIMARY KEY(app_id, label_id)
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_assignment_label ON app_label_assignments(label_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_assignment_app ON app_label_assignments(app_id)")

            // Blacklist
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS backup_blacklist (
                    id TEXT PRIMARY KEY NOT NULL,
                    app_id TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_blacklist_app ON backup_blacklist(app_id)")

            // Backup notes
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS backup_notes (
                    id TEXT PRIMARY KEY NOT NULL,
                    snapshot_id TEXT NOT NULL,
                    note_text TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_note_snapshot ON backup_notes(snapshot_id)")

            // Protected backups
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS protected_backups (
                    id TEXT PRIMARY KEY NOT NULL,
                    snapshot_id TEXT NOT NULL,
                    protected_at INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_protected_snapshot ON protected_backups(snapshot_id)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add backup profiles table for scheduled/smart backups
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS backup_profiles (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    appIds TEXT NOT NULL,
                    components TEXT NOT NULL,
                    incremental INTEGER NOT NULL DEFAULT 0,
                    compressionLevel INTEGER NOT NULL DEFAULT 6,
                    encryptionEnabled INTEGER NOT NULL DEFAULT 0,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    scheduleEnabled INTEGER NOT NULL DEFAULT 0,
                    scheduleCron TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    lastBackupTimestamp INTEGER,
                    cloudSyncEnabled INTEGER NOT NULL DEFAULT 0,
                    cloudProviderId TEXT
                )
            """.trimIndent())
            
            // Add indexes for efficient profile queries
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_profile_enabled 
                ON backup_profiles(isEnabled)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_profile_schedule_enabled 
                ON backup_profiles(scheduleEnabled)
            """.trimIndent())
            
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_profile_created 
                ON backup_profiles(createdAt)
            """.trimIndent())
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9
    )
}
