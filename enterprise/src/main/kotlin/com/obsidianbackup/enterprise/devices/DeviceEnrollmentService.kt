package com.obsidianbackup.enterprise.devices

import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.SignedJWT
import com.obsidianbackup.enterprise.audit.AuditLogService
import com.obsidianbackup.enterprise.model.Device
import com.obsidianbackup.enterprise.repository.DeviceRepository
import com.obsidianbackup.enterprise.repository.OrganizationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Device Enrollment Service for MDM device lifecycle management.
 * 
 * **Research Citation:** Finding 2 - MDM REST API Architecture
 * 
 * **Purpose:**
 * - Device enrollment and registration
 * - Enrollment token validation
 * - Device heartbeat tracking
 * - Organization quota enforcement
 * 
 * **Enrollment Flow:**
 * 1. Device requests enrollment with token
 * 2. Validate enrollment token
 * 3. Check organization device quota
 * 4. Create device record
 * 5. Assign default policies
 * 6. Return device credentials
 * 7. Log enrollment audit event
 * 
 * **Heartbeat:**
 * - Devices must check in regularly (heartbeat)
 * - Updates lastSeenAt, lastSeenIp
 * - Inactive devices flagged for compliance
 */
@Service
@Transactional
class DeviceEnrollmentService(
    private val deviceRepository: DeviceRepository,
    private val organizationRepository: OrganizationRepository,
    private val auditLogService: AuditLogService,
    @org.springframework.beans.factory.annotation.Value("\${application.enrollment.token-secret:changeme-set-in-production}")
    private val enrollmentTokenSecret: String   // M-8: Injected from application properties
) {
    
    private val logger = LoggerFactory.getLogger(DeviceEnrollmentService::class.java)

    companion object {
        private const val TAG = "DeviceEnrollmentService"
        private const val EXPECTED_ISSUER = "ObsidianEnterprise"
    }
    
    /**
     * Enroll new device in organization.
     * 
     * **Research Citation:** Finding 2 - Device enrollment endpoint
     * 
     * **Process:**
     * 1. Validate enrollment token
     * 2. Check organization quota
     * 3. Check device not already enrolled
     * 4. Create device record
     * 5. Assign default policies
     * 6. Log audit event
     * 
     * @param organizationId Organization UUID
     * @param deviceId Device identifier (Android ID, UDID, etc.)
     * @param deviceName User-friendly device name
     * @param osType Operating system type
     * @param osVersion OS version string
     * @param appVersion App version string
     * @param manufacturer Device manufacturer
     * @param model Device model
     * @param enrollmentToken Enrollment token (validates user authorization)
     * @param enrolledBy User enrolling the device
     * @param ipAddress Device IP address
     * @param metadata Additional device metadata
     * @return Enrollment result
     */
    fun enrollDevice(
        organizationId: UUID,
        deviceId: String,
        deviceName: String,
        osType: Device.OsType,
        osVersion: String?,
        appVersion: String?,
        manufacturer: String?,
        model: String?,
        enrollmentToken: String,
        enrolledBy: UUID,
        ipAddress: String?,
        metadata: Map<String, Any>?
    ): EnrollmentResult {
        return try {
            // Validate enrollment token
            if (!validateEnrollmentToken(enrollmentToken, organizationId)) {
                return EnrollmentResult.failure("Invalid enrollment token")
            }
            
            // Get organization
            val organization = organizationRepository.findById(organizationId).orElse(null)
                ?: return EnrollmentResult.failure("Organization not found")
            
            // Check organization is active
            if (!organization.isActive()) {
                return EnrollmentResult.failure("Organization is not active")
            }
            
            // Check device quota
            val currentDeviceCount = deviceRepository.countByOrganizationId(organizationId)
            if (!organization.canAddDevice(currentDeviceCount.toInt())) {
                logger.warn("Device enrollment rejected: quota exceeded for org={}", organizationId)
                return EnrollmentResult.failure("Device quota exceeded for organization")
            }
            
            // Check device not already enrolled
            val existingDevice = deviceRepository.findByOrganizationIdAndDeviceId(organizationId, deviceId)
            if (existingDevice != null) {
                if (existingDevice.wiped) {
                    return EnrollmentResult.failure("Device was previously wiped and cannot be re-enrolled")
                }
                logger.info("Device already enrolled: deviceId={}", deviceId)
                return EnrollmentResult.alreadyEnrolled(existingDevice)
            }
            
            // Create device record
            val device = Device(
                organizationId = organizationId,
                deviceId = deviceId,
                deviceName = deviceName,
                osType = osType,
                osVersion = osVersion,
                appVersion = appVersion,
                manufacturer = manufacturer,
                model = model,
                enrolledAt = Instant.now(),
                enrolledBy = enrolledBy,
                lastSeenAt = Instant.now(),
                lastSeenIp = ipAddress,
                complianceStatus = Device.ComplianceStatus.PENDING,
                metadata = metadata
            )
            
            val savedDevice = deviceRepository.save(device)
            
            // Log enrollment audit event
            auditLogService.logDeviceEnrollment(
                adminId = enrolledBy,
                organizationId = organizationId,
                deviceId = savedDevice.id!!,
                deviceInfo = mapOf(
                    "deviceId" to deviceId,
                    "deviceName" to deviceName,
                    "osType" to osType.name,
                    "osVersion" to (osVersion ?: "unknown"),
                    "manufacturer" to (manufacturer ?: "unknown"),
                    "model" to (model ?: "unknown")
                )
            )
            
            logger.info(
                "Device enrolled: deviceId={}, org={}, osType={}",
                deviceId, organizationId, osType
            )
            
            EnrollmentResult.success(savedDevice)
            
        } catch (e: Exception) {
            logger.error("Device enrollment failed: deviceId={}, org={}", deviceId, organizationId, e)
            EnrollmentResult.failure("Enrollment failed: ${e.message}")
        }
    }
    
    /**
     * Validate enrollment token.
     * 
     * **Note:** This is a placeholder implementation.
     * In production, use JWT-based enrollment tokens with:
     * - Organization ID claim
     * - Expiration (e.g., 24 hours)
     * - Single-use enforcement
     * - Signature validation
     * 
     * @param token Enrollment token
     * @param organizationId Organization UUID
     * @return True if token is valid
     */
    /**
     * M-8: Validate the enrollment JWT token using Nimbus JOSE+JWT HMAC-SHA256.
     * Previously: TODO — any non-empty token was accepted.
     * SOURCE: https://connect2id.com/products/nimbus-jose-jwt
     */
    fun validateEnrollmentToken(token: String, organizationId: UUID): Boolean {
        if (token.isBlank()) return false
        return try {
            val signedJWT = SignedJWT.parse(token)

            // Verify signature with HMAC-SHA256
            val verifier = MACVerifier(enrollmentTokenSecret.toByteArray())
            if (!signedJWT.verify(verifier)) {
                logger.warn("Enrollment token signature verification failed for org={}", organizationId)
                return false
            }

            val claims = signedJWT.jwtClaimsSet

            // Validate expiration
            if (claims.expirationTime?.before(Date()) == true) {
                logger.warn("Enrollment token has expired for org={}", organizationId)
                return false
            }

            // Validate issuer
            if (claims.issuer != EXPECTED_ISSUER) {
                logger.warn("Enrollment token issuer mismatch: {} for org={}", claims.issuer, organizationId)
                return false
            }

            // Validate org claim matches expected organization
            val claimedOrgId = claims.getStringClaim("org_id")
            if (claimedOrgId != organizationId.toString()) {
                logger.warn("Enrollment token org_id mismatch for org={}", organizationId)
                return false
            }

            logger.debug("Enrollment token validated: org={}", organizationId)
            true
        } catch (e: Exception) {
            logger.error("Enrollment token validation error for org={}: {}", organizationId, e.message)
            false
        }
    }
    
    /**
     * Update device heartbeat.
     * 
     * **Purpose:**
     * - Track device activity
     * - Update last seen timestamp
     * - Update IP address
     * - Used for inactive device detection
     * 
     * @param deviceId Device UUID
     * @param ipAddress Device IP address
     * @param fcmToken FCM registration token (optional update)
     * @return True if heartbeat updated successfully
     */
    fun updateHeartbeat(
        deviceId: UUID,
        ipAddress: String?,
        fcmToken: String? = null
    ): Boolean {
        return try {
            val device = deviceRepository.findById(deviceId).orElse(null)
                ?: run {
                    logger.warn("Heartbeat update failed: device not found, deviceId={}", deviceId)
                    return false
                }
            
            // Check device not wiped
            if (device.wiped) {
                logger.warn("Heartbeat update rejected: device wiped, deviceId={}", deviceId)
                return false
            }
            
            // Update heartbeat
            device.updateHeartbeat(ipAddress)
            
            // Update FCM token if provided
            if (fcmToken != null) {
                device.updateFcmToken(fcmToken)
            }
            
            deviceRepository.save(device)
            
            logger.debug("Heartbeat updated: deviceId={}, ip={}", deviceId, ipAddress)
            true
            
        } catch (e: Exception) {
            logger.error("Heartbeat update failed: deviceId={}", deviceId, e)
            false
        }
    }
    
    /**
     * Unenroll device (remove from organization).
     * 
     * **Note:** This does not wipe the device, just removes enrollment.
     * For security wipe, use DeviceCommandService.wipeDevice()
     * 
     * @param deviceId Device UUID
     * @param unenrolledBy User UUID
     * @return True if unenrolled successfully
     */
    fun unenrollDevice(deviceId: UUID, unenrolledBy: UUID): Boolean {
        return try {
            val device = deviceRepository.findById(deviceId).orElse(null)
                ?: run {
                    logger.warn("Unenroll failed: device not found, deviceId={}", deviceId)
                    return false
                }
            
            // Log audit event before deletion
            auditLogService.logDeviceCommand(
                adminId = unenrolledBy,
                organizationId = device.organizationId,
                deviceId = deviceId,
                command = "UNENROLL",
                outcome = "SUCCESS",
                details = mapOf(
                    "deviceName" to device.deviceName,
                    "deviceId" to device.deviceId
                )
            )
            
            // Delete device record
            deviceRepository.delete(device)
            
            logger.info("Device unenrolled: deviceId={}, by={}", deviceId, unenrolledBy)
            true
            
        } catch (e: Exception) {
            logger.error("Unenroll failed: deviceId={}", deviceId, e)
            false
        }
    }
    
    /**
     * Get device by UUID.
     * 
     * @param deviceId Device UUID
     * @return Device or null
     */
    fun getDevice(deviceId: UUID): Device? {
        return deviceRepository.findById(deviceId).orElse(null)
    }
    
    /**
     * Get device by organization and device ID.
     * 
     * @param organizationId Organization UUID
     * @param deviceId Device identifier
     * @return Device or null
     */
    fun getDeviceByDeviceId(organizationId: UUID, deviceId: String): Device? {
        return deviceRepository.findByOrganizationIdAndDeviceId(organizationId, deviceId)
    }
    
    /**
     * Get all devices for organization.
     * 
     * @param organizationId Organization UUID
     * @return List of devices
     */
    fun getOrganizationDevices(organizationId: UUID): List<Device> {
        return deviceRepository.findByOrganizationId(organizationId)
    }
    
    /**
     * Get devices by compliance status.
     * 
     * @param organizationId Organization UUID
     * @param complianceStatus Compliance status
     * @return List of devices
     */
    fun getDevicesByComplianceStatus(
        organizationId: UUID,
        complianceStatus: Device.ComplianceStatus
    ): List<Device> {
        return deviceRepository.findByOrganizationIdAndComplianceStatus(organizationId, complianceStatus)
    }
}

/**
 * Device enrollment result.
 */
sealed class EnrollmentResult {
    data class Success(val device: Device) : EnrollmentResult()
    data class AlreadyEnrolled(val device: Device) : EnrollmentResult()
    data class Failure(val error: String) : EnrollmentResult()
    
    companion object {
        fun success(device: Device) = Success(device)
        fun alreadyEnrolled(device: Device) = AlreadyEnrolled(device)
        fun failure(error: String) = Failure(error)
    }
}
