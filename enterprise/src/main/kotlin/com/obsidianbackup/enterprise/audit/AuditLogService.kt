package com.obsidianbackup.enterprise.audit

import com.obsidianbackup.enterprise.model.AuditLog
import com.obsidianbackup.enterprise.repository.AuditLogRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

/**
 * Audit Log Service for SOC 2 compliant audit logging.
 * 
 * **Research Citation:** Finding 8 - SOC 2 Audit Logging Requirements
 * - 12-24 month retention (730 days minimum)
 * - Immutable logs (append-only via database rules)
 * - Hash chaining for tamper detection (SHA-256)
 * - Required fields: who, what, when, where, outcome
 * - WORM (Write Once Read Many) storage pattern
 * 
 * **Hash Chain Implementation:**
 * Each audit log entry is linked to the previous entry via SHA-256 hash:
 * 1. Retrieve last entry for organization
 * 2. Extract previous entry's hash
 * 3. Generate new entry hash (includes previous hash)
 * 4. Store entry with both hashes
 * 
 * **Security Features:**
 * - Tamper detection via hash chain validation
 * - Multi-tenant isolation via Row-Level Security (RLS)
 * - Immutable entries (database rules prevent updates/deletes)
 * - Sequence numbers detect missing entries
 * - Complete audit trail for all security events
 * 
 * **Compliance Support:**
 * - SOC 2 Trust Service Criteria
 * - HIPAA Security Rule (45 CFR §164.312)
 * - GDPR Article 30 (Records of Processing)
 * - PCI-DSS Requirement 10 (Audit Logging)
 * 
 * @see AuditLog
 * @see AuditLogRepository
 * @see <a href="https://www.aicpa.org/soc2">SOC 2 Compliance</a>
 */
@Service
@Transactional
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    private val logger = LoggerFactory.getLogger(AuditLogService::class.java)
    
    // ============================================================================
    // ROW-LEVEL SECURITY (RLS)
    // ============================================================================
    
    /**
     * Set organization context for Row-Level Security.
     * 
     * **Critical:** Must be called before ANY database operation.
     * PostgreSQL RLS policies use `app.current_organization` to enforce tenant isolation.
     * 
     * @param organizationId Organization UUID
     */
    private fun setOrganizationContext(organizationId: UUID) {
        entityManager.createNativeQuery(
            "SET LOCAL app.current_organization = :orgId"
        ).setParameter("orgId", organizationId.toString())
         .executeUpdate()
    }
    
    // ============================================================================
    // HASH CHAIN GENERATION
    // ============================================================================
    
    /**
     * Generate SHA-256 hash for audit log entry.
     * 
     * **Hash Includes:**
     * - Sequence number (ordering)
     * - User ID (who)
     * - Action (what)
     * - Timestamp (when)
     * - Outcome (result)
     * - Previous hash (chain link)
     * 
     * **Formula:**
     * hash = SHA256(seq|userId|action|timestamp|outcome|previousHash)
     * 
     * @param entry AuditLog entry to hash
     * @return 64-character hex string (SHA-256)
     */
    private fun generateEntryHash(entry: AuditLog): String {
        val data = "${entry.sequenceNumber}|" +
                   "${entry.userId ?: ""}|" +
                   "${entry.action}|" +
                   "${entry.timestamp}|" +
                   "${entry.outcome}|" +
                   "${entry.previousHash ?: ""}"
        
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get next sequence number for organization.
     * 
     * @param organizationId Organization UUID
     * @return Next sequence number (1 for first entry)
     */
    private fun getNextSequenceNumber(organizationId: UUID): Long {
        val lastEntry = auditLogRepository.findFirstByOrganizationIdOrderBySequenceNumberDesc(organizationId)
        return (lastEntry?.sequenceNumber ?: 0) + 1
    }
    
    // ============================================================================
    // CORE LOGGING
    // ============================================================================
    
    /**
     * Save audit log entry with hash chaining.
     * 
     * **Process:**
     * 1. Set RLS context
     * 2. Get last entry for previous hash
     * 3. Generate sequence number
     * 4. Generate entry hash
     * 5. Save to database
     * 
     * @param entry AuditLog entry (without hash/sequence)
     * @return Saved AuditLog with generated hash chain
     */
    private fun saveAuditLog(entry: AuditLog): AuditLog {
        try {
            // Set RLS context
            setOrganizationContext(entry.organizationId)
            
            // Get previous hash
            val lastEntry = auditLogRepository.findFirstByOrganizationIdOrderBySequenceNumberDesc(entry.organizationId)
            val previousHash = lastEntry?.entryHash
            
            // Get next sequence number
            val sequenceNumber = getNextSequenceNumber(entry.organizationId)
            
            // Create entry with sequence and previous hash
            val entryWithSequence = entry.copy(
                sequenceNumber = sequenceNumber,
                previousHash = previousHash
            )
            
            // Generate hash for this entry
            val entryHash = generateEntryHash(entryWithSequence)
            
            // Create final entry with hash
            val finalEntry = entryWithSequence.copy(entryHash = entryHash)
            
            // Save to database
            val saved = auditLogRepository.save(finalEntry)
            
            logger.debug(
                "Audit log saved: org={}, seq={}, action={}, outcome={}",
                entry.organizationId,
                sequenceNumber,
                entry.action,
                entry.outcome
            )
            
            return saved
            
        } catch (e: Exception) {
            logger.error("Failed to save audit log: org={}, action={}", entry.organizationId, entry.action, e)
            throw e
        }
    }
    
    // ============================================================================
    // AUTHENTICATION EVENTS
    // ============================================================================
    
    /**
     * Log successful authentication.
     * 
     * **SOC 2 Requirement:** CC6.1 - Logical and physical access controls
     * 
     * @param userId User UUID
     * @param organizationId Organization UUID
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param sessionId Session identifier
     * @param deviceInfo Device information (optional)
     */
    fun logAuthenticationSuccess(
        userId: UUID,
        organizationId: UUID,
        ipAddress: String?,
        userAgent: String?,
        sessionId: String?,
        deviceInfo: Map<String, Any>? = null
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0, // Will be set by saveAuditLog
                userId = userId,
                action = AuditLog.Action.LOGIN_SUCCESS,
                resourceType = AuditLog.ResourceType.SESSION,
                resourceId = sessionId,
                outcome = AuditLog.Outcome.SUCCESS,
                ipAddress = ipAddress,
                userAgent = userAgent,
                sessionId = sessionId,
                details = deviceInfo,
                complianceFrameworks = arrayOf("SOC2", "HIPAA"),
                dataClassification = AuditLog.DataClassification.CONFIDENTIAL,
                entryHash = "" // Will be set by saveAuditLog
            )
        )
    }
    
    /**
     * Log failed authentication attempt.
     * 
     * **SOC 2 Requirement:** CC6.1 - Failed login tracking for brute force detection
     * 
     * @param email Email attempted (user may not exist)
     * @param reason Failure reason (e.g., "Invalid credentials", "Account locked")
     * @param organizationId Organization UUID
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     */
    fun logAuthenticationFailure(
        email: String,
        reason: String,
        organizationId: UUID,
        ipAddress: String?,
        userAgent: String?
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = null, // User not authenticated
                action = AuditLog.Action.LOGIN_FAILURE,
                resourceType = AuditLog.ResourceType.SESSION,
                resourceId = email,
                outcome = AuditLog.Outcome.FAILURE,
                ipAddress = ipAddress,
                userAgent = userAgent,
                details = mapOf("email" to email, "reason" to reason),
                complianceFrameworks = arrayOf("SOC2", "HIPAA"),
                dataClassification = AuditLog.DataClassification.CONFIDENTIAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log JWT token refresh.
     * 
     * @param userId User UUID
     * @param organizationId Organization UUID
     * @param ipAddress Client IP address
     * @param oldTokenId Old token ID (optional)
     * @param newTokenId New token ID (optional)
     */
    fun logTokenRefresh(
        userId: UUID,
        organizationId: UUID,
        ipAddress: String?,
        oldTokenId: String? = null,
        newTokenId: String? = null
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = userId,
                action = AuditLog.Action.TOKEN_REFRESH,
                resourceType = AuditLog.ResourceType.TOKEN,
                resourceId = newTokenId,
                outcome = AuditLog.Outcome.SUCCESS,
                ipAddress = ipAddress,
                details = mapOf(
                    "oldTokenId" to (oldTokenId ?: ""),
                    "newTokenId" to (newTokenId ?: "")
                ),
                complianceFrameworks = arrayOf("SOC2"),
                dataClassification = AuditLog.DataClassification.INTERNAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log user logout.
     * 
     * @param userId User UUID
     * @param organizationId Organization UUID
     * @param reason Logout reason (e.g., "User initiated", "Session timeout", "Admin forced")
     * @param sessionId Session identifier
     */
    fun logLogout(
        userId: UUID,
        organizationId: UUID,
        reason: String,
        sessionId: String?
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = userId,
                action = AuditLog.Action.LOGOUT,
                resourceType = AuditLog.ResourceType.SESSION,
                resourceId = sessionId,
                outcome = AuditLog.Outcome.SUCCESS,
                details = mapOf("reason" to reason),
                complianceFrameworks = arrayOf("SOC2"),
                dataClassification = AuditLog.DataClassification.INTERNAL,
                entryHash = ""
            )
        )
    }
    
    // ============================================================================
    // DEVICE MANAGEMENT EVENTS
    // ============================================================================
    
    /**
     * Log device enrollment.
     * 
     * @param adminId Admin who enrolled device
     * @param organizationId Organization UUID
     * @param deviceId Device UUID
     * @param deviceInfo Device details (model, OS, etc.)
     */
    fun logDeviceEnrollment(
        adminId: UUID,
        organizationId: UUID,
        deviceId: UUID,
        deviceInfo: Map<String, Any>
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = adminId,
                action = AuditLog.Action.DEVICE_ENROLLED,
                resourceType = AuditLog.ResourceType.DEVICE,
                resourceId = deviceId.toString(),
                outcome = AuditLog.Outcome.SUCCESS,
                details = deviceInfo,
                complianceFrameworks = arrayOf("SOC2", "HIPAA"),
                dataClassification = AuditLog.DataClassification.INTERNAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log device command issuance.
     * 
     * **SOC 2 Requirement:** CC6.2 - System operations monitoring
     * 
     * @param adminId Admin who issued command
     * @param organizationId Organization UUID
     * @param deviceId Device UUID
     * @param command Command type (LOCK, WIPE, SYNC_POLICY, etc.)
     * @param outcome Command outcome
     * @param details Command details
     */
    fun logDeviceCommand(
        adminId: UUID,
        organizationId: UUID,
        deviceId: UUID,
        command: String,
        outcome: String,
        details: Map<String, Any>? = null
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = adminId,
                action = AuditLog.Action.DEVICE_COMMAND_ISSUED,
                resourceType = AuditLog.ResourceType.DEVICE,
                resourceId = deviceId.toString(),
                outcome = outcome,
                details = (details ?: emptyMap()) + mapOf("command" to command),
                complianceFrameworks = arrayOf("SOC2", "HIPAA"),
                dataClassification = AuditLog.DataClassification.CONFIDENTIAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log device wipe (critical security event).
     * 
     * **SOC 2 Requirement:** CC6.6 - Logical and physical access restrictions
     * 
     * @param adminId Admin who initiated wipe
     * @param organizationId Organization UUID
     * @param deviceId Device UUID
     * @param reason Wipe reason (e.g., "Device lost", "Security breach", "Employee terminated")
     * @param ipAddress Admin IP address
     */
    fun logDeviceWipe(
        adminId: UUID,
        organizationId: UUID,
        deviceId: UUID,
        reason: String,
        ipAddress: String?
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = adminId,
                action = AuditLog.Action.DEVICE_WIPED,
                resourceType = AuditLog.ResourceType.DEVICE,
                resourceId = deviceId.toString(),
                outcome = AuditLog.Outcome.SUCCESS,
                ipAddress = ipAddress,
                details = mapOf("reason" to reason),
                complianceFrameworks = arrayOf("SOC2", "HIPAA", "GDPR"),
                dataClassification = AuditLog.DataClassification.CONFIDENTIAL,
                entryHash = ""
            )
        )
    }
    
    // ============================================================================
    // POLICY MANAGEMENT EVENTS
    // ============================================================================
    
    /**
     * Log policy change (create, update, delete).
     * 
     * **SOC 2 Requirement:** CC7.2 - System monitoring for configuration changes
     * 
     * @param adminId Admin who made change
     * @param organizationId Organization UUID
     * @param policyId Policy UUID
     * @param action Action type (CREATED, UPDATED, DELETED)
     * @param previousState Previous policy state (null for create)
     * @param newState New policy state (null for delete)
     */
    fun logPolicyChange(
        adminId: UUID,
        organizationId: UUID,
        policyId: UUID,
        action: String,
        previousState: Map<String, Any>? = null,
        newState: Map<String, Any>? = null
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = adminId,
                action = action,
                resourceType = AuditLog.ResourceType.POLICY,
                resourceId = policyId.toString(),
                outcome = AuditLog.Outcome.SUCCESS,
                previousState = previousState,
                newState = newState,
                complianceFrameworks = arrayOf("SOC2", "HIPAA"),
                dataClassification = AuditLog.DataClassification.INTERNAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log policy evaluation result.
     * 
     * @param deviceId Device being evaluated
     * @param organizationId Organization UUID
     * @param policyId Policy UUID
     * @param result Evaluation result (PASS, FAIL, ERROR)
     * @param violations Policy violations (if any)
     */
    fun logPolicyEvaluation(
        deviceId: UUID,
        organizationId: UUID,
        policyId: UUID,
        result: String,
        violations: Map<String, Any>? = null
    ): AuditLog {
        val outcome = if (result == "PASS") AuditLog.Outcome.SUCCESS else AuditLog.Outcome.FAILURE
        
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = null, // System-generated
                action = AuditLog.Action.POLICY_EVALUATED,
                resourceType = AuditLog.ResourceType.DEVICE,
                resourceId = deviceId.toString(),
                outcome = outcome,
                details = mapOf(
                    "policyId" to policyId.toString(),
                    "result" to result,
                    "violations" to (violations ?: emptyMap<String, Any>())
                ),
                complianceFrameworks = arrayOf("SOC2", "HIPAA"),
                dataClassification = AuditLog.DataClassification.INTERNAL,
                entryHash = ""
            )
        )
    }
    
    // ============================================================================
    // SECURITY EVENTS
    // ============================================================================
    
    /**
     * Log unauthorized access attempt.
     * 
     * **SOC 2 Requirement:** CC6.1 - Detect and prevent unauthorized access
     * 
     * @param userId User attempting access (null if unauthenticated)
     * @param organizationId Organization UUID
     * @param resource Resource being accessed
     * @param action Action attempted
     * @param ipAddress Client IP address
     */
    fun logUnauthorizedAccess(
        userId: UUID?,
        organizationId: UUID,
        resource: String,
        action: String,
        ipAddress: String?
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = userId,
                action = AuditLog.Action.UNAUTHORIZED_ACCESS,
                resourceType = AuditLog.ResourceType.DATA,
                resourceId = resource,
                outcome = AuditLog.Outcome.FAILURE,
                ipAddress = ipAddress,
                details = mapOf("attemptedAction" to action),
                complianceFrameworks = arrayOf("SOC2", "HIPAA", "GDPR"),
                dataClassification = AuditLog.DataClassification.CONFIDENTIAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log permission denied event.
     * 
     * @param userId User UUID
     * @param organizationId Organization UUID
     * @param resource Resource being accessed
     * @param requiredRole Role required for access
     * @param userRole User's actual role
     */
    fun logPermissionDenied(
        userId: UUID,
        organizationId: UUID,
        resource: String,
        requiredRole: String,
        userRole: String
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = userId,
                action = AuditLog.Action.PERMISSION_DENIED,
                resourceType = AuditLog.ResourceType.DATA,
                resourceId = resource,
                outcome = AuditLog.Outcome.FAILURE,
                details = mapOf(
                    "requiredRole" to requiredRole,
                    "userRole" to userRole
                ),
                complianceFrameworks = arrayOf("SOC2"),
                dataClassification = AuditLog.DataClassification.INTERNAL,
                entryHash = ""
            )
        )
    }
    
    /**
     * Log suspicious activity.
     * 
     * **SOC 2 Requirement:** CC7.2 - Detect anomalies and security incidents
     * 
     * @param userId User UUID (null for anonymous)
     * @param organizationId Organization UUID
     * @param activityType Type of suspicious activity
     * @param details Activity details
     * @param ipAddress Client IP address
     */
    fun logSuspiciousActivity(
        userId: UUID?,
        organizationId: UUID,
        activityType: String,
        details: Map<String, Any>,
        ipAddress: String?
    ): AuditLog {
        return saveAuditLog(
            AuditLog(
                organizationId = organizationId,
                sequenceNumber = 0,
                userId = userId,
                action = AuditLog.Action.SUSPICIOUS_ACTIVITY,
                resourceType = AuditLog.ResourceType.DATA,
                resourceId = activityType,
                outcome = AuditLog.Outcome.FAILURE,
                ipAddress = ipAddress,
                details = details,
                complianceFrameworks = arrayOf("SOC2", "HIPAA", "GDPR"),
                dataClassification = AuditLog.DataClassification.CONFIDENTIAL,
                entryHash = ""
            )
        )
    }
    
    // ============================================================================
    // COMPLIANCE REPORTING
    // ============================================================================
    
    /**
     * Get audit logs for compliance reporting.
     * 
     * **Use Case:** Generate SOC 2, HIPAA, or GDPR compliance reports
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param complianceFrameworks Frameworks to filter by (optional)
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    fun getAuditLogsForCompliance(
        organizationId: UUID,
        startDate: Instant,
        endDate: Instant,
        complianceFrameworks: List<String>? = null,
        pageable: Pageable
    ): Page<AuditLog> {
        setOrganizationContext(organizationId)
        
        return if (complianceFrameworks.isNullOrEmpty()) {
            auditLogRepository.findByOrganizationIdAndTimestampBetweenOrderByTimestampDesc(
                organizationId, startDate, endDate, pageable
            )
        } else {
            // Query for any matching framework
            auditLogRepository.findByOrganizationIdAndComplianceFramework(
                organizationId, complianceFrameworks.first(), pageable
            )
        }
    }
    
    /**
     * Validate hash chain integrity.
     * 
     * **SOC 2 Requirement:** CC7.2 - Detect unauthorized changes to audit logs
     * 
     * **Process:**
     * 1. Retrieve all audit logs in sequence order
     * 2. Verify each entry's previous hash matches the actual previous entry
     * 3. Verify each entry's hash is correctly calculated
     * 4. Detect gaps in sequence numbers
     * 
     * @param organizationId Organization UUID
     * @return Validation result with details
     */
    fun validateHashChain(organizationId: UUID): HashChainValidationResult {
        try {
            setOrganizationContext(organizationId)
            
            val logs = auditLogRepository.findByOrganizationIdOrderBySequenceNumberAsc(organizationId)
            
            if (logs.isEmpty()) {
                return HashChainValidationResult(
                    valid = true,
                    message = "No audit logs found",
                    totalEntries = 0
                )
            }
            
            var previousHash: String? = null
            var expectedSequence = 1L
            
            for (log in logs) {
                // Check sequence number continuity
                if (log.sequenceNumber != expectedSequence) {
                    return HashChainValidationResult(
                        valid = false,
                        message = "Sequence number gap detected: expected $expectedSequence, found ${log.sequenceNumber}",
                        totalEntries = logs.size,
                        firstErrorSequence = log.sequenceNumber
                    )
                }
                
                // Verify previous hash matches
                if (log.previousHash != previousHash) {
                    return HashChainValidationResult(
                        valid = false,
                        message = "Hash chain broken at sequence ${log.sequenceNumber}: " +
                                  "expected previousHash=$previousHash, found ${log.previousHash}",
                        totalEntries = logs.size,
                        firstErrorSequence = log.sequenceNumber
                    )
                }
                
                // Verify entry hash is correct
                val calculatedHash = generateEntryHash(log)
                if (log.entryHash != calculatedHash) {
                    return HashChainValidationResult(
                        valid = false,
                        message = "Entry hash mismatch at sequence ${log.sequenceNumber}: " +
                                  "calculated=$calculatedHash, stored=${log.entryHash}",
                        totalEntries = logs.size,
                        firstErrorSequence = log.sequenceNumber
                    )
                }
                
                previousHash = log.entryHash
                expectedSequence++
            }
            
            return HashChainValidationResult(
                valid = true,
                message = "Hash chain valid for ${logs.size} entries",
                totalEntries = logs.size
            )
            
        } catch (e: Exception) {
            logger.error("Hash chain validation failed for org=$organizationId", e)
            return HashChainValidationResult(
                valid = false,
                message = "Validation error: ${e.message}",
                totalEntries = 0
            )
        }
    }
    
    /**
     * Get security event statistics.
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Security event statistics
     */
    fun getSecurityEventStatistics(
        organizationId: UUID,
        startDate: Instant,
        endDate: Instant
    ): SecurityEventStatistics {
        setOrganizationContext(organizationId)
        
        val totalEvents = auditLogRepository.countByOrganizationIdAndTimestampBetween(
            organizationId, startDate, endDate
        )
        
        val successfulEvents = auditLogRepository.countByOrganizationIdAndOutcome(
            organizationId, AuditLog.Outcome.SUCCESS
        )
        
        val failedEvents = auditLogRepository.countByOrganizationIdAndOutcome(
            organizationId, AuditLog.Outcome.FAILURE
        )
        
        val actionStats = auditLogRepository.getActionStatistics(organizationId, startDate, endDate)
            .associate { (it[0] as String) to (it[1] as Long) }
        
        return SecurityEventStatistics(
            totalEvents = totalEvents,
            successfulEvents = successfulEvents,
            failedEvents = failedEvents,
            actionBreakdown = actionStats
        )
    }
    
    /**
     * Get user activity summary.
     * 
     * @param userId User UUID
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return User activity summary
     */
    fun getUserActivitySummary(
        userId: UUID,
        organizationId: UUID,
        startDate: Instant,
        endDate: Instant
    ): UserActivitySummary {
        setOrganizationContext(organizationId)
        
        val totalActions = auditLogRepository.countByOrganizationIdAndUserIdAndTimestampBetween(
            organizationId, userId, startDate, endDate
        )
        
        return UserActivitySummary(
            userId = userId,
            totalActions = totalActions,
            dateRange = startDate to endDate
        )
    }
}

/**
 * Hash chain validation result.
 */
data class HashChainValidationResult(
    val valid: Boolean,
    val message: String,
    val totalEntries: Int,
    val firstErrorSequence: Long? = null
)

/**
 * Security event statistics.
 */
data class SecurityEventStatistics(
    val totalEvents: Long,
    val successfulEvents: Long,
    val failedEvents: Long,
    val actionBreakdown: Map<String, Long>
)

/**
 * User activity summary.
 */
data class UserActivitySummary(
    val userId: UUID,
    val totalActions: Long,
    val dateRange: Pair<Instant, Instant>
)
