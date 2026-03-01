package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.DeviceCommand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Repository for DeviceCommand entity.
 * 
 * **Research Citations:**
 * - Finding 2: MDM REST API Architecture
 * - Finding 3: FCM High-Priority Push command delivery
 * 
 * **Purpose:**
 * - Command queuing and retrieval
 * - Status tracking
 * - Retry management
 * - Command history
 * - Statistics and monitoring
 */
@Repository
interface DeviceCommandRepository : JpaRepository<DeviceCommand, UUID> {
    
    /**
     * Find commands for device.
     * 
     * **Use Case:** Device command history
     * 
     * @param deviceId Device UUID
     * @return List of commands ordered by issued date DESC
     */
    fun findByDeviceIdOrderByIssuedAtDesc(deviceId: UUID): List<DeviceCommand>
    
    /**
     * Find commands for organization.
     * 
     * **Use Case:** Admin command monitoring
     * 
     * @param organizationId Organization UUID
     * @return List of commands ordered by issued date DESC
     */
    fun findByOrganizationIdOrderByIssuedAtDesc(organizationId: UUID): List<DeviceCommand>
    
    /**
     * Find commands by status.
     * 
     * **Use Case:** Command queue processing
     * 
     * @param organizationId Organization UUID
     * @param status Command status
     * @return List of commands
     */
    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: DeviceCommand.CommandStatus
    ): List<DeviceCommand>
    
    /**
     * Find commands by device and status.
     * 
     * **Use Case:** Device-specific command queue
     * 
     * @param deviceId Device UUID
     * @param status Command status
     * @return List of commands
     */
    fun findByDeviceIdAndStatus(
        deviceId: UUID,
        status: DeviceCommand.CommandStatus
    ): List<DeviceCommand>
    
    /**
     * Find pending commands (PENDING or QUEUED).
     * 
     * **Research Citation:** Finding 3 - FCM command delivery queue
     * 
     * **Use Case:** FCM delivery queue processing
     * 
     * @param organizationId Organization UUID
     * @return List of pending commands ordered by issued date
     */
    @Query(
        "SELECT c FROM DeviceCommand c " +
        "WHERE c.organizationId = :orgId " +
        "AND c.status IN ('PENDING', 'QUEUED') " +
        "ORDER BY c.issuedAt ASC"
    )
    fun findPendingCommands(@Param("orgId") organizationId: UUID): List<DeviceCommand>
    
    /**
     * Find pending commands for device.
     * 
     * **Use Case:** Device-specific command delivery
     * 
     * @param deviceId Device UUID
     * @return List of pending commands
     */
    @Query(
        "SELECT c FROM DeviceCommand c " +
        "WHERE c.deviceId = :deviceId " +
        "AND c.status IN ('PENDING', 'QUEUED') " +
        "ORDER BY c.issuedAt ASC"
    )
    fun findPendingCommandsForDevice(@Param("deviceId") deviceId: UUID): List<DeviceCommand>
    
    /**
     * Find failed commands eligible for retry.
     * 
     * **Use Case:** Retry logic for failed commands
     * 
     * @param organizationId Organization UUID
     * @return List of failed commands with retries remaining
     */
    @Query(
        "SELECT c FROM DeviceCommand c " +
        "WHERE c.organizationId = :orgId " +
        "AND c.status = 'FAILED' " +
        "AND c.retryCount < c.maxRetries " +
        "ORDER BY c.issuedAt DESC"
    )
    fun findFailedCommandsForRetry(@Param("orgId") organizationId: UUID): List<DeviceCommand>
    
    /**
     * Find commands by type.
     * 
     * **Use Case:** Command type analytics
     * 
     * @param organizationId Organization UUID
     * @param commandType Command type
     * @return List of commands
     */
    fun findByOrganizationIdAndCommandType(
        organizationId: UUID,
        commandType: DeviceCommand.CommandType
    ): List<DeviceCommand>
    
    /**
     * Find commands issued by user.
     * 
     * **Use Case:** Admin action tracking
     * 
     * @param organizationId Organization UUID
     * @param issuedBy User UUID
     * @return List of commands
     */
    fun findByOrganizationIdAndIssuedByOrderByIssuedAtDesc(
        organizationId: UUID,
        issuedBy: UUID
    ): List<DeviceCommand>
    
    /**
     * Find commands in date range.
     * 
     * **Use Case:** Historical command analysis
     * 
     * @param organizationId Organization UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of commands
     */
    fun findByOrganizationIdAndIssuedAtBetweenOrderByIssuedAtDesc(
        organizationId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<DeviceCommand>
    
    /**
     * Count commands by status.
     * 
     * **Use Case:** Command queue metrics
     * 
     * @param organizationId Organization UUID
     * @param status Command status
     * @return Command count
     */
    fun countByOrganizationIdAndStatus(
        organizationId: UUID,
        status: DeviceCommand.CommandStatus
    ): Long
    
    /**
     * Count commands by type.
     * 
     * **Use Case:** Command type distribution
     * 
     * @param organizationId Organization UUID
     * @param commandType Command type
     * @return Command count
     */
    fun countByOrganizationIdAndCommandType(
        organizationId: UUID,
        commandType: DeviceCommand.CommandType
    ): Long
    
    /**
     * Count commands for device.
     * 
     * **Use Case:** Device command history metrics
     * 
     * @param deviceId Device UUID
     * @return Command count
     */
    fun countByDeviceId(deviceId: UUID): Long
    
    /**
     * Get command status statistics.
     * 
     * **Use Case:** Admin dashboard showing command queue status
     * 
     * @param organizationId Organization UUID
     * @return List of (status, count) pairs
     */
    @Query(
        "SELECT c.status, COUNT(c) FROM DeviceCommand c " +
        "WHERE c.organizationId = :orgId " +
        "GROUP BY c.status " +
        "ORDER BY COUNT(c) DESC"
    )
    fun getCommandStatusStatistics(@Param("orgId") organizationId: UUID): List<Array<Any>>
    
    /**
     * Get command type statistics.
     * 
     * **Use Case:** Command type distribution analysis
     * 
     * @param organizationId Organization UUID
     * @return List of (commandType, count) pairs
     */
    @Query(
        "SELECT c.commandType, COUNT(c) FROM DeviceCommand c " +
        "WHERE c.organizationId = :orgId " +
        "GROUP BY c.commandType " +
        "ORDER BY COUNT(c) DESC"
    )
    fun getCommandTypeStatistics(@Param("orgId") organizationId: UUID): List<Array<Any>>
    
    /**
     * Get average command execution duration.
     * 
     * **Use Case:** Performance monitoring
     * 
     * @param organizationId Organization UUID
     * @param commandType Command type (optional filter)
     * @return Average duration in milliseconds
     */
    @Query(
        "SELECT AVG(EXTRACT(EPOCH FROM (c.completedAt - c.issuedAt)) * 1000) " +
        "FROM DeviceCommand c " +
        "WHERE c.organizationId = :orgId " +
        "AND c.status = 'COMPLETED' " +
        "AND (:commandType IS NULL OR c.commandType = :commandType)"
    )
    fun getAverageExecutionDuration(
        @Param("orgId") organizationId: UUID,
        @Param("commandType") commandType: DeviceCommand.CommandType? = null
    ): Double?
    
    /**
     * Find stale commands (pending for too long).
     * 
     * **Use Case:** Timeout detection, cleanup
     * 
     * @param organizationId Organization UUID
     * @param staleThreshold Timestamp threshold
     * @return List of stale commands
     */
    @Query(
        "SELECT c FROM DeviceCommand c " +
        "WHERE c.organizationId = :orgId " +
        "AND c.status IN ('PENDING', 'QUEUED', 'DELIVERED') " +
        "AND c.issuedAt < :staleThreshold"
    )
    fun findStaleCommands(
        @Param("orgId") organizationId: UUID,
        @Param("staleThreshold") staleThreshold: Instant
    ): List<DeviceCommand>
}
