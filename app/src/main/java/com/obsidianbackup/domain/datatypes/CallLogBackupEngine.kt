package com.obsidianbackup.domain.datatypes

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import com.obsidianbackup.rootcore.shell.ShellExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call log backup and restore engine.
 * Uses ContentResolver to read call history, root shell to insert on restore.
 */
@Singleton
class CallLogBackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellExecutor: ShellExecutor,
    private val json: Json
) {
    companion object {
        private const val TAG = "[CallLogBackup]"
        private val PROJECTION = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.NEW,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_NUMBER_TYPE,
            CallLog.Calls.GEOCODED_LOCATION,
            CallLog.Calls.PHONE_ACCOUNT_ID,
            CallLog.Calls.IS_READ
        )
    }

    @Serializable
    data class CallEntry(
        val number: String,
        val date: Long,
        val duration: Long,
        val type: Int,
        val isNew: Int,
        val cachedName: String? = null,
        val cachedNumberType: Int = 0,
        val geocodedLocation: String? = null,
        val phoneAccountId: String? = null,
        val isRead: Int = 1
    )

    @Serializable
    data class CallLogBackupData(
        val entries: List<CallEntry>,
        val backupTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Backup all call log entries.
     */
    suspend fun backup(outputFile: File): Result<Int> = runCatching {
        grantCallLogPermissions()
        val resolver = context.contentResolver
        val entries = mutableListOf<CallEntry>()

        resolver.query(
            CallLog.Calls.CONTENT_URI,
            PROJECTION,
            null, null,
            "${CallLog.Calls.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                entries.add(
                    CallEntry(
                        number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "",
                        date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)),
                        duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)),
                        type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)),
                        isNew = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.NEW)),
                        cachedName = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)),
                        cachedNumberType = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NUMBER_TYPE)),
                        geocodedLocation = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)),
                        phoneAccountId = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)),
                        isRead = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.IS_READ))
                    )
                )
            }
        }

        val data = CallLogBackupData(entries)
        outputFile.writeText(json.encodeToString(data))
        Timber.d("$TAG Backed up ${entries.size} call log entries")
        entries.size
    }

    /**
     * Restore call log entries via root `content insert`.
     */
    suspend fun restore(inputFile: File, skipDuplicates: Boolean = true): Result<Int> = runCatching {
        grantCallLogPermissions()
        val data = json.decodeFromString<CallLogBackupData>(inputFile.readText())
        val resolver = context.contentResolver
        var restored = 0

        for (entry in data.entries) {
            if (skipDuplicates && isDuplicate(resolver, entry)) continue

            val result = shellExecutor.executeRoot(
                "content insert --uri content://call_log/calls " +
                    "--bind number:s:${entry.number} " +
                    "--bind date:l:${entry.date} " +
                    "--bind duration:l:${entry.duration} " +
                    "--bind type:i:${entry.type} " +
                    "--bind new:i:${entry.isNew} " +
                    "--bind is_read:i:${entry.isRead}"
            )
            if (result.success) restored++
        }

        Timber.d("$TAG Restored $restored/${data.entries.size} call log entries")
        restored
    }

    private fun isDuplicate(resolver: ContentResolver, entry: CallEntry): Boolean {
        return resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID),
            "${CallLog.Calls.NUMBER}=? AND ${CallLog.Calls.DATE}=? AND ${CallLog.Calls.TYPE}=?",
            arrayOf(entry.number, entry.date.toString(), entry.type.toString()),
            null
        )?.use { it.count > 0 } ?: false
    }

    private suspend fun grantCallLogPermissions() {
        shellExecutor.executeRoot("pm grant ${context.packageName} android.permission.READ_CALL_LOG")
        shellExecutor.executeRoot("pm grant ${context.packageName} android.permission.WRITE_CALL_LOG")
    }
}
