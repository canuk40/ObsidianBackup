package com.obsidianbackup.enterprise.admin

import com.obsidianbackup.enterprise.auth.JwtService
import com.obsidianbackup.enterprise.audit.AuditLogService
import com.obsidianbackup.enterprise.devices.CommandResult
import com.obsidianbackup.enterprise.devices.DeviceCommandService
import com.obsidianbackup.enterprise.devices.DeviceEnrollmentService
import com.obsidianbackup.enterprise.model.Device
import com.obsidianbackup.enterprise.model.DeviceCommand
import com.obsidianbackup.enterprise.model.Policy
import com.obsidianbackup.enterprise.policies.ComplianceEvaluationResult
import com.obsidianbackup.enterprise.policies.PolicyEnforcementService
import com.obsidianbackup.enterprise.policies.PolicyViolation
import com.obsidianbackup.enterprise.repository.DeviceRepository
import com.obsidianbackup.enterprise.repository.PolicyRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.time.Instant
import java.util.*

/**
 * Device Management REST API Controller.
 * 
 * **Research Citations:**
 * - Finding 2: MDM REST API Architecture
 * - Finding 7: Admin REST API Design (pagination, filtering, versioning)
 * 
 * **Purpose:**
 * - Device enrollment into MDM system
 * - Device listing with filtering and pagination
 * - Remote device commands (lock, wipe)
 * - Compliance status checking
 * 
 * **Base Path:** `/api/v1/devices`
 * 
 * **Security:**
 * - All endpoints require authentication
 * - Lock/Wipe: Admin role only (SUPER_ADMIN, ADMIN)
 * - Enrollment: DEVICE_MANAGER or higher
 * - Organization isolation via JWT claims
 * 
 * **Endpoints:**
 * 1. POST /enroll - Enroll new device
 * 2. GET / - List devices (paginated)
 * 3. GET /{id} - Get device details
 * 4. POST /{id}/commands/lock - Lock device
 * 5. POST /{id}/commands/wipe - Wipe device (factory reset)
 * 6. GET /{id}/compliance - Get compliance status
 */
@RestController
@RequestMapping("/api/v1/devices")
@Validated
class DeviceController(
    private val deviceEnrollmentService: DeviceEnrollmentService,
    private val deviceCommandService: DeviceCommandService,
    private val policyEnforcementService: PolicyEnforcementService,
    private val deviceRepository: DeviceRepository,
    private val policyRepository: PolicyRepository,
    private val auditLogService: AuditLogService,
    private val jwtService: JwtService
) {
    
    private val logger = LoggerFactory.getLogger(DeviceController::class.java)
    
    /**
     * Enroll new device into MDM system.
     * 
     * **Research Citation:** Finding 2 - MDM REST API Architecture
     * 
     * **Process:**
     * 1. Validate enrollment token (JWT)
     * 2. Check organization quota (maxDevices)
     * 3. Create device record
     * 4. Assign default policies
     * 5. Log enrollment event
     * 
     * **Access:** DEVICE_MANAGER or higher
     * 
     * @param request Enrollment request
     * @param principal Authenticated user
     * @param httpRequest HTTP request (for IP address)
     * @return Enrollment response (201 Created)
     */
    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVICE_MANAGER')")
    fun enrollDevice(
        @Valid @RequestBody request: EnrollDeviceRequest,
        @AuthenticationPrincipal principal: UserDetails,
        httpRequest: HttpServletRequest
    ): ResponseEntity<EnrollDeviceResponse> {
        return try {
            val ipAddress = httpRequest.remoteAddr
            val organizationId = extractOrganizationId(principal)
            
            // Validate OS type
            val osType = try {
                Device.OsType.valueOf(request.osType.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(
                    EnrollDeviceResponse.error("Invalid OS type: ${request.osType}")
                )
            }
            
            // Enroll device
            when (val result = deviceEnrollmentService.enrollDevice(
                enrollmentToken = request.enrollmentToken,
                deviceId = request.deviceId,
                manufacturer = request.manufacturer,
                model = request.model,
                osType = osType,
                osVersion = request.osVersion,
                appVersion = request.appVersion,
                fcmToken = request.fcmToken,
                organizationId = organizationId,
                ipAddress = ipAddress
            )) {
                is com.obsidianbackup.enterprise.devices.EnrollmentResult.Success -> {
                    // Get assigned policies
                    val policies = policyRepository.findByOrganizationIdAndEnabledOrderByPriorityDesc(
                        organizationId, true
                    )
                    
                    val policySummaries = policies.map { policy ->
                        PolicySummary(
                            id = policy.id!!,
                            name = policy.name,
                            type = policy.type.name,
                            priority = policy.priority
                        )
                    }
                    
                    logger.info(
                        "Device enrolled: deviceId={}, orgId={}, assignedPolicies={}",
                        result.device.id, organizationId, policies.size
                    )
                    
                    ResponseEntity.status(HttpStatus.CREATED).body(
                        EnrollDeviceResponse.success(
                            deviceId = result.device.id!!,
                            status = "ENROLLED",
                            assignedPolicies = policySummaries
                        )
                    )
                }
                is com.obsidianbackup.enterprise.devices.EnrollmentResult.Failure -> {
                    logger.warn("Device enrollment failed: {}", result.error)
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        EnrollDeviceResponse.error(result.error)
                    )
                }
            }
            
        } catch (e: Exception) {
            logger.error("Device enrollment error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EnrollDeviceResponse.error("Enrollment failed: ${e.message}")
            )
        }
    }
    
    /**
     * List devices for organization.
     * 
     * **Research Citation:** Finding 7 - Admin REST API pagination and filtering
     * 
     * **Query Parameters:**
     * - page (default: 0) - Page number
     * - size (default: 20) - Page size
     * - complianceStatus - Filter by compliance status
     * - userId - Filter by user
     * - osType - Filter by OS type
     * 
     * **Access:** DEVICE_MANAGER or higher
     * 
     * @param page Page number
     * @param size Page size
     * @param complianceStatus Compliance status filter
     * @param userId User ID filter
     * @param osType OS type filter
     * @param principal Authenticated user
     * @return Paginated device list (200 OK)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVICE_MANAGER', 'VIEWER')")
    fun listDevices(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) complianceStatus: String?,
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) osType: String?,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<DeviceListResponse> {
        return try {
            val organizationId = extractOrganizationId(principal)
            
            // Validate page size
            val validSize = size.coerceIn(1, 100)
            val pageable = PageRequest.of(page, validSize, Sort.by("enrolledAt").descending())
            
            // Get devices for organization
            var devices = deviceRepository.findByOrganizationId(organizationId)
            
            // Apply filters
            if (complianceStatus != null) {
                val status = try {
                    Device.ComplianceStatus.valueOf(complianceStatus.uppercase())
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(
                        DeviceListResponse.error("Invalid compliance status: $complianceStatus")
                    )
                }
                devices = devices.filter { it.complianceStatus == status }
            }
            
            if (userId != null) {
                devices = devices.filter { it.userId == userId }
            }
            
            if (osType != null) {
                val osTypeEnum = try {
                    Device.OsType.valueOf(osType.uppercase())
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(
                        DeviceListResponse.error("Invalid OS type: $osType")
                    )
                }
                devices = devices.filter { it.osType == osTypeEnum }
            }
            
            // Paginate manually (in production, use database pagination)
            val totalElements = devices.size.toLong()
            val totalPages = ((totalElements + validSize - 1) / validSize).toInt()
            val startIndex = page * validSize
            val endIndex = minOf(startIndex + validSize, devices.size)
            val pageDevices = if (startIndex < devices.size) {
                devices.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            val deviceSummaries = pageDevices.map { device ->
                DeviceSummary(
                    id = device.id!!,
                    deviceId = device.deviceId,
                    manufacturer = device.manufacturer,
                    model = device.model,
                    osType = device.osType.name,
                    osVersion = device.osVersion,
                    complianceStatus = device.complianceStatus.name,
                    lastSeenAt = device.lastSeenAt,
                    enrolledAt = device.enrolledAt
                )
            }
            
            val pageInfo = PageInfo(
                page = page,
                size = validSize,
                totalElements = totalElements,
                totalPages = totalPages
            )
            
            ResponseEntity.ok(DeviceListResponse.success(deviceSummaries, pageInfo))
            
        } catch (e: Exception) {
            logger.error("Device list error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DeviceListResponse.error("Failed to list devices: ${e.message}")
            )
        }
    }
    
    /**
     * Get device details.
     * 
     * **Access:** Device owner or DEVICE_MANAGER or higher
     * 
     * @param id Device UUID
     * @param principal Authenticated user
     * @return Device details (200 OK)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVICE_MANAGER', 'VIEWER')")
    fun getDevice(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<DeviceDetailResponse> {
        return try {
            val organizationId = extractOrganizationId(principal)
            
            val device = deviceRepository.findById(id).orElse(null)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    DeviceDetailResponse.error("Device not found")
                )
            
            // Check organization access
            if (device.organizationId != organizationId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    DeviceDetailResponse.error("Access denied")
                )
            }
            
            // Get assigned policies
            val policies = policyRepository.findByOrganizationIdAndEnabledOrderByPriorityDesc(
                organizationId, true
            )
            
            val policySummaries = policies.map { policy ->
                PolicySummary(
                    id = policy.id!!,
                    name = policy.name,
                    type = policy.type.name,
                    priority = policy.priority
                )
            }
            
            ResponseEntity.ok(
                DeviceDetailResponse.success(
                    id = device.id!!,
                    deviceId = device.deviceId,
                    userId = device.userId,
                    manufacturer = device.manufacturer,
                    model = device.model,
                    osType = device.osType.name,
                    osVersion = device.osVersion,
                    appVersion = device.appVersion,
                    fcmToken = device.fcmToken,
                    complianceStatus = device.complianceStatus.name,
                    isLocked = device.isLocked,
                    isWiped = device.isWiped,
                    enrolledAt = device.enrolledAt,
                    lastSeenAt = device.lastSeenAt,
                    metadata = device.metadata,
                    assignedPolicies = policySummaries
                )
            )
            
        } catch (e: Exception) {
            logger.error("Get device error: id={}", id, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DeviceDetailResponse.error("Failed to get device: ${e.message}")
            )
        }
    }
    
    /**
     * Lock device remotely.
     * 
     * **Research Citation:** Finding 2 - MDM remote commands
     * 
     * **Effect:**
     * - Device screen locks immediately
     * - User cannot access device until unlocked by admin
     * - Background sync continues
     * 
     * **Access:** ADMIN or higher only
     * 
     * @param id Device UUID
     * @param request Lock request
     * @param principal Authenticated user
     * @param httpRequest HTTP request (for IP address)
     * @return Command response (202 Accepted)
     */
    @PostMapping("/{id}/commands/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    fun lockDevice(
        @PathVariable id: UUID,
        @Valid @RequestBody request: LockDeviceRequest,
        @AuthenticationPrincipal principal: UserDetails,
        httpRequest: HttpServletRequest
    ): ResponseEntity<CommandResponse> {
        return try {
            val organizationId = extractOrganizationId(principal)
            val userId = extractUserId(principal)
            val ipAddress = httpRequest.remoteAddr
            
            val device = deviceRepository.findById(id).orElse(null)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommandResponse.error("Device not found")
                )
            
            // Check organization access
            if (device.organizationId != organizationId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    CommandResponse.error("Access denied")
                )
            }
            
            // Check if device already locked or wiped
            if (device.isLocked) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    CommandResponse.error("Device is already locked")
                )
            }
            
            if (device.isWiped) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    CommandResponse.error("Device has been wiped")
                )
            }
            
            // Send lock command
            when (val result = deviceCommandService.lockDevice(id, request.reason, userId)) {
                is CommandResult.Success -> {
                    ResponseEntity.status(HttpStatus.ACCEPTED).body(
                        CommandResponse.success(
                            commandId = result.command.id!!,
                            status = result.command.status.name,
                            issuedAt = result.command.issuedAt,
                            message = "Lock command queued successfully"
                        )
                    )
                }
                is CommandResult.Failure -> {
                    logger.warn("Lock command failed: deviceId={}, error={}", id, result.error)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        CommandResponse.error(result.error)
                    )
                }
            }
            
        } catch (e: Exception) {
            logger.error("Lock device error: id={}", id, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CommandResponse.error("Lock command failed: ${e.message}")
            )
        }
    }
    
    /**
     * Wipe device remotely (factory reset).
     * 
     * **Research Citation:** Finding 3 - High-priority FCM for critical wipe command
     * 
     * **Effect:**
     * - All device data erased
     * - Factory reset performed
     * - Device unenrolled from MDM
     * - Cannot be re-enrolled (security measure)
     * 
     * **Use Cases:**
     * - Lost/stolen device
     * - Employee termination
     * - Security breach
     * 
     * **Access:** ADMIN or higher only
     * **Confirmation Required:** Must send "CONFIRM_WIPE"
     * 
     * @param id Device UUID
     * @param request Wipe request with confirmation
     * @param principal Authenticated user
     * @param httpRequest HTTP request (for IP address)
     * @return Command response (202 Accepted)
     */
    @PostMapping("/{id}/commands/wipe")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    fun wipeDevice(
        @PathVariable id: UUID,
        @Valid @RequestBody request: WipeDeviceRequest,
        @AuthenticationPrincipal principal: UserDetails,
        httpRequest: HttpServletRequest
    ): ResponseEntity<CommandResponse> {
        return try {
            val organizationId = extractOrganizationId(principal)
            val userId = extractUserId(principal)
            val ipAddress = httpRequest.remoteAddr
            
            // Validate confirmation
            if (request.confirmation != "CONFIRM_WIPE") {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommandResponse.error("Invalid confirmation. Must be 'CONFIRM_WIPE'")
                )
            }
            
            val device = deviceRepository.findById(id).orElse(null)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommandResponse.error("Device not found")
                )
            
            // Check organization access
            if (device.organizationId != organizationId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    CommandResponse.error("Access denied")
                )
            }
            
            // Check if device already wiped
            if (device.isWiped) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    CommandResponse.error("Device has already been wiped")
                )
            }
            
            // Send wipe command
            when (val result = deviceCommandService.wipeDevice(id, request.reason, userId, ipAddress)) {
                is CommandResult.Success -> {
                    logger.warn(
                        "CRITICAL: Wipe command issued - deviceId={}, adminId={}, reason={}",
                        id, userId, request.reason
                    )
                    
                    ResponseEntity.status(HttpStatus.ACCEPTED).body(
                        CommandResponse.success(
                            commandId = result.command.id!!,
                            status = result.command.status.name,
                            issuedAt = result.command.issuedAt,
                            message = "Wipe command queued successfully. Device will be factory reset."
                        )
                    )
                }
                is CommandResult.Failure -> {
                    logger.error("Wipe command failed: deviceId={}, error={}", id, result.error)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        CommandResponse.error(result.error)
                    )
                }
            }
            
        } catch (e: Exception) {
            logger.error("Wipe device error: id={}", id, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CommandResponse.error("Wipe command failed: ${e.message}")
            )
        }
    }
    
    /**
     * Get device compliance status.
     * 
     * **Research Citation:** Finding 4 - Policy enforcement evaluation
     * 
     * **Process:**
     * 1. Trigger live compliance evaluation
     * 2. Check device against all active policies
     * 3. Return violations with severity
     * 4. Update device compliance status
     * 
     * **Access:** Device owner or DEVICE_MANAGER or higher
     * 
     * @param id Device UUID
     * @param principal Authenticated user
     * @return Compliance status with violations (200 OK)
     */
    @GetMapping("/{id}/compliance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DEVICE_MANAGER', 'VIEWER')")
    fun getCompliance(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<ComplianceResponse> {
        return try {
            val organizationId = extractOrganizationId(principal)
            
            val device = deviceRepository.findById(id).orElse(null)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ComplianceResponse.error("Device not found")
                )
            
            // Check organization access
            if (device.organizationId != organizationId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ComplianceResponse.error("Access denied")
                )
            }
            
            // Trigger compliance evaluation
            when (val result = policyEnforcementService.evaluateCompliance(id)) {
                is ComplianceEvaluationResult.Success -> {
                    val violationDetails = result.violations.map { violation ->
                        ViolationDetail(
                            policyId = violation.policyId,
                            policyName = violation.policyName,
                            rule = violation.rule,
                            message = violation.message,
                            severity = violation.severity.name
                        )
                    }
                    
                    ResponseEntity.ok(
                        ComplianceResponse.success(
                            deviceId = id,
                            complianceStatus = result.device.complianceStatus.name,
                            violations = violationDetails,
                            evaluatedPolicies = result.evaluatedPolicies,
                            lastEvaluatedAt = Instant.now()
                        )
                    )
                }
                is ComplianceEvaluationResult.Failure -> {
                    logger.error("Compliance evaluation failed: deviceId={}, error={}", id, result.error)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ComplianceResponse.error(result.error)
                    )
                }
            }
            
        } catch (e: Exception) {
            logger.error("Get compliance error: id={}", id, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ComplianceResponse.error("Compliance check failed: ${e.message}")
            )
        }
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    /**
     * M-10: Extract organization ID from authenticated principal via JWT claims.
     * Previously: Used a hardcoded placeholder UUID.
     */
    private fun extractOrganizationId(principal: UserDetails): UUID {
        return (principal as? com.obsidianbackup.enterprise.model.User)?.organizationId
            ?: throw org.springframework.security.access.AccessDeniedException("Cannot determine organization from principal")
    }
    
    /**
     * M-10: Extract user ID from authenticated principal via JWT claims.
     * Previously: Used a hardcoded placeholder UUID.
     */
    private fun extractUserId(principal: UserDetails): UUID {
        return (principal as? com.obsidianbackup.enterprise.model.User)?.id
            ?: throw org.springframework.security.access.AccessDeniedException("Cannot determine user ID from principal")
    }
}

// ============================================================================
// REQUEST DTOs
// ============================================================================

/**
 * Device enrollment request.
 */
data class EnrollDeviceRequest(
    @field:NotBlank(message = "Enrollment token is required")
    val enrollmentToken: String,
    
    @field:NotBlank(message = "Device ID is required")
    val deviceId: String,
    
    val manufacturer: String?,
    val model: String?,
    
    @field:NotBlank(message = "OS type is required")
    @field:Pattern(regexp = "ANDROID|IOS", message = "OS type must be ANDROID or IOS")
    val osType: String,
    
    val osVersion: String?,
    
    @field:NotBlank(message = "App version is required")
    val appVersion: String,
    
    val fcmToken: String?
)

/**
 * Lock device request.
 */
data class LockDeviceRequest(
    @field:NotBlank(message = "Lock reason is required")
    val reason: String
)

/**
 * Wipe device request.
 */
data class WipeDeviceRequest(
    @field:NotBlank(message = "Wipe reason is required")
    val reason: String,
    
    @field:NotBlank(message = "Confirmation is required")
    @field:Pattern(regexp = "CONFIRM_WIPE", message = "Confirmation must be 'CONFIRM_WIPE'")
    val confirmation: String
)

// ============================================================================
// RESPONSE DTOs
// ============================================================================

/**
 * Device enrollment response.
 */
data class EnrollDeviceResponse(
    val success: Boolean,
    val deviceId: UUID? = null,
    val status: String? = null,
    val assignedPolicies: List<PolicySummary>? = null,
    val error: String? = null
) {
    companion object {
        fun success(deviceId: UUID, status: String, assignedPolicies: List<PolicySummary>) =
            EnrollDeviceResponse(true, deviceId, status, assignedPolicies, null)
        
        fun error(error: String) =
            EnrollDeviceResponse(false, null, null, null, error)
    }
}

/**
 * Device list response.
 */
data class DeviceListResponse(
    val success: Boolean,
    val devices: List<DeviceSummary>? = null,
    val page: PageInfo? = null,
    val error: String? = null
) {
    companion object {
        fun success(devices: List<DeviceSummary>, page: PageInfo) =
            DeviceListResponse(true, devices, page, null)
        
        fun error(error: String) =
            DeviceListResponse(false, null, null, error)
    }
}

/**
 * Device summary for list view.
 */
data class DeviceSummary(
    val id: UUID,
    val deviceId: String,
    val manufacturer: String?,
    val model: String?,
    val osType: String,
    val osVersion: String?,
    val complianceStatus: String,
    val lastSeenAt: Instant?,
    val enrolledAt: Instant
)

/**
 * Device detail response.
 */
data class DeviceDetailResponse(
    val success: Boolean,
    val id: UUID? = null,
    val deviceId: String? = null,
    val userId: UUID? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val osType: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val fcmToken: String? = null,
    val complianceStatus: String? = null,
    val isLocked: Boolean? = null,
    val isWiped: Boolean? = null,
    val enrolledAt: Instant? = null,
    val lastSeenAt: Instant? = null,
    val metadata: Map<String, Any>? = null,
    val assignedPolicies: List<PolicySummary>? = null,
    val error: String? = null
) {
    companion object {
        fun success(
            id: UUID,
            deviceId: String,
            userId: UUID,
            manufacturer: String?,
            model: String?,
            osType: String,
            osVersion: String?,
            appVersion: String,
            fcmToken: String?,
            complianceStatus: String,
            isLocked: Boolean,
            isWiped: Boolean,
            enrolledAt: Instant,
            lastSeenAt: Instant?,
            metadata: Map<String, Any>?,
            assignedPolicies: List<PolicySummary>
        ) = DeviceDetailResponse(
            success = true,
            id = id,
            deviceId = deviceId,
            userId = userId,
            manufacturer = manufacturer,
            model = model,
            osType = osType,
            osVersion = osVersion,
            appVersion = appVersion,
            fcmToken = fcmToken,
            complianceStatus = complianceStatus,
            isLocked = isLocked,
            isWiped = isWiped,
            enrolledAt = enrolledAt,
            lastSeenAt = lastSeenAt,
            metadata = metadata,
            assignedPolicies = assignedPolicies,
            error = null
        )
        
        fun error(error: String) =
            DeviceDetailResponse(success = false, error = error)
    }
}

/**
 * Command execution response.
 */
data class CommandResponse(
    val success: Boolean,
    val commandId: UUID? = null,
    val status: String? = null,
    val issuedAt: Instant? = null,
    val message: String? = null,
    val error: String? = null
) {
    companion object {
        fun success(commandId: UUID, status: String, issuedAt: Instant, message: String) =
            CommandResponse(true, commandId, status, issuedAt, message, null)
        
        fun error(error: String) =
            CommandResponse(false, null, null, null, null, error)
    }
}

/**
 * Compliance evaluation response.
 */
data class ComplianceResponse(
    val success: Boolean,
    val deviceId: UUID? = null,
    val complianceStatus: String? = null,
    val violations: List<ViolationDetail>? = null,
    val evaluatedPolicies: Int? = null,
    val lastEvaluatedAt: Instant? = null,
    val error: String? = null
) {
    companion object {
        fun success(
            deviceId: UUID,
            complianceStatus: String,
            violations: List<ViolationDetail>,
            evaluatedPolicies: Int,
            lastEvaluatedAt: Instant
        ) = ComplianceResponse(
            success = true,
            deviceId = deviceId,
            complianceStatus = complianceStatus,
            violations = violations,
            evaluatedPolicies = evaluatedPolicies,
            lastEvaluatedAt = lastEvaluatedAt,
            error = null
        )
        
        fun error(error: String) =
            ComplianceResponse(success = false, error = error)
    }
}

/**
 * Policy violation detail.
 */
data class ViolationDetail(
    val policyId: UUID,
    val policyName: String,
    val rule: String,
    val message: String,
    val severity: String
)

/**
 * Policy summary.
 */
data class PolicySummary(
    val id: UUID,
    val name: String,
    val type: String,
    val priority: Int
)

/**
 * Pagination info.
 */
data class PageInfo(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
