package com.obsidianbackup.wear.data

/**
 * Data layer paths for communication between phone and watch
 */
object DataLayerPaths {
    const val BACKUP_STATUS_PATH = "/backup_status"
    const val BACKUP_TRIGGER_PATH = "/backup_trigger"
    const val BACKUP_PROGRESS_PATH = "/backup_progress"
    const val SETTINGS_PATH = "/settings"
    const val CAPABILITY_PHONE_APP = "obsidian_backup_phone_app"
}

/**
 * Message types for Data Layer communication
 */
object MessageTypes {
    const val REQUEST_BACKUP = "request_backup"
    const val REQUEST_STATUS = "request_status"
    const val CANCEL_BACKUP = "cancel_backup"
}
