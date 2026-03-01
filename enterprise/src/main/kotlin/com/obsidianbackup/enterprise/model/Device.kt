package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * Device entity for Mobile Device Management (MDM).
 * 
 * **Research Citation:** Finding 2 - MDM REST API Architecture
 * 
 * **Table:** devices
 * **Schema:** V1__initial_schema.sql (lines 88-113)
 * 
 * **Purpose:**
 * - Device enrollment and lifecycle management
 * - Compliance monitoring and enforcement
 * - Remote command execution (lock, wipe, sync)
 * - FCM push notification support
 * - Organization device inventory
 * 
 * **Lifecycle:**
 * 1. Enrollment: Device registers with organization
 * 2. Active: Regular heartbeat updates, compliance checks
 * 3. Locked: Remote lock issued (device disabled)
 * 4. Wiped: Remote wipe issued (data erased)
 * 
 * **Compliance:**
 * - PENDING: Initial state, awaiting first evaluation
 * - COMPLIANT: Passes all policy checks
 * - NON_COMPLIANT: Fails one or more policy checks
 * - EXEMPTED: Manually exempted from policies
 * 
 * **Relationships:**
 * - ManyToOne: Organization (tenant)
 * - ManyToOne: User (enrolledBy)
 * - OneToMany: DeviceCommands (remote commands)
 * - OneToMany: PolicyEvaluations (compliance history)
 */
@Entity
@Table(
    name = "devices",
    indexes = [
        Index(name = "idx_devices_organization", columnList = "organization_id"),
        Index(name = "idx_devices_device_id", columnList = "device_id"),
        Index(name = "idx_devices_compliance", columnList = "compliance_status"),
        Index(name = "idx_devices_last_seen", columnList = "last_seen_at"),
        Index(name = "idx_devices_fcm_token", columnList = "fcm_token")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "unq_device_org", columnNames = ["organization_id", "device_id"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
data class Device(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    
    @Column(name = "device_id", nullable = false, length = 255)
    @field:NotBlank(message = "Device ID is required")
    val deviceId: String,
    
    @Column(name = "device_name", nullable = false, length = 255)
    @field:NotBlank(message = "Device name is required")
    @field:Size(max = 255, message = "Device name cannot exceed 255 characters")
    val deviceName: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "os_type", nullable = false, length = 50)
    val osType: OsType,
    
    @Column(name = "os_version", length = 100)
    val osVersion: String? = null,
    
    @Column(name = "app_version", length = 100)
    val appVersion: String? = null,
    
    @Column(name = "manufacturer", length = 100)
    val manufacturer: String? = null,
    
    @Column(name = "model", length = 100)
    val model: String? = null,
    
    @Column(name = "enrolled_at", nullable = false)
    val enrolledAt: Instant = Instant.now(),
    
    @Column(name = "enrolled_by")
    val enrolledBy: UUID? = null,
    
    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null,
    
    @Column(name = "last_seen_ip", columnDefinition = "inet")
    var lastSeenIp: String? = null,
    
    @Column(name = "fcm_token", length = 500)
    var fcmToken: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", nullable = false, length = 50)
    var complianceStatus: ComplianceStatus = ComplianceStatus.PENDING,
    
    @Column(name = "locked", nullable = false)
    var locked: Boolean = false,
    
    @Column(name = "wiped", nullable = false)
    var wiped: Boolean = false,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: Map<String, Any>? = null,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * OS type enum.
     * Matches database constraint: CHECK (os_type IN ('ANDROID', 'IOS', 'WINDOWS', 'MACOS', 'LINUX'))
     */
    enum class OsType {
        ANDROID,
        IOS,
        WINDOWS,
        MACOS,
        LINUX
    }
    
    /**
     * Compliance status enum.
     * Matches database constraint: CHECK (compliance_status IN ('PENDING', 'COMPLIANT', 'NON_COMPLIANT', 'EXEMPTED'))
     */
    enum class ComplianceStatus {
        /**
         * Pending: Initial state, awaiting first compliance evaluation.
         */
        PENDING,
        
        /**
         * Compliant: Device passes all policy checks.
         */
        COMPLIANT,
        
        /**
         * Non-compliant: Device fails one or more policy checks.
         */
        NON_COMPLIANT,
        
        /**
         * Exempted: Device manually exempted from compliance checks.
         */
        EXEMPTED
    }
    
    /**
     * Update device heartbeat.
     * Called when device checks in.
     * 
     * @param ipAddress Device IP address
     */
    fun updateHeartbeat(ipAddress: String?) {
        lastSeenAt = Instant.now()
        lastSeenIp = ipAddress
    }
    
    /**
     * Update FCM token for push notifications.
     * 
     * **Research Citation:** Finding 3 - FCM High-Priority Push
     * 
     * @param token FCM registration token
     */
    fun updateFcmToken(token: String?) {
        fcmToken = token
    }
    
    /**
     * Update compliance status after policy evaluation.
     * 
     * @param status New compliance status
     */
    fun updateComplianceStatus(status: ComplianceStatus) {
        complianceStatus = status
    }
    
    /**
     * Lock device (disable functionality).
     */
    fun lock() {
        locked = true
    }
    
    /**
     * Unlock device (restore functionality).
     */
    fun unlock() {
        locked = false
    }
    
    /**
     * Mark device as wiped (data erased).
     */
    fun markAsWiped() {
        wiped = true
    }
    
    /**
     * Check if device is active (seen recently).
     * 
     * @param thresholdMinutes Minutes since last seen (default: 60)
     * @return True if seen within threshold
     */
    fun isActive(thresholdMinutes: Long = 60): Boolean {
        val lastSeen = lastSeenAt ?: return false
        val threshold = Instant.now().minusSeconds(thresholdMinutes * 60)
        return lastSeen.isAfter(threshold)
    }
    
    /**
     * Check if device is enrolled (not wiped).
     */
    fun isEnrolled(): Boolean = !wiped
    
    /**
     * Check if device can receive commands.
     */
    fun canReceiveCommands(): Boolean = !wiped && fcmToken != null
    
    /**
     * Check if device is compliant.
     */
    fun isCompliant(): Boolean = complianceStatus == ComplianceStatus.COMPLIANT
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Device(id=$id, deviceId='$deviceId', name='$deviceName', osType=$osType, compliance=$complianceStatus)"
    }
}
