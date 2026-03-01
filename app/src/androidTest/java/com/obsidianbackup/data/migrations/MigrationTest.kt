// MigrationTest.kt
// SOURCE: https://developer.android.com/training/data-storage/room/migrating-db-versions#test
// SOURCE: https://medium.com/androiddevelopers/testing-room-migrations-be93cdb0d975

package com.obsidianbackup.data.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.obsidianbackup.storage.BackupDatabase
import com.obsidianbackup.storage.migrations.DatabaseMigrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test-backup"
    }

    // MigrationTestHelper reads schema JSON files from androidTest/assets/
    // SOURCE: https://developer.android.com/reference/android/arch/persistence/room/testing/MigrationTestHelper
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BackupDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test all migrations in sequence from v1 → v9.
     * SOURCE: https://developer.android.com/training/data-storage/room/migrating-db-versions#test-migrations
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).apply { close() }

        val db = androidx.room.Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BackupDatabase::class.java,
            TEST_DB
        )
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()

        db.openHelper.writableDatabase.close()
    }

    /** Test Migration 1 → 2 with data integrity check. */
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert v1 schema data before migration
            execSQL("INSERT INTO snapshots (id, name, timestamp) VALUES ('snap-001', 'Test Snapshot', 1700000000000)")
            close()
        }
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)
        // Validate data survived migration
        val cursor = db.query("SELECT * FROM snapshots WHERE id = 'snap-001'")
        assert(cursor.moveToFirst()) { "Row must survive migration 1→2" }
        cursor.close()
        db.close()
    }

    /** Test Migration 2 → 3. */
    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        var db = helper.createDatabase(TEST_DB, 2).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, DatabaseMigrations.MIGRATION_2_3)
        db.close()
    }

    /** Test Migration 3 → 4. */
    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB, 3).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, DatabaseMigrations.MIGRATION_3_4)
        db.close()
    }

    /** Test Migration 4 → 5. */
    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        var db = helper.createDatabase(TEST_DB, 4).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, DatabaseMigrations.MIGRATION_4_5)
        db.close()
    }

    /** Test Migration 5 → 6. */
    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, DatabaseMigrations.MIGRATION_5_6)
        db.close()
    }

    /** Test Migration 6 → 7. */
    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        var db = helper.createDatabase(TEST_DB, 6).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 7, true, DatabaseMigrations.MIGRATION_6_7)
        db.close()
    }

    /** Test Migration 7 → 8. */
    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        var db = helper.createDatabase(TEST_DB, 7).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, DatabaseMigrations.MIGRATION_7_8)
        db.close()
    }

    /** Test Migration 8 → 9 (adds backup_profiles table). */
    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        var db = helper.createDatabase(TEST_DB, 8).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, DatabaseMigrations.MIGRATION_8_9)
        // Verify backup_profiles table was created
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='backup_profiles'")
        assert(cursor.moveToFirst()) { "backup_profiles table must exist after migration 8→9" }
        cursor.close()
        db.close()
    }
}
