package com.obsidianbackup.domain.datatypes

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
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
 * Contacts backup and restore engine.
 *
 * Exports contacts as JSON (internal format) with optional VCF (vCard) export.
 * Root approach: grants READ_CONTACTS/WRITE_CONTACTS via pm grant.
 * Restore inserts raw contacts back via ContentResolver batch operations.
 */
@Singleton
class ContactsBackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellExecutor: ShellExecutor,
    private val json: Json
) {
    companion object {
        private const val TAG = "[ContactsBackup]"
    }

    @Serializable
    data class Contact(
        val displayName: String?,
        val phones: List<Phone> = emptyList(),
        val emails: List<Email> = emptyList(),
        val addresses: List<Address> = emptyList(),
        val organization: String? = null,
        val title: String? = null,
        val note: String? = null,
        val starred: Boolean = false
    )

    @Serializable
    data class Phone(val number: String, val type: Int = 0, val label: String? = null)

    @Serializable
    data class Email(val address: String, val type: Int = 0, val label: String? = null)

    @Serializable
    data class Address(val formatted: String, val type: Int = 0, val label: String? = null)

    @Serializable
    data class ContactsBackupData(
        val contacts: List<Contact>,
        val backupTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Backup all contacts.
     */
    suspend fun backup(outputFile: File): Result<Int> = runCatching {
        grantContactsPermissions()
        val resolver = context.contentResolver
        val contacts = mutableListOf<Contact>()

        // Get all contact IDs
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.STARRED
            ),
            null, null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                val starred = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1

                val phones = getPhones(resolver, contactId)
                val emails = getEmails(resolver, contactId)
                val addresses = getAddresses(resolver, contactId)
                val (org, title) = getOrganization(resolver, contactId)
                val note = getNote(resolver, contactId)

                contacts.add(
                    Contact(
                        displayName = displayName,
                        phones = phones,
                        emails = emails,
                        addresses = addresses,
                        organization = org,
                        title = title,
                        note = note,
                        starred = starred
                    )
                )
            }
        }

        val data = ContactsBackupData(contacts)
        outputFile.writeText(json.encodeToString(data))
        Timber.d("$TAG Backed up ${contacts.size} contacts")
        contacts.size
    }

    /**
     * Export contacts as VCF (vCard) format for universal compatibility.
     * Uses the system's vCard lookup URI.
     */
    suspend fun exportVcf(outputFile: File): Result<Int> = runCatching {
        grantContactsPermissions()
        val resolver = context.contentResolver
        val sb = StringBuilder()
        var count = 0

        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                val vcardUri = ContactsContract.Contacts.CONTENT_VCARD_URI.buildUpon()
                    .appendPath(lookupKey).build()
                try {
                    resolver.openAssetFileDescriptor(vcardUri, "r")?.use { afd ->
                        afd.createInputStream().use { input ->
                            sb.append(input.bufferedReader().readText())
                            sb.append("\n")
                            count++
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "$TAG Failed to export vCard for $lookupKey")
                }
            }
        }

        outputFile.writeText(sb.toString())
        Timber.d("$TAG Exported $count contacts as VCF")
        count
    }

    /**
     * Restore contacts from JSON backup.
     * Uses root-granted WRITE_CONTACTS to insert via ContentProvider batch ops.
     */
    suspend fun restore(inputFile: File, skipDuplicates: Boolean = true): Result<Int> = runCatching {
        grantContactsPermissions()
        val data = json.decodeFromString<ContactsBackupData>(inputFile.readText())
        val resolver = context.contentResolver
        var restored = 0

        for (contact in data.contacts) {
            if (skipDuplicates && contactExists(resolver, contact.displayName)) continue

            val ops = ArrayList<android.content.ContentProviderOperation>()
            val rawIndex = ops.size

            // Insert raw contact
            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .withValue(ContactsContract.RawContacts.STARRED, if (contact.starred) 1 else 0)
                    .build()
            )

            // Display name
            if (contact.displayName != null) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.displayName)
                        .build()
                )
            }

            // Phones
            for (phone in contact.phones) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                        .build()
                )
            }

            // Emails
            for (email in contact.emails) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.address)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                        .build()
                )
            }

            // Organization
            if (contact.organization != null) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contact.organization)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, contact.title)
                        .build()
                )
            }

            // Note
            if (contact.note != null) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.note)
                        .build()
                )
            }

            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, ops)
                restored++
            } catch (e: Exception) {
                Timber.w(e, "$TAG Failed to restore contact: ${contact.displayName}")
            }
        }

        Timber.d("$TAG Restored $restored/${data.contacts.size} contacts")
        restored
    }

    private fun getPhones(resolver: ContentResolver, contactId: String): List<Phone> {
        val phones = mutableListOf<Phone>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
            arrayOf(contactId), null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                phones.add(
                    Phone(
                        number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)),
                        type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                    )
                )
            }
        }
        return phones
    }

    private fun getEmails(resolver: ContentResolver, contactId: String): List<Email> {
        val emails = mutableListOf<Email>()
        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
            arrayOf(contactId), null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                emails.add(
                    Email(
                        address = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)),
                        type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE))
                    )
                )
            }
        }
        return emails
    }

    private fun getAddresses(resolver: ContentResolver, contactId: String): List<Address> {
        val addresses = mutableListOf<Address>()
        resolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, null,
            "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID}=?",
            arrayOf(contactId), null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                addresses.add(
                    Address(
                        formatted = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)) ?: "",
                        type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))
                    )
                )
            }
        }
        return addresses
    }

    private fun getOrganization(resolver: ContentResolver, contactId: String): Pair<String?, String?> {
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE
            ),
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return Pair(cursor.getString(0), cursor.getString(1))
            }
        }
        return Pair(null, null)
    }

    private fun getNote(resolver: ContentResolver, contactId: String): String? {
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Note.NOTE),
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun contactExists(resolver: ContentResolver, displayName: String?): Boolean {
        if (displayName == null) return false
        return resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME}=?",
            arrayOf(displayName),
            null
        )?.use { it.count > 0 } ?: false
    }

    private suspend fun grantContactsPermissions() {
        shellExecutor.executeRoot("pm grant ${context.packageName} android.permission.READ_CONTACTS")
        shellExecutor.executeRoot("pm grant ${context.packageName} android.permission.WRITE_CONTACTS")
    }
}
