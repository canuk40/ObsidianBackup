package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

/**
 * AuditLog entity for SOC 2 compliant audit logging.
 * 
 * **Research Citation:** Finding 8 - SOC 2 Audit Logging Requirements
 * - 12-24 month retention (730 days minimum)
 * - Immutable logs (append-only via database rules)
 * - Hash chaining for tamper detection (SHA-256)
 * - Required fields: who, what, when, where, outcome
 * 
 * **Table:** audit_logs
 * **Schema:** V1__initial_schema.sql (lines 225-258)
 * 
 * **Purpose:**
 * - Tamper-evident audit trail for all security-sensitive operations
 * - SOC 2, HIPAA, GDPR compliance support
 * - Hash-chained entries for integrity verification
 * - Multi-tenant isolation via Row-Level Security (RLS)
 * - Append-only design (no updates or deletes allowed)
 * 
 * **Hash Chain:**
 * Each entry contains:
 * - `previousHash`: SHA-256 hash of previous entry (null for first entry)
 * - `entryHash`: SHA-256 hash of current entry
 * - `sequenceNumber`: Auto-incrementing sequence per organization
 * 
 * **Immutability:**
 * - Database rules prevent UPDATE and DELETE operations
 * - Hash chain detects tampering or deleted entries
 * - All fields are immutable (no setters)
 * 
 * **Relationships:**
 * - ManyToOne: Organization (multi-tenant)
 * - ManyToOne: User (optional - null for system events)
 * 
 * @see <a href="https://www.soc2.com/audit-logging-requirements">SOC 2 Audit Logging</a>
 * @see <a href="https://www.postgresql.org/docs/current/ddl-rowsecurity.html">PostgreSQL RLS</a>
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = [
        Index(name = "idx_audit_logs_org", columnList = "organization_id"),
        Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
        Index(name = "idx_audit_logs_user", columnList = "user_id"),
        Index(name = "idx_audit_logs_action", columnList = "action"),
        Index(name = "idx_audit_logs_resource", columnList = "resource_type,resource_id"),
        Index(name = "idx_audit_logs_outcome", columnList = "outcome"),
        Index(name = "idx_audit_logs_sequence", columnList = "sequence_number")
    ]
)
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    
    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Long,
    
    @Column(name = "timestamp", nullable = false)
    val timestamp: Instant = Instant.now(),
    
    // SOC 2 Required Fields: WHO
    @Column(name = "user_id")
    val userId: UUID? = null,
    
    // SOC 2 Required Fields: WHAT
    @Column(name = "action", nullable = false, length = 100)
    val action: String,
    
    @Column(name = "resource_type", nullable = false, length = 50)
    val resourceType: String,
    
    @Column(name = "resource_id", length = 255)
    val resourceId: String? = null,
    
    // SOC 2 Required Fields: OUTCOME
    @Column(name = "outcome", nullable = false, length = 20)
    val outcome: String,
    
    // SOC 2 Required Fields: WHERE (context)
    @Column(name = "ip_address")
    val ipAddress: String? = null,
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,
    
    @Column(name = "session_id", length = 255)
    val sessionId: String? = null,
    
    // Details (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    val details: Map<String, Any>? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    val previousState: Map<String, Any>? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", columnDefinition = "jsonb")
    val newState: Map<String, Any>? = null,
    
    // Compliance
    @Column(name = "compliance_frameworks", columnDefinition = "varchar(100)[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    val complianceFrameworks: Array<String>? = null,
    
    @Column(name = "data_classification", length = 50)
    val dataClassification: String? = null,
    
    // Hash Chain (Tamper Detection)
    @Column(name = "previous_hash", length = 64)
    val previousHash: String? = null,
    
    @Column(name = "entry_hash", nullable = false, length = 64)
    val entryHash: String
) {
    /**
     * Outcome enum values.
     * Matches database constraint: CHECK (outcome IN ('SUCCESS', 'FAILURE', 'PARTIAL'))
     */
    object Outcome {
        const val SUCCESS = "SUCCESS"
        const val FAILURE = "FAILURE"
        const val PARTIAL = "PARTIAL"
    }
    
    /**
     * Data classification enum values.
     * Matches database constraint: CHECK (data_classification IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'PHI', 'PII'))
     */
    object DataClassification {
        const val PUBLIC = "PUBLIC"
        const val INTERNAL = "INTERNAL"
        const val CONFIDENTIAL = "CONFIDENTIAL"
        const val PHI = "PHI"
        const val PII = "PII"
    }
    
    /**
     * Common audit actions.
     * Not exhaustive - custom actions are allowed.
     */
    object Action {
        // Authentication
        const val LOGIN_SUCCESS = "LOGIN_SUCCESS"
        const val LOGIN_FAILURE = "LOGIN_FAILURE"
        const val LOGOUT = "LOGOUT"
        const val TOKEN_REFRESH = "TOKEN_REFRESH"
        const val PASSWORD_CHANGE = "PASSWORD_CHANGE"
        const val MFA_ENABLED = "MFA_ENABLED"
        const val MFA_DISABLED = "MFA_DISABLED"
        
        // Authorization
        const val UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS"
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
        const val ROLE_CHANGED = "ROLE_CHANGED"
        
        // Device Management
        const val DEVICE_ENROLLED = "DEVICE_ENROLLED"
        const val DEVICE_UNENROLLED = "DEVICE_UNENROLLED"
        const val DEVICE_LOCKED = "DEVICE_LOCKED"
        const val DEVICE_UNLOCKED = "DEVICE_UNLOCKED"
        const val DEVICE_WIPED = "DEVICE_WIPED"
        const val DEVICE_COMMAND_ISSUED = "DEVICE_COMMAND_ISSUED"
        
        // Policy Management
        const val POLICY_CREATED = "POLICY_CREATED"
        const val POLICY_UPDATED = "POLICY_UPDATED"
        const val POLICY_DELETED = "POLICY_DELETED"
        const val POLICY_ASSIGNED = "POLICY_ASSIGNED"
        const val POLICY_EVALUATED = "POLICY_EVALUATION"
        
        // Security Events
        const val SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY"
        const val DATA_ACCESS = "DATA_ACCESS"
        const val DATA_EXPORT = "DATA_EXPORT"
        const val CONFIGURATION_CHANGE = "CONFIGURATION_CHANGE"
    }
    
    /**
     * Resource types for audit logging.
     */
    object ResourceType {
        const val USER = "USER"
        const val DEVICE = "DEVICE"
        const val POLICY = "POLICY"
        const val ORGANIZATION = "ORGANIZATION"
        const val TOKEN = "TOKEN"
        const val SESSION = "SESSION"
        const val CONFIGURATION = "CONFIGURATION"
        const val DATA = "DATA"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuditLog) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "AuditLog(id=$id, orgId=$organizationId, seq=$sequenceNumber, " +
               "action='$action', resource=$resourceType/$resourceId, outcome='$outcome')"
    }
}
