package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.Device
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Repository for Device entity.
 * 
 * **Research Citation:** Finding 2 - MDM REST API Architecture
 * 
 * **Purpose:**
 * - Device enrollment and lookup queries
 * - Compliance status queries
 * - Device inventory management
 * - Heartbeat monitoring
 * - Statistics and reporting
 */
@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {
    
    /**
     * Find device by device ID (unique identifier from device).
     * 
     * **Use Case:** Device authentication, enrollment validation
     * 
     * @param deviceId Device identifier (e.g., Android ID, UDID)
     * @return Device or null
     */
    fun findByDeviceId(deviceId: String): Device?
    
    /**
     * Find device by organization and device ID.
     * 
     * **Use Case:** Multi-tenant device lookup
     * 
     * @param organizationId Organization UUID
     * @param deviceId Device identifier
     * @return Device or null
     */
    fun findByOrganizationIdAndDeviceId(organizationId: UUID, deviceId: String): Device?
    
    /**
     * Find all devices in organization.
     * 
     * **Use Case:** Device inventory listing
     * 
     * @param organizationId Organization UUID
     * @return List of devices
     */
    fun findByOrganizationId(organizationId: UUID): List<Device>
    
    /**
     * Find devices by compliance status.
     * 
     * **Use Case:** Compliance reporting, non-compliant device alerts
     * 
     * @param organizationId Organization UUID
     * @param complianceStatus Compliance status
     * @return List of devices
     */
    fun findByOrganizationIdAndComplianceStatus(
        organizationId: UUID,
        complianceStatus: Device.ComplianceStatus
    ): List<Device>
    
    /**
     * Find devices by OS type.
     * 
     * **Use Case:** Platform-specific policy enforcement
     * 
     * @param organizationId Organization UUID
     * @param osType OS type
     * @return List of devices
     */
    fun findByOrganizationIdAndOsType(organizationId: UUID, osType: Device.OsType): List<Device>
    
    /**
     * Find locked devices.
     * 
     * **Use Case:** Remote lock status monitoring
     * 
     * @param organizationId Organization UUID
     * @param locked Lock status
     * @return List of locked devices
     */
    fun findByOrganizationIdAndLocked(organizationId: UUID, locked: Boolean): List<Device>
    
    /**
     * Find wiped devices.
     * 
     * **Use Case:** Wiped device tracking
     * 
     * @param organizationId Organization UUID
     * @param wiped Wipe status
     * @return List of wiped devices
     */
    fun findByOrganizationIdAndWiped(organizationId: UUID, wiped: Boolean): List<Device>
    
    /**
     * Find devices enrolled by specific user.
     * 
     * **Use Case:** Admin tracking, user device inventory
     * 
     * @param organizationId Organization UUID
     * @param enrolledBy User UUID
     * @return List of devices
     */
    fun findByOrganizationIdAndEnrolledBy(organizationId: UUID, enrolledBy: UUID): List<Device>
    
    /**
     * Find devices last seen after timestamp.
     * 
     * **Use Case:** Active device monitoring
     * 
     * @param organizationId Organization UUID
     * @param lastSeenAfter Timestamp threshold
     * @return List of recently active devices
     */
    fun findByOrganizationIdAndLastSeenAtAfter(
        organizationId: UUID,
        lastSeenAfter: Instant
    ): List<Device>
    
    /**
     * Find devices with FCM token (can receive push).
     * 
     * **Research Citation:** Finding 3 - FCM High-Priority Push
     * 
     * @param organizationId Organization UUID
     * @return List of devices with FCM
     */
    @Query(
        "SELECT d FROM Device d " +
        "WHERE d.organizationId = :orgId " +
        "AND d.fcmToken IS NOT NULL " +
        "AND d.wiped = false"
    )
    fun findDevicesWithFcm(@Param("orgId") organizationId: UUID): List<Device>
    
    /**
     * Check if device ID exists in organization.
     * 
     * **Use Case:** Enrollment validation
     * 
     * @param organizationId Organization UUID
     * @param deviceId Device identifier
     * @return True if device exists
     */
    fun existsByOrganizationIdAndDeviceId(organizationId: UUID, deviceId: String): Boolean
    
    /**
     * Count devices in organization.
     * 
     * **Use Case:** Quota enforcement
     * 
     * @param organizationId Organization UUID
     * @return Device count
     */
    fun countByOrganizationId(organizationId: UUID): Long
    
    /**
     * Count devices by compliance status.
     * 
     * **Use Case:** Compliance metrics
     * 
     * @param organizationId Organization UUID
     * @param complianceStatus Compliance status
     * @return Device count
     */
    fun countByOrganizationIdAndComplianceStatus(
        organizationId: UUID,
        complianceStatus: Device.ComplianceStatus
    ): Long
    
    /**
     * Count devices by OS type.
     * 
     * **Use Case:** Platform distribution metrics
     * 
     * @param organizationId Organization UUID
     * @param osType OS type
     * @return Device count
     */
    fun countByOrganizationIdAndOsType(organizationId: UUID, osType: Device.OsType): Long
    
    /**
     * Get compliance statistics.
     * 
     * **Use Case:** Admin dashboard
     * 
     * @param organizationId Organization UUID
     * @return List of (complianceStatus, count) pairs
     */
    @Query(
        "SELECT d.complianceStatus, COUNT(d) FROM Device d " +
        "WHERE d.organizationId = :orgId " +
        "AND d.wiped = false " +
        "GROUP BY d.complianceStatus"
    )
    fun getComplianceStatistics(@Param("orgId") organizationId: UUID): List<Array<Any>>
    
    /**
     * Get OS type distribution.
     * 
     * **Use Case:** Platform analytics
     * 
     * @param organizationId Organization UUID
     * @return List of (osType, count) pairs
     */
    @Query(
        "SELECT d.osType, COUNT(d) FROM Device d " +
        "WHERE d.organizationId = :orgId " +
        "AND d.wiped = false " +
        "GROUP BY d.osType " +
        "ORDER BY COUNT(d) DESC"
    )
    fun getOsTypeDistribution(@Param("orgId") organizationId: UUID): List<Array<Any>>
    
    /**
     * Find inactive devices (not seen in threshold).
     * 
     * **Use Case:** Stale device cleanup, compliance alerts
     * 
     * @param organizationId Organization UUID
     * @param inactiveSince Timestamp threshold
     * @return List of inactive devices
     */
    @Query(
        "SELECT d FROM Device d " +
        "WHERE d.organizationId = :orgId " +
        "AND d.wiped = false " +
        "AND (d.lastSeenAt IS NULL OR d.lastSeenAt < :inactiveSince)"
    )
    fun findInactiveDevices(
        @Param("orgId") organizationId: UUID,
        @Param("inactiveSince") inactiveSince: Instant
    ): List<Device>
}
