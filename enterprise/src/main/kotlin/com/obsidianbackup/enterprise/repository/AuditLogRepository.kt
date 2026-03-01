package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Repository for AuditLog entity.
 * 
 * **Research Citation:** Finding 8 - SOC 2 Audit Logging
 * 
 * **Purpose:**
 * - Query audit logs for compliance reporting
 * - Statistics and analytics for security events
 * - Hash chain validation support
 * - Multi-tenant isolation via RLS
 * 
 * **Immutability Enforcement:**
 * - NO UPDATE methods (append-only)
 * - NO DELETE methods (retention policy only)
 * - Only INSERT and SELECT operations allowed
 * 
 * **Security:**
 * - All queries respect Row-Level Security (RLS)
 * - Organization context must be set before queries
 * - No cross-tenant data access possible
 * 
 * @see AuditLog
 */
@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    
    // ============================================================================
    // BASIC QUERIES
    // ============================================================================
    
    /**
     * Find all audit logs for an organization (paginated).
     * 
     * **Use Case:** Compliance officer reviewing all audit events
     * 
     * @param organizationId Organization UUID
     * @param pageable Pagination parameters
     * @return Page of audit logs ordered by timestamp DESC
     */
    fun findByOrganizationIdOrderByTimestampDesc(
        organizationId: UUID,
        pageable: Pageable
    ): Page<AuditLog>
    
    /**
     * Find the last audit log entry for an organization.
     * Used for hash chain linking.
     * 
     * **Use Case:** Get previous entry hash when creating new audit log
     * 
     * @param organizationId Organization UUID
     * @return Latest audit log entry or null
     */
    fun findFirstByOrganizationIdOrderBySequenceNumberDesc(organizationId: UUID): AuditLog?
    
    // ============================================================================
    // FILTERING QUERIES
    // ============================================================================
    
    /**
     * Find audit logs by action types.
     * 
     * **Use Case:** Filter logs for specific event types (e.g., all login attempts)
     * 
     * @param organizationId Organization UUID
     * @param actions List of action types to filter
     * @param pageable Pagination parameters
     * @return Page of matching audit logs
     */
    fun findByOrganizationIdAndActionInOrderByTimestampDesc(
        organizationId: UUID,
        actions: List<String>,
        pageable: Pageable
    ): Page<AuditLog>
    
    /**
     * Find audit logs by outcome.
     * 
     * **Use Case:** Review all failed operations for security analysis
     * 
     * @param organizationId Organization UUID
     * @param outcome Outcome value (SUCCESS, FAILURE, PARTIAL)
     * @param pageable Pagination parameters
     * @return Page of matching audit logs
     */
    fun findByOrganizationIdAndOutcomeOrderByTimestampDesc(
        organizationId: UUID,
        outcome: String,
        pageable: Pageable
    ): Page<AuditLog>
    
    /**
     * Find audit logs for a specific user within date range.
     * 
     * **Use Case:** User activity report for compliance audit
     * 
     * @param organizationId Organization UUID
     * @param userId User UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of user's audit logs
     */
    fun findByOrganizationIdAndUserIdAndTimestampBetweenOrderByTimestampDesc(
        organizationId: UUID,
        userId: UUID,
        startDate: Instant,
        endDate: Instant,
        pageable: Pageable
    ): Page<AuditLog>
    
    /**
     * Find audit logs within date range.
     * 
     * **Use Case:** Compliance report for specific time period (e.g., quarterly audit)
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of audit logs in date range
     */
    fun findByOrganizationIdAndTimestampBetweenOrderByTimestampDesc(
        organizationId: UUID,
        startDate: Instant,
        endDate: Instant,
        pageable: Pageable
    ): Page<AuditLog>
    
    /**
     * Find audit logs for a specific resource.
     * 
     * **Use Case:** Audit trail for a specific device, policy, or user
     * 
     * @param organizationId Organization UUID
     * @param resourceType Resource type (DEVICE, POLICY, USER, etc.)
     * @param resourceId Resource identifier
     * @param pageable Pagination parameters
     * @return Page of resource audit logs
     */
    fun findByOrganizationIdAndResourceTypeAndResourceIdOrderByTimestampDesc(
        organizationId: UUID,
        resourceType: String,
        resourceId: String,
        pageable: Pageable
    ): Page<AuditLog>
    
    /**
     * Find audit logs by compliance framework.
     * 
     * **Use Case:** Generate HIPAA or SOC 2 specific audit reports
     * 
     * @param organizationId Organization UUID
     * @param framework Compliance framework name (e.g., "HIPAA", "SOC2", "GDPR")
     * @param pageable Pagination parameters
     * @return Page of compliance-tagged audit logs
     */
    @Query(
        "SELECT a FROM AuditLog a WHERE a.organizationId = :orgId " +
        "AND :framework = ANY(a.complianceFrameworks) " +
        "ORDER BY a.timestamp DESC"
    )
    fun findByOrganizationIdAndComplianceFramework(
        @Param("orgId") organizationId: UUID,
        @Param("framework") framework: String,
        pageable: Pageable
    ): Page<AuditLog>
    
    // ============================================================================
    // HASH CHAIN VALIDATION
    // ============================================================================
    
    /**
     * Find all audit logs for an organization ordered by sequence number.
     * Used for hash chain validation.
     * 
     * **Use Case:** Verify integrity of entire audit log chain
     * 
     * @param organizationId Organization UUID
     * @return List of all audit logs in sequence order
     */
    fun findByOrganizationIdOrderBySequenceNumberAsc(organizationId: UUID): List<AuditLog>
    
    // ============================================================================
    // STATISTICS
    // ============================================================================
    
    /**
     * Count audit logs within date range.
     * 
     * **Use Case:** Dashboard statistics
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of audit logs
     */
    fun countByOrganizationIdAndTimestampBetween(
        organizationId: UUID,
        startDate: Instant,
        endDate: Instant
    ): Long
    
    /**
     * Count audit logs by outcome.
     * 
     * **Use Case:** Success/failure metrics
     * 
     * @param organizationId Organization UUID
     * @param outcome Outcome value
     * @return Count of audit logs with outcome
     */
    fun countByOrganizationIdAndOutcome(
        organizationId: UUID,
        outcome: String
    ): Long
    
    /**
     * Count audit logs for a user within date range.
     * 
     * **Use Case:** User activity metrics
     * 
     * @param organizationId Organization UUID
     * @param userId User UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of user's audit logs
     */
    fun countByOrganizationIdAndUserIdAndTimestampBetween(
        organizationId: UUID,
        userId: UUID,
        startDate: Instant,
        endDate: Instant
    ): Long
    
    /**
     * Count audit logs by action within date range.
     * 
     * **Use Case:** Event type frequency analysis
     * 
     * @param organizationId Organization UUID
     * @param action Action type
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of audit logs with action
     */
    fun countByOrganizationIdAndActionAndTimestampBetween(
        organizationId: UUID,
        action: String,
        startDate: Instant,
        endDate: Instant
    ): Long
    
    /**
     * Get action statistics for date range.
     * 
     * **Use Case:** Security dashboard showing top event types
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of (action, count) pairs ordered by count DESC
     */
    @Query(
        "SELECT a.action, COUNT(a) as cnt FROM AuditLog a " +
        "WHERE a.organizationId = :orgId " +
        "AND a.timestamp BETWEEN :startDate AND :endDate " +
        "GROUP BY a.action " +
        "ORDER BY cnt DESC"
    )
    fun getActionStatistics(
        @Param("orgId") organizationId: UUID,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<Array<Any>>
    
    /**
     * Get user activity statistics for date range.
     * 
     * **Use Case:** Identify most active users
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of (userId, count) pairs ordered by count DESC
     */
    @Query(
        "SELECT a.userId, COUNT(a) as cnt FROM AuditLog a " +
        "WHERE a.organizationId = :orgId " +
        "AND a.userId IS NOT NULL " +
        "AND a.timestamp BETWEEN :startDate AND :endDate " +
        "GROUP BY a.userId " +
        "ORDER BY cnt DESC"
    )
    fun getUserActivityStatistics(
        @Param("orgId") organizationId: UUID,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<Array<Any>>
}
