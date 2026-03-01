package com.obsidianbackup.testing

/**
 * Test constants and configuration.
 */
object TestConstants {
    const val TEST_TIMEOUT_MS = 10000L
    const val TEST_PACKAGE_NAME = "com.obsidianbackup.test"
    
    // Test file paths
    const val TEST_BACKUP_DIR = "/test/backups"
    const val TEST_SOURCE_DIR = "/test/source"
    const val TEST_RESTORE_DIR = "/test/restore"
    
    // Test credentials
    const val TEST_PASSWORD = "test_password_123"
    const val TEST_EMAIL = "test@example.com"
    
    // Test data sizes
    const val SMALL_FILE_SIZE = 1024L // 1KB
    const val MEDIUM_FILE_SIZE = 1024L * 100 // 100KB
    const val LARGE_FILE_SIZE = 1024L * 1024 * 10 // 10MB
    
    // Test counts
    const val SMALL_FILE_COUNT = 100
    const val MEDIUM_FILE_COUNT = 50
    const val LARGE_FILE_COUNT = 10
}
