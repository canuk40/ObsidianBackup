package com.obsidianbackup.model

enum class BackupComponent {
    APK,           // Application package
    DATA,          // App data (/data/data)
    EXTERNAL_DATA, // External storage data
    EXTERNAL,      // External storage (alias)
    OBB,           // OBB files
    CACHE,         // Cache directory
    MEDIA;         // Media files
    
    companion object {
        val DEFAULT = setOf(APK, DATA, OBB)
        val ALL = values().toSet()
    }
}
