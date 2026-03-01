package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

/**
 * DeviceCommand entity for remote device management.
 * 
 * **Research Citations:**
 * - Finding 2: MDM REST API Architecture
 * - Finding 3: FCM High-Priority Push for remote commands
 * 
 * **Table:** device_commands
 * **Schema:** V1__initial_schema.sql (lines 196-212)
 * 
 * **Purpose:**
 * - Remote device lock/unlock
 * - Remote device wipe
 * - Policy synchronization
 * - App updates
 * - Compliance remediation
 * 
 * **Command Lifecycle:**
 * 1. PENDING: Command created, awaiting FCM delivery
 * 2. QUEUED: Command queued for delivery
 * 3. DELIVERED: FCM notification delivered to device
 * 4. COMPLETED: Device executed command successfully
 * 5. FAILED: Command execution failed
 * 6. CANCELLED: Command cancelled by admin
 * 
 * **Command Types:**
 * - LOCK: Disable device functionality
 * - UNLOCK: Restore device functionality
 * - WIPE: Erase all device data
 * - SYNC_POLICY: Synchronize policy settings
 * - UPDATE_APP: Trigger app update
 * - REBOOT: Restart device
 * - CUSTOM: Organization-specific command
 * 
 * **Retry Logic:**
 * - Failed commands are retried up to maxRetries times
 * - Exponential backoff between retries
 * - Retry count tracked for monitoring
 * 
 * **Relationships:**
 * - ManyToOne: Organization (tenant)
 * - ManyToOne: Device (target device)
 * - ManyToOne: User (issuedBy)
 */
@Entity
@Table(
    name = "device_commands",
    indexes = [
        Index(name = "idx_commands_org", columnList = "organization_id"),
        Index(name = "idx_commands_device", columnList = "device_id"),
        Index(name = "idx_commands_status", columnList = "status"),
        Index(name = "idx_commands_issued", columnList = "issued_at")
    ]
)
data class DeviceCommand(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    
    @Column(name = "device_id", nullable = false)
    val deviceId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 50)
    val commandType: CommandType,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "command_payload", columnDefinition = "jsonb")
    val commandPayload: Map<String, Any>? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: CommandStatus = CommandStatus.PENDING,
    
    @Column(name = "issued_by")
    val issuedBy: UUID? = null,
    
    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant = Instant.now(),
    
    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,
    
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,
    
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    
    @Column(name = "max_retries", nullable = false)
    val maxRetries: Int = 3
) {
    /**
     * Command type enum.
     * Matches database constraint: CHECK (command_type IN ('LOCK', 'UNLOCK', 'WIPE', 'SYNC_POLICY', 'UPDATE_APP', 'REBOOT', 'CUSTOM'))
     */
    enum class CommandType {
        /**
         * Lock: Disable device functionality (screen lock, app access disabled)
         */
        LOCK,
        
        /**
         * Unlock: Restore device functionality
         */
        UNLOCK,
        
        /**
         * Wipe: Erase all device data (factory reset)
         */
        WIPE,
        
        /**
         * Sync Policy: Synchronize policy settings from server
         */
        SYNC_POLICY,
        
        /**
         * Update App: Trigger app update from Play Store
         */
        UPDATE_APP,
        
        /**
         * Reboot: Restart device
         */
        REBOOT,
        
        /**
         * Custom: Organization-specific command
         */
        CUSTOM
    }
    
    /**
     * Command status enum.
     * Matches database constraint: CHECK (status IN ('PENDING', 'QUEUED', 'DELIVERED', 'COMPLETED', 'FAILED', 'CANCELLED'))
     */
    enum class CommandStatus {
        /**
         * Pending: Command created, awaiting delivery
         */
        PENDING,
        
        /**
         * Queued: Command queued for FCM delivery
         */
        QUEUED,
        
        /**
         * Delivered: FCM notification delivered to device
         */
        DELIVERED,
        
        /**
         * Completed: Device executed command successfully
         */
        COMPLETED,
        
        /**
         * Failed: Command execution failed
         */
        FAILED,
        
        /**
         * Cancelled: Command cancelled by admin
         */
        CANCELLED
    }
    
    /**
     * Mark command as queued.
     */
    fun markAsQueued() {
        status = CommandStatus.QUEUED
    }
    
    /**
     * Mark command as delivered.
     * 
     * **Research Citation:** Finding 3 - FCM delivery confirmation
     */
    fun markAsDelivered() {
        status = CommandStatus.DELIVERED
        deliveredAt = Instant.now()
    }
    
    /**
     * Mark command as completed.
     */
    fun markAsCompleted() {
        status = CommandStatus.COMPLETED
        completedAt = Instant.now()
    }
    
    /**
     * Mark command as failed with error message.
     * 
     * @param error Error message
     */
    fun markAsFailed(error: String) {
        status = CommandStatus.FAILED
        errorMessage = error
        completedAt = Instant.now()
    }
    
    /**
     * Mark command as cancelled.
     */
    fun markAsCancelled() {
        status = CommandStatus.CANCELLED
        completedAt = Instant.now()
    }
    
    /**
     * Increment retry count.
     * 
     * @return True if retries remaining
     */
    fun incrementRetryCount(): Boolean {
        retryCount++
        return retryCount < maxRetries
    }
    
    /**
     * Check if command can be retried.
     */
    fun canRetry(): Boolean {
        return status == CommandStatus.FAILED && retryCount < maxRetries
    }
    
    /**
     * Check if command is pending execution.
     */
    fun isPending(): Boolean {
        return status == CommandStatus.PENDING || status == CommandStatus.QUEUED
    }
    
    /**
     * Check if command is final state (completed, failed, cancelled).
     */
    fun isFinalState(): Boolean {
        return status in listOf(CommandStatus.COMPLETED, CommandStatus.FAILED, CommandStatus.CANCELLED)
    }
    
    /**
     * Get command execution duration (issued to completed).
     * 
     * @return Duration in milliseconds or null if not completed
     */
    fun getExecutionDuration(): Long? {
        val completed = completedAt ?: return null
        return completed.toEpochMilli() - issuedAt.toEpochMilli()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceCommand) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "DeviceCommand(id=$id, type=$commandType, status=$status, retryCount=$retryCount/$maxRetries)"
    }
}
