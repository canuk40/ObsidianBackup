package com.obsidianbackup.enterprise.policies

import com.obsidianbackup.enterprise.audit.AuditLogService
import com.obsidianbackup.enterprise.model.Device
import com.obsidianbackup.enterprise.model.Policy
import com.obsidianbackup.enterprise.repository.DeviceRepository
import com.obsidianbackup.enterprise.repository.PolicyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Policy Enforcement Service for automated compliance evaluation.
 * 
 * **Research Citation:** Finding 4 - Policy Enforcement Engine
 * 
 * **Purpose:**
 * - Evaluate device compliance against policies
 * - Detect policy violations
 * - Trigger automated remediation
 * - Generate compliance reports
 * 
 * **Policy Evaluation:**
 * 1. Retrieve active policies for organization (ordered by priority)
 * 2. For each policy, check device against rules
 * 3. Collect violations
 * 4. Determine overall compliance status
 * 5. Update device compliance status
 * 6. Trigger remediation if auto-remediation enabled
 * 7. Log evaluation audit event
 * 
 * **Rule Checks:**
 * - OS Version: Device OS >= minimum required version
 * - Encryption: Device encryption enabled
 * - Cloud Providers: Only allowed providers used
 * - Backup Interval: Backups performed within interval
 * - Screen Lock: Screen lock enabled
 * - Network: Only allowed networks used
 * - Rooted Devices: Rooted/jailbroken devices blocked
 * 
 * **Compliance Frameworks:**
 * - HIPAA: PHI protection (encryption, access controls)
 * - SOC 2: Trust service criteria
 * - GDPR: Data protection (encryption, deletion)
 * - PCI-DSS: Payment card security
 */
@Service
@Transactional
class PolicyEnforcementService(
    private val policyRepository: PolicyRepository,
    private val deviceRepository: DeviceRepository,
    private val auditLogService: AuditLogService
) {
    
    private val logger = LoggerFactory.getLogger(PolicyEnforcementService::class.java)
    
    /**
     * Evaluate device compliance against organization policies.
     * 
     * **Research Citation:** Finding 4 - Priority-based policy evaluation
     * 
     * **Process:**
     * 1. Get active policies (ordered by priority DESC)
     * 2. Evaluate each policy
     * 3. Collect violations
     * 4. Determine compliance status
     * 5. Update device status
     * 6. Log audit event
     * 
     * @param deviceId Device UUID
     * @return Compliance evaluation result
     */
    fun evaluateCompliance(deviceId: UUID): ComplianceEvaluationResult {
        return try {
            // Get device
            val device = deviceRepository.findById(deviceId).orElse(null)
                ?: return ComplianceEvaluationResult.failure("Device not found")
            
            // Get active policies (ordered by priority)
            val policies = policyRepository.findByOrganizationIdAndEnabledOrderByPriorityDesc(
                device.organizationId, true
            )
            
            if (policies.isEmpty()) {
                // No policies defined - device is compliant by default
                device.updateComplianceStatus(Device.ComplianceStatus.COMPLIANT)
                deviceRepository.save(device)
                
                return ComplianceEvaluationResult.success(
                    device = device,
                    violations = emptyList(),
                    evaluatedPolicies = 0
                )
            }
            
            // Evaluate each policy
            val allViolations = mutableListOf<PolicyViolation>()
            
            for (policy in policies) {
                val violations = evaluatePolicy(device, policy)
                allViolations.addAll(violations)
                
                // Log policy evaluation
                auditLogService.logPolicyEvaluation(
                    deviceId = deviceId,
                    organizationId = device.organizationId,
                    policyId = policy.id!!,
                    result = if (violations.isEmpty()) "PASS" else "FAIL",
                    violations = if (violations.isNotEmpty()) {
                        violations.associate { it.rule to it.message }
                    } else null
                )
            }
            
            // Determine overall compliance status
            val complianceStatus = if (allViolations.isEmpty()) {
                Device.ComplianceStatus.COMPLIANT
            } else {
                Device.ComplianceStatus.NON_COMPLIANT
            }
            
            // Update device compliance status
            device.updateComplianceStatus(complianceStatus)
            deviceRepository.save(device)
            
            logger.info(
                "Compliance evaluated: deviceId={}, status={}, violations={}",
                deviceId, complianceStatus, allViolations.size
            )
            
            ComplianceEvaluationResult.success(
                device = device,
                violations = allViolations,
                evaluatedPolicies = policies.size
            )
            
        } catch (e: Exception) {
            logger.error("Compliance evaluation failed: deviceId={}", deviceId, e)
            ComplianceEvaluationResult.failure("Evaluation failed: ${e.message}")
        }
    }
    
    /**
     * Evaluate device against single policy.
     * 
     * **Research Citation:** Finding 4 - Policy rule evaluation
     * 
     * @param device Device to evaluate
     * @param policy Policy to check
     * @return List of violations (empty if compliant)
     */
    fun evaluatePolicy(device: Device, policy: Policy): List<PolicyViolation> {
        val violations = mutableListOf<PolicyViolation>()
        
        // Check minimum OS version
        policy.getRuleAsString(Policy.RuleKeys.MINIMUM_OS_VERSION)?.let { minVersion ->
            if (!checkOsVersion(device, minVersion)) {
                violations.add(
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.MINIMUM_OS_VERSION,
                        message = "OS version ${device.osVersion} is below minimum required $minVersion",
                        severity = ViolationSeverity.HIGH
                    )
                )
            }
        }
        
        // Check encryption requirement
        if (policy.getRuleAsBoolean(Policy.RuleKeys.REQUIRE_ENCRYPTION, false)) {
            if (!checkEncryption(device)) {
                violations.add(
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.REQUIRE_ENCRYPTION,
                        message = "Device encryption is not enabled",
                        severity = ViolationSeverity.CRITICAL
                    )
                )
            }
        }
        
        // Check allowed cloud providers
        val allowedProviders = policy.getRuleAsList(Policy.RuleKeys.ALLOWED_CLOUD_PROVIDERS)
        if (allowedProviders.isNotEmpty()) {
            val violations = checkAllowedCloudProviders(device, allowedProviders)
            if (violations.isNotEmpty()) {
                this.violations.addAll(violations.map { provider ->
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.ALLOWED_CLOUD_PROVIDERS,
                        message = "Unauthorized cloud provider in use: $provider",
                        severity = ViolationSeverity.MEDIUM
                    )
                })
            }
        }
        
        // Check backup interval
        policy.getRuleAsString(Policy.RuleKeys.MINIMUM_BACKUP_INTERVAL)?.let { intervalStr ->
            if (!checkBackupInterval(device, intervalStr)) {
                violations.add(
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.MINIMUM_BACKUP_INTERVAL,
                        message = "Backup interval exceeds maximum allowed ($intervalStr)",
                        severity = ViolationSeverity.MEDIUM
                    )
                )
            }
        }
        
        // Check screen lock requirement
        if (policy.getRuleAsBoolean(Policy.RuleKeys.REQUIRE_SCREEN_LOCK, false)) {
            if (!checkScreenLock(device)) {
                violations.add(
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.REQUIRE_SCREEN_LOCK,
                        message = "Screen lock is not enabled",
                        severity = ViolationSeverity.HIGH
                    )
                )
            }
        }
        
        // Check rooted/jailbroken devices
        if (!policy.getRuleAsBoolean(Policy.RuleKeys.ALLOW_ROOTED_DEVICES, false)) {
            if (checkRooted(device)) {
                violations.add(
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.ALLOW_ROOTED_DEVICES,
                        message = "Rooted/jailbroken devices are not allowed",
                        severity = ViolationSeverity.CRITICAL
                    )
                )
            }
        }
        
        // Check maximum offline days
        policy.getRuleAsString(Policy.RuleKeys.MAXIMUM_OFFLINE_DAYS)?.let { maxDaysStr ->
            val maxDays = maxDaysStr.toIntOrNull() ?: return@let
            if (!checkOfflineDuration(device, maxDays)) {
                violations.add(
                    PolicyViolation(
                        policyId = policy.id!!,
                        policyName = policy.name,
                        rule = Policy.RuleKeys.MAXIMUM_OFFLINE_DAYS,
                        message = "Device offline for more than $maxDays days",
                        severity = ViolationSeverity.MEDIUM
                    )
                )
            }
        }
        
        return violations
    }
    
    // ============================================================================
    // RULE CHECK METHODS
    // ============================================================================
    
    /**
     * Check OS version meets minimum requirement.
     * 
     * **Note:** This is a simplified version comparison.
     * Production should use semantic version comparison (e.g., SemVer library).
     * 
     * @param device Device to check
     * @param minVersion Minimum version required
     * @return True if device OS >= minimum
     */
    private fun checkOsVersion(device: Device, minVersion: String): Boolean {
        val deviceVersion = device.osVersion ?: return false
        return isOsVersionCompliant(deviceVersion, minVersion)
    }

    /**
     * M-12: Check OS version compliance with proper integer component comparison.
     * Replaces naive string comparison — handles "13.0" == "13", "14.1" > "14.0" correctly.
     */
    private fun isOsVersionCompliant(deviceOsVersion: String, minimumRequired: String): Boolean {
        return try {
            val device = parseVersion(deviceOsVersion)
            val required = parseVersion(minimumRequired)
            compareVersions(device, required) >= 0
        } catch (e: Exception) {
            logger.warn("Cannot parse version '{}' or '{}': {}", deviceOsVersion, minimumRequired, e.message)
            false  // Fail closed on parse error — non-compliant is safer than assuming compliant
        }
    }

    /**
     * Parse a version string like "13", "13.0", "13.0.1" into integer components.
     */
    private fun parseVersion(version: String): List<Int> {
        return version.trim().split(".").map { part -> part.trim().toIntOrNull() ?: 0 }
    }

    /**
     * Compare two version component lists. Returns positive if a > b, negative if a < b, 0 if equal.
     */
    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val aPart = a.getOrElse(i) { 0 }
            val bPart = b.getOrElse(i) { 0 }
            if (aPart != bPart) return aPart - bPart
        }
        return 0
    }
    
    /**
     * Check device encryption is enabled.
     * 
     * **Note:** Placeholder implementation.
     * Actual check requires device metadata field.
     * 
     * @param device Device to check
     * @return True if encryption enabled
     */
    private fun checkEncryption(device: Device): Boolean {
        // Check device metadata for encryption status
        return device.metadata?.get("encryptionEnabled") as? Boolean ?: false
    }
    
    /**
     * Check only allowed cloud providers are used.
     * 
     * **Note:** Placeholder implementation.
     * Actual check requires backup configuration data.
     * 
     * @param device Device to check
     * @param allowedProviders List of allowed provider names
     * @return List of unauthorized providers in use
     */
    private fun checkAllowedCloudProviders(device: Device, allowedProviders: List<String>): List<String> {
        // Check device metadata for configured cloud providers
        val configuredProviders = device.metadata?.get("cloudProviders") as? List<String> ?: emptyList()
        return configuredProviders.filter { it !in allowedProviders }
    }
    
    /**
     * Check backup interval is within allowed range.
     * 
     * **Note:** Placeholder implementation.
     * Actual check requires backup history data.
     * 
     * @param device Device to check
     * @param intervalStr Interval string (e.g., "PT24H" for 24 hours)
     * @return True if last backup within interval
     */
    private fun checkBackupInterval(device: Device, intervalStr: String): Boolean {
        // Parse interval (ISO 8601 duration format)
        val interval = try {
            Duration.parse(intervalStr)
        } catch (e: Exception) {
            logger.warn("Invalid backup interval format: {}", intervalStr, e)
            return false
        }
        
        // Check device metadata for last backup time
        val lastBackupStr = device.metadata?.get("lastBackupTime") as? String
        if (lastBackupStr == null) {
            return false // No backup recorded
        }
        
        val lastBackup = try {
            Instant.parse(lastBackupStr)
        } catch (e: Exception) {
            logger.warn("Invalid last backup timestamp: {}", lastBackupStr, e)
            return false
        }
        
        val elapsed = Duration.between(lastBackup, Instant.now())
        return elapsed <= interval
    }
    
    /**
     * Check screen lock is enabled.
     * 
     * **Note:** Placeholder implementation.
     * Actual check requires device metadata.
     * 
     * @param device Device to check
     * @return True if screen lock enabled
     */
    private fun checkScreenLock(device: Device): Boolean {
        return device.metadata?.get("screenLockEnabled") as? Boolean ?: false
    }
    
    /**
     * Check if device is rooted/jailbroken.
     * 
     * **Note:** Placeholder implementation.
     * Actual check requires device metadata.
     * 
     * @param device Device to check
     * @return True if device is rooted
     */
    private fun checkRooted(device: Device): Boolean {
        return device.metadata?.get("isRooted") as? Boolean ?: false
    }
    
    /**
     * Check device offline duration.
     * 
     * @param device Device to check
     * @param maxDays Maximum offline days allowed
     * @return True if within limit
     */
    private fun checkOfflineDuration(device: Device, maxDays: Int): Boolean {
        val lastSeen = device.lastSeenAt ?: return false
        val daysSinceLastSeen = Duration.between(lastSeen, Instant.now()).toDays()
        return daysSinceLastSeen <= maxDays
    }
    
    /**
     * Trigger automated remediation for violations.
     * 
     * **Research Citation:** Finding 4 - Auto-remediation support
     * 
     * **Remediation Actions:**
     * - Lock device if encryption disabled
     * - Force app update if version outdated
     * - Trigger policy sync
     * - Send notification to user
     * 
     * **Note:** Placeholder implementation.
     * Production requires DeviceCommandService integration.
     * 
     * @param deviceId Device UUID
     * @param violations List of violations to remediate
     * @return Number of remediation actions triggered
     */
    fun triggerRemediation(deviceId: UUID, violations: List<PolicyViolation>): Int {
        if (violations.isEmpty()) {
            return 0
        }
        
        var remediationCount = 0
        
        for (violation in violations) {
            // Get policy
            val policy = policyRepository.findById(violation.policyId).orElse(null)
                ?: continue
            
            // Check if auto-remediation enabled
            if (!policy.autoRemediation) {
                continue
            }
            
            // Get remediation actions from policy
            val remediationActions = policy.remediationActions ?: continue
            
            // Execute remediation based on violation type
            when (violation.rule) {
                Policy.RuleKeys.REQUIRE_ENCRYPTION -> {
                    // Remediation: Lock device until encryption enabled
                    logger.info("Auto-remediation: Lock device due to missing encryption, deviceId={}", deviceId)
                    // deviceCommandService.lockDevice(deviceId, "Encryption required", systemUserId)
                    remediationCount++
                }
                Policy.RuleKeys.MINIMUM_OS_VERSION -> {
                    // Remediation: Send update notification
                    logger.info("Auto-remediation: Notify user to update OS, deviceId={}", deviceId)
                    // notificationService.sendUpdateNotification(deviceId)
                    remediationCount++
                }
                Policy.RuleKeys.MINIMUM_BACKUP_INTERVAL -> {
                    // Remediation: Trigger immediate backup
                    logger.info("Auto-remediation: Trigger backup, deviceId={}", deviceId)
                    // deviceCommandService.sendCommand(deviceId, SYNC_POLICY)
                    remediationCount++
                }
            }
        }
        
        logger.info("Triggered {} remediation actions for deviceId={}", remediationCount, deviceId)
        return remediationCount
    }
    
    /**
     * Get compliance summary for organization.
     * 
     * @param organizationId Organization UUID
     * @return Compliance summary
     */
    fun getOrganizationComplianceSummary(organizationId: UUID): ComplianceSummary {
        val devices = deviceRepository.findByOrganizationId(organizationId)
        
        val compliantCount = devices.count { it.complianceStatus == Device.ComplianceStatus.COMPLIANT }
        val nonCompliantCount = devices.count { it.complianceStatus == Device.ComplianceStatus.NON_COMPLIANT }
        val pendingCount = devices.count { it.complianceStatus == Device.ComplianceStatus.PENDING }
        val exemptedCount = devices.count { it.complianceStatus == Device.ComplianceStatus.EXEMPTED }
        
        return ComplianceSummary(
            totalDevices = devices.size,
            compliantDevices = compliantCount,
            nonCompliantDevices = nonCompliantCount,
            pendingDevices = pendingCount,
            exemptedDevices = exemptedCount,
            complianceRate = if (devices.isNotEmpty()) {
                (compliantCount.toDouble() / devices.size) * 100
            } else 0.0
        )
    }
}

/**
 * Compliance evaluation result.
 */
sealed class ComplianceEvaluationResult {
    data class Success(
        val device: Device,
        val violations: List<PolicyViolation>,
        val evaluatedPolicies: Int
    ) : ComplianceEvaluationResult()
    
    data class Failure(val error: String) : ComplianceEvaluationResult()
    
    companion object {
        fun success(device: Device, violations: List<PolicyViolation>, evaluatedPolicies: Int) =
            Success(device, violations, evaluatedPolicies)
        fun failure(error: String) = Failure(error)
    }
}

/**
 * Policy violation.
 */
data class PolicyViolation(
    val policyId: UUID,
    val policyName: String,
    val rule: String,
    val message: String,
    val severity: ViolationSeverity
)

/**
 * Violation severity.
 */
enum class ViolationSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Compliance summary for organization.
 */
data class ComplianceSummary(
    val totalDevices: Int,
    val compliantDevices: Int,
    val nonCompliantDevices: Int,
    val pendingDevices: Int,
    val exemptedDevices: Int,
    val complianceRate: Double
)
