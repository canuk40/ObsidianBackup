package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.database.*
import com.obsidianbackup.enterprise.models.*
import com.obsidianbackup.enterprise.routes.DeviceRegistrationRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class DeviceService {
    private val auditService = AuditService()
    
    fun getDevicesByUser(userId: String): List<Device> {
        return transaction {
            Devices.select { Devices.userId eq UUID.fromString(userId) }
                .map { rowToDevice(it) }
        }
    }
    
    fun getDevice(deviceId: String): Device? {
        return transaction {
            Devices.select { Devices.id eq UUID.fromString(deviceId) }
                .singleOrNull()
                ?.let { rowToDevice(it) }
        }
    }
    
    fun registerDevice(userId: String, request: DeviceRegistrationRequest): Device {
        return transaction {
            val userUUID = UUID.fromString(userId)
            val user = Users.select { Users.id eq userUUID }.single()
            val orgId = user[Users.organizationId]
            
            val deviceId = Devices.insertAndGetId {
                it[name] = request.name
                it[Devices.userId] = userUUID
                it[organizationId] = orgId
                it[platform] = request.platform
                it[osVersion] = request.osVersion
                it[appVersion] = request.appVersion
                it[status] = DeviceStatus.ACTIVE.name
                it[complianceStatus] = Json.encodeToString(ComplianceStatus(true, emptyList(), Instant.now().toString()))
                it[registeredAt] = Instant.now()
            }
            
            auditService.log(
                userId = userId,
                deviceId = deviceId.value.toString(),
                action = "DEVICE_REGISTERED",
                resourceType = "Device",
                resourceId = deviceId.value.toString(),
                status = "SUCCESS",
                details = mapOf("deviceName" to request.name),
                ipAddress = "0.0.0.0",
                userAgent = "API"
            )
            
            getDevice(deviceId.value.toString())!!
        }
    }
    
    fun wipeDevice(deviceId: String, reason: String) {
        transaction {
            Devices.update({ Devices.id eq UUID.fromString(deviceId) }) {
                it[status] = DeviceStatus.WIPED.name
            }
            
            auditService.log(
                userId = "SYSTEM",
                deviceId = deviceId,
                action = "DEVICE_WIPED",
                resourceType = "Device",
                resourceId = deviceId,
                status = "SUCCESS",
                details = mapOf("reason" to reason),
                ipAddress = "0.0.0.0",
                userAgent = "ADMIN_CONSOLE"
            )
        }
    }
    
    fun checkCompliance(deviceId: String): ComplianceStatus {
        return transaction {
            val device = Devices.select { Devices.id eq UUID.fromString(deviceId) }.single()
            Json.decodeFromString(device[Devices.complianceStatus])
        }
    }
    
    private fun rowToDevice(row: ResultRow): Device {
        val policies = DevicePolicies
            .select { DevicePolicies.deviceId eq row[Devices.id].value }
            .map { it[DevicePolicies.policyId].value.toString() }
        
        return Device(
            id = row[Devices.id].value.toString(),
            name = row[Devices.name],
            userId = row[Devices.userId].value.toString(),
            organizationId = row[Devices.organizationId].value.toString(),
            platform = row[Devices.platform],
            osVersion = row[Devices.osVersion],
            appVersion = row[Devices.appVersion],
            lastSyncAt = row[Devices.lastSyncAt]?.toString(),
            status = DeviceStatus.valueOf(row[Devices.status]),
            policies = policies,
            complianceStatus = Json.decodeFromString(row[Devices.complianceStatus]),
            registeredAt = row[Devices.registeredAt].toString()
        )
    }
}
