package com.obsidianbackup.domain.datatypes

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.obsidianbackup.rootcore.shell.ShellExecutor
import com.obsidianbackup.rootcore.shell.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS/MMS backup and restore engine.
 *
 * Root approach: reads directly from content://sms and content://mms via ContentResolver
 * (root grants READ_SMS automatically via RootPermissionGranter).
 * Restore requires temporarily becoming the default SMS app OR using root to insert directly.
 *
 * Follows Swift Backup's "default SMS handler dance" pattern but prefers root insert.
 */
@Singleton
class SmsBackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellExecutor: ShellExecutor,
    private val json: Json
) {
    companion object {
        private const val TAG = "[SmsBackup]"
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.SEEN,
            Telephony.Sms.STATUS,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.SUBSCRIPTION_ID
        )
    }

    @Serializable
    data class SmsMessage(
        val address: String,
        val body: String,
        val date: Long,
        val dateSent: Long,
        val type: Int,
        val read: Int,
        val seen: Int,
        val status: Int,
        val threadId: Long,
        val subscriptionId: Int = -1
    )

    @Serializable
    data class MmsMessage(
        val id: Long,
        val date: Long,
        val dateSent: Long,
        val messageBox: Int,
        val read: Int,
        val seen: Int,
        val threadId: Long,
        val addresses: List<String>,
        val parts: List<MmsPart>
    )

    @Serializable
    data class MmsPart(
        val contentType: String,
        val text: String? = null,
        val dataFile: String? = null
    )

    @Serializable
    data class SmsBackupData(
        val smsMessages: List<SmsMessage>,
        val mmsMessages: List<MmsMessage>,
        val backupTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Backup all SMS messages to a JSON file.
     * Uses root to grant READ_SMS if not already granted.
     */
    suspend fun backupSms(outputFile: File): Result<Int> = runCatching {
        grantSmsPermissions()

        val resolver = context.contentResolver
        val messages = mutableListOf<SmsMessage>()

        resolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            null, null,
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                messages.add(cursorToSms(cursor))
            }
        }

        Timber.d("$TAG Backed up ${messages.size} SMS messages")
        val data = SmsBackupData(smsMessages = messages, mmsMessages = emptyList())
        outputFile.writeText(json.encodeToString(data))
        messages.size
    }

    /**
     * Backup all MMS messages. MMS is more complex — has addresses and multi-part content.
     */
    suspend fun backupMms(outputFile: File): Result<Int> = runCatching {
        grantSmsPermissions()

        val resolver = context.contentResolver
        val mmsMessages = mutableListOf<MmsMessage>()

        resolver.query(
            Telephony.Mms.CONTENT_URI,
            null, null, null,
            "${Telephony.Mms.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms._ID))
                val mms = readMmsMessage(resolver, id, cursor)
                if (mms != null) mmsMessages.add(mms)
            }
        }

        Timber.d("$TAG Backed up ${mmsMessages.size} MMS messages")
        val smsData = if (outputFile.exists()) {
            json.decodeFromString<SmsBackupData>(outputFile.readText())
        } else {
            SmsBackupData(smsMessages = emptyList(), mmsMessages = emptyList())
        }
        outputFile.writeText(json.encodeToString(smsData.copy(mmsMessages = mmsMessages)))
        mmsMessages.size
    }

    /**
     * Full backup: SMS + MMS combined.
     */
    suspend fun backupAll(outputFile: File): Result<SmsBackupData> = runCatching {
        grantSmsPermissions()
        val resolver = context.contentResolver

        // SMS
        val smsMessages = mutableListOf<SmsMessage>()
        resolver.query(
            Telephony.Sms.CONTENT_URI, SMS_PROJECTION,
            null, null, "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                smsMessages.add(cursorToSms(cursor))
            }
        }

        // MMS
        val mmsMessages = mutableListOf<MmsMessage>()
        resolver.query(
            Telephony.Mms.CONTENT_URI, null, null, null,
            "${Telephony.Mms.DATE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms._ID))
                readMmsMessage(resolver, id, cursor)?.let { mmsMessages.add(it) }
            }
        }

        val data = SmsBackupData(smsMessages, mmsMessages)
        outputFile.writeText(json.encodeToString(data))
        Timber.d("$TAG Full backup: ${smsMessages.size} SMS, ${mmsMessages.size} MMS")
        data
    }

    /**
     * Restore SMS messages via root (insert directly into content://sms).
     * Root approach avoids the "default SMS handler dance" entirely.
     */
    suspend fun restoreSms(inputFile: File, skipDuplicates: Boolean = true): Result<Int> = runCatching {
        grantSmsPermissions()
        val data = json.decodeFromString<SmsBackupData>(inputFile.readText())
        val resolver = context.contentResolver
        var restored = 0

        for (sms in data.smsMessages) {
            if (skipDuplicates && isDuplicateSms(resolver, sms)) continue

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sms.address)
                put(Telephony.Sms.BODY, sms.body)
                put(Telephony.Sms.DATE, sms.date)
                put(Telephony.Sms.DATE_SENT, sms.dateSent)
                put(Telephony.Sms.TYPE, sms.type)
                put(Telephony.Sms.READ, sms.read)
                put(Telephony.Sms.SEEN, sms.seen)
                put(Telephony.Sms.STATUS, sms.status)
            }

            // Root insert: use content command via shell for non-default-SMS-app insert
            val result = shellExecutor.executeRoot(
                "content insert --uri content://sms " +
                    "--bind address:s:${sms.address} " +
                    "--bind body:s:${sms.body.replace("'", "\\'")} " +
                    "--bind date:l:${sms.date} " +
                    "--bind date_sent:l:${sms.dateSent} " +
                    "--bind type:i:${sms.type} " +
                    "--bind read:i:${sms.read} " +
                    "--bind seen:i:${sms.seen}"
            )
            if (result.success) restored++
        }

        Timber.d("$TAG Restored $restored/${data.smsMessages.size} SMS messages")
        restored
    }

    private fun cursorToSms(cursor: Cursor): SmsMessage {
        return SmsMessage(
            address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "",
            body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: "",
            date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)),
            dateSent = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT)),
            type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)),
            read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)),
            seen = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.SEEN)),
            status = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS)),
            threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)),
            subscriptionId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))
        )
    }

    private fun readMmsMessage(resolver: ContentResolver, mmsId: Long, cursor: Cursor): MmsMessage? {
        return try {
            val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE))
            val dateSent = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE_SENT))
            val messageBox = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX))
            val read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.READ))
            val seen = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.SEEN))
            val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID))

            val addresses = readMmsAddresses(resolver, mmsId)
            val parts = readMmsParts(resolver, mmsId)

            MmsMessage(mmsId, date, dateSent, messageBox, read, seen, threadId, addresses, parts)
        } catch (e: Exception) {
            Timber.w(e, "$TAG Failed to read MMS $mmsId")
            null
        }
    }

    private fun readMmsAddresses(resolver: ContentResolver, mmsId: Long): List<String> {
        val addresses = mutableListOf<String>()
        resolver.query(
            Uri.parse("content://mms/$mmsId/addr"),
            arrayOf(Telephony.Mms.Addr.ADDRESS),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val addr = cursor.getString(0) ?: continue
                if (addr.isNotBlank() && !addr.contains("insert-address")) {
                    addresses.add(addr)
                }
            }
        }
        return addresses
    }

    private fun readMmsParts(resolver: ContentResolver, mmsId: Long): List<MmsPart> {
        val parts = mutableListOf<MmsPart>()
        resolver.query(
            Uri.parse("content://mms/$mmsId/part"),
            arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.TEXT),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val contentType = cursor.getString(1) ?: continue
                val text = cursor.getString(2)
                parts.add(MmsPart(contentType = contentType, text = text))
            }
        }
        return parts
    }

    private fun isDuplicateSms(resolver: ContentResolver, sms: SmsMessage): Boolean {
        return resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.ADDRESS}=? AND ${Telephony.Sms.DATE}=? AND ${Telephony.Sms.TYPE}=?",
            arrayOf(sms.address, sms.date.toString(), sms.type.toString()),
            null
        )?.use { it.count > 0 } ?: false
    }

    private suspend fun grantSmsPermissions() {
        shellExecutor.executeRoot("pm grant ${context.packageName} android.permission.READ_SMS")
    }
}
