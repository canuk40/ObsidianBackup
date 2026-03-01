// security/audit/AuditLogEntity.kt
package com.obsidianbackup.security.audit

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * HIPAA-Compliant Audit Log Entity
 * 
 * Immutable audit record for all PHI access.
 * 
 * HIPAA Requirements (45 CFR § 164.312(b)):
 * - Who: userId
 * - What: action
 * - When: timestamp
 * - Where: deviceId
 * - Which: filePath, dataType
 * - Outcome: outcome (SUCCESS/FAILURE)
 * 
 * Security Features:
 * - Immutable (no updates/deletes allowed)
 * - Encrypted database (SQLCipher)
 * - Append-only operations
 * - 6-year retention requirement
 */
@Entity(tableName = "audit_logs")
@TypeConverters(AuditLogConverters::class)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // HIPAA Required: Who accessed the data
    val userId: String,
    
    // HIPAA Required: What action was performed
    val action: String,  // EXPORT, IMPORT, VIEW, DELETE, ENCRYPT, DECRYPT
    
    // HIPAA Required: What type of data
    val dataType: String,  // StepsRecord, HeartRateRecord, etc.
    
    // HIPAA Required: When (with timezone)
    val timestamp: Long = System.currentTimeMillis(),
    
    // HIPAA Required: Where (device identifier)
    val deviceId: String,
    
    // HIPAA Required: Outcome (SUCCESS or FAILURE)
    val outcome: String,  // SUCCESS, FAILURE
    
    // Additional Context
    val recordCount: Int? = null,  // How many records affected
    val filePath: String? = null,  // File path for exports/imports
    val fileSize: Long? = null,  // File size in bytes
    val errorMessage: String? = null,  // Error details if outcome = FAILURE
    
    // Security/Compliance
    val encryptionAlgorithm: String? = null,  // AES-256-GCM, etc.
    val complianceFlags: Set<String> = emptySet(),  // HIPAA, NIST_FIPS_140_2, etc.
    
    // Session tracking
    val sessionId: String? = null,
    val ipAddress: String? = null,  // For network operations
    
    // Tamper detection
    val sequenceNumber: Long = 0,  // Monotonically increasing
    val previousHash: String? = null  // Hash of previous log entry (future: blockchain-style)
)

/**
 * Type converters for Room to handle Set<String>
 */
class AuditLogConverters {
    @androidx.room.TypeConverter
    fun fromStringSet(value: Set<String>?): String {
        return value?.joinToString(",") ?: ""
    }
    
    @androidx.room.TypeConverter
    fun toStringSet(value: String): Set<String> {
        return if (value.isBlank()) emptySet() else value.split(",").toSet()
    }
}

/**
 * Audit action constants
 */
object AuditAction {
    const val EXPORT = "EXPORT"
    const val IMPORT = "IMPORT"
    const val VIEW = "VIEW"
    const val DELETE = "DELETE"
    const val ENCRYPT = "ENCRYPT"
    const val DECRYPT = "DECRYPT"
    const val BACKUP = "BACKUP"
    const val RESTORE = "RESTORE"
    const val AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS"
    const val AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE"
    const val AUTHORIZATION_DENIED = "AUTHORIZATION_DENIED"
    const val SECURITY_VIOLATION = "SECURITY_VIOLATION"
}

/**
 * Audit outcome constants
 */
object AuditOutcome {
    const val SUCCESS = "SUCCESS"
    const val FAILURE = "FAILURE"
}
