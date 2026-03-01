package com.obsidianbackup.enterprise.devices

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.obsidianbackup.enterprise.audit.AuditLogService
import com.obsidianbackup.enterprise.model.Device
import com.obsidianbackup.enterprise.model.DeviceCommand
import com.obsidianbackup.enterprise.repository.DeviceCommandRepository
import com.obsidianbackup.enterprise.repository.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Device Command Service for remote device management.
 * 
 * **Research Citations:**
 * - Finding 2: MDM REST API Architecture
 * - Finding 3: FCM High-Priority Push for critical commands
 * 
 * **Purpose:**
 * - Remote device lock/unlock
 * - Remote device wipe
 * - Policy synchronization
 * - Command retry logic
 * - FCM push notification delivery
 * 
 * **Command Flow:**
 * 1. Admin issues command (lock, wipe, sync)
 * 2. Create command record (status: PENDING)
 * 3. Send FCM high-priority notification
 * 4. Device receives notification, executes command
 * 5. Device reports completion
 * 6. Update command status (COMPLETED or FAILED)
 * 7. Log audit event
 * 
 * **FCM Integration:**
 * - High-priority messages for lock/wipe (Finding 3)
 * - Bypasses Doze mode for critical commands
 * - Retry logic for failed deliveries
 * - Delivery confirmation tracking
 * 
 * **Retry Logic:**
 * - Failed commands automatically retried (up to 3 times)
 * - Exponential backoff between retries
 * - Scheduled job processes retry queue
 */
@Service
@Transactional
class DeviceCommandService(
    private val deviceRepository: DeviceRepository,
    private val deviceCommandRepository: DeviceCommandRepository,
    private val auditLogService: AuditLogService,
    // Requires FirebaseApp to be initialized at startup. Add a @Bean of type FirebaseApp in a
    // @Configuration class using FirebaseApp.initializeApp(FirebaseOptions) with your
    // service account credentials. FirebaseMessaging.getInstance() is then Spring-injectable.
    private val firebaseMessaging: FirebaseMessaging,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(DeviceCommandService::class.java)
    
    /**
     * Send command to device.
     * 
     * **Generic command sender for all command types.**
     * 
     * @param deviceId Device UUID
     * @param commandType Command type
     * @param commandPayload Command payload (optional)
     * @param issuedBy User issuing command
     * @return Command result
     */
    fun sendCommand(
        deviceId: UUID,
        commandType: DeviceCommand.CommandType,
        commandPayload: Map<String, Any>? = null,
        issuedBy: UUID
    ): CommandResult {
        return try {
            // Get device
            val device = deviceRepository.findById(deviceId).orElse(null)
                ?: return CommandResult.failure("Device not found")
            
            // Check device can receive commands
            if (!device.canReceiveCommands()) {
                return CommandResult.failure("Device cannot receive commands (wiped or no FCM token)")
            }
            
            // Create command record
            val command = DeviceCommand(
                organizationId = device.organizationId,
                deviceId = deviceId,
                commandType = commandType,
                commandPayload = commandPayload,
                issuedBy = issuedBy
            )
            
            val savedCommand = deviceCommandRepository.save(command)
            
            // Send FCM notification
            val fcmResult = sendFcmNotification(device, savedCommand)
            
            if (fcmResult) {
                // Mark as queued/delivered
                savedCommand.markAsQueued()
                deviceCommandRepository.save(savedCommand)
                
                // Log audit event
                auditLogService.logDeviceCommand(
                    adminId = issuedBy,
                    organizationId = device.organizationId,
                    deviceId = deviceId,
                    command = commandType.name,
                    outcome = "SUCCESS",
                    details = commandPayload ?: emptyMap()
                )
                
                logger.info(
                    "Command sent: deviceId={}, type={}, commandId={}",
                    deviceId, commandType, savedCommand.id
                )
                
                CommandResult.success(savedCommand)
            } else {
                // FCM send failed
                savedCommand.markAsFailed("FCM notification delivery failed")
                deviceCommandRepository.save(savedCommand)
                
                logger.warn("Command FCM delivery failed: deviceId={}, type={}", deviceId, commandType)
                
                CommandResult.failure("Failed to send FCM notification")
            }
            
        } catch (e: Exception) {
            logger.error("Command send failed: deviceId={}, type={}", deviceId, commandType, e)
            CommandResult.failure("Command failed: ${e.message}")
        }
    }
    
    /**
     * Lock device remotely.
     * 
     * **Research Citation:** Finding 3 - High-priority FCM for critical commands
     * 
     * **Effect:**
     * - Device screen locks immediately
     * - User cannot access device until unlocked by admin
     * - Background sync continues
     * 
     * @param deviceId Device UUID
     * @param reason Lock reason (displayed to user)
     * @param issuedBy User issuing lock command
     * @return Command result
     */
    fun lockDevice(deviceId: UUID, reason: String, issuedBy: UUID): CommandResult {
        val payload = mapOf("reason" to reason)
        val result = sendCommand(deviceId, DeviceCommand.CommandType.LOCK, payload, issuedBy)
        
        // Update device lock status if command sent successfully
        if (result is CommandResult.Success) {
            deviceRepository.findById(deviceId).ifPresent { device ->
                device.lock()
                deviceRepository.save(device)
            }
        }
        
        return result
    }
    
    /**
     * Unlock device remotely.
     * 
     * @param deviceId Device UUID
     * @param issuedBy User issuing unlock command
     * @return Command result
     */
    fun unlockDevice(deviceId: UUID, issuedBy: UUID): CommandResult {
        val result = sendCommand(deviceId, DeviceCommand.CommandType.UNLOCK, null, issuedBy)
        
        // Update device lock status if command sent successfully
        if (result is CommandResult.Success) {
            deviceRepository.findById(deviceId).ifPresent { device ->
                device.unlock()
                deviceRepository.save(device)
            }
        }
        
        return result
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
     * @param deviceId Device UUID
     * @param reason Wipe reason (for audit trail)
     * @param issuedBy User issuing wipe command
     * @param ipAddress Admin IP address
     * @return Command result
     */
    fun wipeDevice(deviceId: UUID, reason: String, issuedBy: UUID, ipAddress: String?): CommandResult {
        val payload = mapOf("reason" to reason)
        val result = sendCommand(deviceId, DeviceCommand.CommandType.WIPE, payload, issuedBy)
        
        // Update device wipe status and log audit event
        if (result is CommandResult.Success) {
            deviceRepository.findById(deviceId).ifPresent { device ->
                device.markAsWiped()
                deviceRepository.save(device)
                
                // Log critical audit event
                auditLogService.logDeviceWipe(
                    adminId = issuedBy,
                    organizationId = device.organizationId,
                    deviceId = deviceId,
                    reason = reason,
                    ipAddress = ipAddress
                )
            }
        }
        
        return result
    }
    
    /**
     * Sync policy to device.
     * 
     * **Purpose:**
     * - Push updated policy settings to device
     * - Trigger compliance re-evaluation
     * - Update app configuration
     * 
     * @param deviceId Device UUID
     * @param policyId Policy UUID to sync
     * @param issuedBy User issuing sync command
     * @return Command result
     */
    fun syncPolicy(deviceId: UUID, policyId: UUID, issuedBy: UUID): CommandResult {
        val payload = mapOf("policyId" to policyId.toString())
        return sendCommand(deviceId, DeviceCommand.CommandType.SYNC_POLICY, payload, issuedBy)
    }
    
    /**
     * Send FCM high-priority notification to device via Firebase Admin SDK.
     *
     * High-priority (bypasses Doze): LOCK, WIPE, SYNC_POLICY
     * Normal priority: all other command types
     *
     * @param device Target device — must have a non-null fcmToken
     * @param command Command record to deliver
     * @return true if FCM accepted the message, false on any error
     */
    private fun sendFcmNotification(device: Device, command: DeviceCommand): Boolean {
        val fcmToken = device.fcmToken
        if (fcmToken == null) {
            logger.warn("Cannot send FCM: device has no token, deviceId={}", device.id)
            return false
        }

        val isHighPriority = command.commandType in listOf(
            DeviceCommand.CommandType.LOCK,
            DeviceCommand.CommandType.WIPE,
            DeviceCommand.CommandType.SYNC_POLICY
        )

        val payloadJson = command.commandPayload
            ?.let { runCatching { objectMapper.writeValueAsString(it) }.getOrDefault("{}") }
            ?: "{}"

        val message = Message.builder()
            .setToken(fcmToken)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(
                        if (isHighPriority) AndroidConfig.Priority.HIGH
                        else AndroidConfig.Priority.NORMAL
                    )
                    .setTtl(3_600_000L) // 1 hour TTL in milliseconds
                    .build()
            )
            .putData("commandId", command.id.toString())
            .putData("commandType", command.commandType.name)
            .putData("payload", payloadJson)
            .build()

        return try {
            val messageId = firebaseMessaging.send(message)
            logger.info(
                "FCM sent: deviceId={}, commandType={}, priority={}, messageId={}",
                device.id, command.commandType,
                if (isHighPriority) "HIGH" else "NORMAL",
                messageId
            )
            true
        } catch (e: FirebaseMessagingException) {
            when (e.messagingErrorCode?.name) {
                "INVALID_ARGUMENT", "UNREGISTERED" -> {
                    // Token is stale — clear it so we stop sending to it
                    logger.warn(
                        "FCM token rejected ({}): clearing token for deviceId={}. Error: {}",
                        e.messagingErrorCode, device.id, e.message
                    )
                    device.fcmToken = null
                    deviceRepository.save(device)
                }
                "QUOTA_EXCEEDED" -> {
                    logger.error(
                        "FCM quota exceeded for deviceId={}. Backing off. Error: {}",
                        device.id, e.message
                    )
                }
                else -> {
                    logger.error(
                        "FCM send failed for deviceId={}, commandType={}: {}",
                        device.id, command.commandType, e.message
                    )
                }
            }
            false
        } catch (e: Exception) {
            logger.error(
                "Unexpected error sending FCM for deviceId={}: {}",
                device.id, e.message, e
            )
            false
        }
    }
    
    /**
     * Mark command as delivered (called by device).
     * 
     * @param commandId Command UUID
     * @return True if updated successfully
     */
    fun markCommandDelivered(commandId: UUID): Boolean {
        return try {
            val command = deviceCommandRepository.findById(commandId).orElse(null)
                ?: return false
            
            command.markAsDelivered()
            deviceCommandRepository.save(command)
            
            logger.info("Command marked as delivered: commandId={}", commandId)
            true
        } catch (e: Exception) {
            logger.error("Failed to mark command as delivered: commandId={}", commandId, e)
            false
        }
    }
    
    /**
     * Mark command as completed (called by device after execution).
     * 
     * @param commandId Command UUID
     * @return True if updated successfully
     */
    fun markCommandCompleted(commandId: UUID): Boolean {
        return try {
            val command = deviceCommandRepository.findById(commandId).orElse(null)
                ?: return false
            
            command.markAsCompleted()
            deviceCommandRepository.save(command)
            
            logger.info("Command marked as completed: commandId={}", commandId)
            true
        } catch (e: Exception) {
            logger.error("Failed to mark command as completed: commandId={}", commandId, e)
            false
        }
    }
    
    /**
     * Mark command as failed (called by device if execution fails).
     * 
     * @param commandId Command UUID
     * @param error Error message
     * @return True if updated successfully
     */
    fun markCommandFailed(commandId: UUID, error: String): Boolean {
        return try {
            val command = deviceCommandRepository.findById(commandId).orElse(null)
                ?: return false
            
            command.markAsFailed(error)
            deviceCommandRepository.save(command)
            
            logger.warn("Command marked as failed: commandId={}, error={}", commandId, error)
            true
        } catch (e: Exception) {
            logger.error("Failed to mark command as failed: commandId={}", commandId, e)
            false
        }
    }
    
    /**
     * Retry failed commands.
     * 
     * **Scheduled Job:** Runs every 5 minutes
     * 
     * **Process:**
     * 1. Find failed commands with retries remaining
     * 2. Increment retry count
     * 3. Resend FCM notification
     * 4. Update status
     * 
     * @return Number of commands retried
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    fun retryFailedCommands(): Int {
        return try {
            // Find all organizations with failed commands
            val failedCommands = deviceCommandRepository.findAll()
                .filter { it.canRetry() }
            
            var retriedCount = 0
            
            for (command in failedCommands) {
                val device = deviceRepository.findById(command.deviceId).orElse(null)
                    ?: continue
                
                if (!device.canReceiveCommands()) {
                    continue
                }
                
                // Increment retry count
                if (!command.incrementRetryCount()) {
                    logger.warn("Max retries reached: commandId={}", command.id)
                    continue
                }
                
                // Reset status to pending
                command.status = DeviceCommand.CommandStatus.PENDING
                deviceCommandRepository.save(command)
                
                // Resend FCM
                val fcmResult = sendFcmNotification(device, command)
                
                if (fcmResult) {
                    command.markAsQueued()
                    deviceCommandRepository.save(command)
                    retriedCount++
                    logger.info("Command retried: commandId={}, attempt={}", command.id, command.retryCount)
                }
            }
            
            if (retriedCount > 0) {
                logger.info("Retried {} failed commands", retriedCount)
            }
            
            retriedCount
            
        } catch (e: Exception) {
            logger.error("Failed command retry job error", e)
            0
        }
    }
    
    /**
     * Cleanup stale commands (pending for >24 hours).
     * 
     * **Scheduled Job:** Runs daily at 3 AM
     * 
     * @return Number of commands cleaned up
     */
    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupStaleCommands(): Int {
        return try {
            val staleThreshold = Instant.now().minus(24, ChronoUnit.HOURS)
            
            // Find all stale commands across all organizations
            val staleCommands = deviceCommandRepository.findAll()
                .filter { command ->
                    !command.isFinalState() && command.issuedAt.isBefore(staleThreshold)
                }
            
            for (command in staleCommands) {
                command.markAsFailed("Command timed out (>24 hours)")
                deviceCommandRepository.save(command)
            }
            
            if (staleCommands.isNotEmpty()) {
                logger.info("Cleaned up {} stale commands", staleCommands.size)
            }
            
            staleCommands.size
            
        } catch (e: Exception) {
            logger.error("Stale command cleanup job error", e)
            0
        }
    }
    
    /**
     * Get command by ID.
     * 
     * @param commandId Command UUID
     * @return Command or null
     */
    fun getCommand(commandId: UUID): DeviceCommand? {
        return deviceCommandRepository.findById(commandId).orElse(null)
    }
    
    /**
     * Get commands for device.
     * 
     * @param deviceId Device UUID
     * @return List of commands
     */
    fun getDeviceCommands(deviceId: UUID): List<DeviceCommand> {
        return deviceCommandRepository.findByDeviceIdOrderByIssuedAtDesc(deviceId)
    }
    
    /**
     * Get pending commands for device.
     * 
     * @param deviceId Device UUID
     * @return List of pending commands
     */
    fun getPendingCommands(deviceId: UUID): List<DeviceCommand> {
        return deviceCommandRepository.findPendingCommandsForDevice(deviceId)
    }
}

/**
 * Command execution result.
 */
sealed class CommandResult {
    data class Success(val command: DeviceCommand) : CommandResult()
    data class Failure(val error: String) : CommandResult()
    
    companion object {
        fun success(command: DeviceCommand) = Success(command)
        fun failure(error: String) = Failure(error)
    }
}
