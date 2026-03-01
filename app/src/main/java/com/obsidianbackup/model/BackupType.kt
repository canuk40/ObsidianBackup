package com.obsidianbackup.model

/**
 * Type of backup operation
 */
enum class BackupType {
    /**
     * Full backup of all app data
     */
    FULL,
    
    /**
     * Incremental backup capturing only changed files since last backup
     * Requires PRO tier
     */
    INCREMENTAL
}
