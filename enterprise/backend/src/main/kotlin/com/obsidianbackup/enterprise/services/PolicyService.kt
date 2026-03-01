package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.database.*
import com.obsidianbackup.enterprise.models.*
import com.obsidianbackup.enterprise.routes.PolicyCreateRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class PolicyService {
    private val auditService = AuditService()
    
    fun getAllPolicies(): List<Policy> {
        return transaction {
            Policies.selectAll().map { rowToPolicy(it) }
        }
    }
    
    fun getPolicy(policyId: String): Policy? {
        return transaction {
            Policies.select { Policies.id eq UUID.fromString(policyId) }
                .singleOrNull()
                ?.let { rowToPolicy(it) }
        }
    }
    
    fun createPolicy(request: PolicyCreateRequest): Policy {
        return transaction {
            val orgId = Organizations.selectAll().limit(1).first()[Organizations.id].value
            
            val policyId = Policies.insertAndGetId {
                it[name] = request.name
                it[organizationId] = orgId
                it[type] = request.type
                it[config] = Json.encodeToString(request.config)
                it[isEnforced] = true
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
            
            // Assign to target devices
            request.targetDevices.forEach { deviceId ->
                DevicePolicies.insert {
                    it[DevicePolicies.deviceId] = UUID.fromString(deviceId)
                    it[DevicePolicies.policyId] = policyId.value
                }
            }
            
            auditService.log(
                userId = "ADMIN",
                action = "POLICY_CREATED",
                resourceType = "Policy",
                resourceId = policyId.value.toString(),
                status = "SUCCESS",
                details = mapOf("policyName" to request.name, "type" to request.type),
                ipAddress = "0.0.0.0",
                userAgent = "API"
            )
            
            getPolicy(policyId.value.toString())!!
        }
    }
    
    fun updatePolicy(policyId: String, request: PolicyCreateRequest): Policy? {
        return transaction {
            val uuid = UUID.fromString(policyId)
            Policies.update({ Policies.id eq uuid }) {
                it[name] = request.name
                it[type] = request.type
                it[config] = Json.encodeToString(request.config)
                it[updatedAt] = Instant.now()
            }
            
            // Update device assignments
            DevicePolicies.deleteWhere { DevicePolicies.policyId eq uuid }
            request.targetDevices.forEach { deviceId ->
                DevicePolicies.insert {
                    it[DevicePolicies.deviceId] = UUID.fromString(deviceId)
                    it[DevicePolicies.policyId] = uuid
                }
            }
            
            getPolicy(policyId)
        }
    }
    
    fun deletePolicy(policyId: String) {
        transaction {
            val uuid = UUID.fromString(policyId)
            DevicePolicies.deleteWhere { DevicePolicies.policyId eq uuid }
            Policies.deleteWhere { Policies.id eq uuid }
        }
    }
    
    fun enforcePolicy(policyId: String) {
        transaction {
            Policies.update({ Policies.id eq UUID.fromString(policyId) }) {
                it[isEnforced] = true
                it[updatedAt] = Instant.now()
            }
            
            auditService.log(
                userId = "ADMIN",
                action = "POLICY_ENFORCED",
                resourceType = "Policy",
                resourceId = policyId,
                status = "SUCCESS",
                details = emptyMap(),
                ipAddress = "0.0.0.0",
                userAgent = "API"
            )
        }
    }
    
    private fun rowToPolicy(row: ResultRow): Policy {
        val targetDevices = DevicePolicies
            .select { DevicePolicies.policyId eq row[Policies.id].value }
            .map { it[DevicePolicies.deviceId].value.toString() }
        
        return Policy(
            id = row[Policies.id].value.toString(),
            name = row[Policies.name],
            organizationId = row[Policies.organizationId].value.toString(),
            type = PolicyType.valueOf(row[Policies.type]),
            config = Json.decodeFromString(row[Policies.config]),
            targetDevices = targetDevices,
            isEnforced = row[Policies.isEnforced],
            createdAt = row[Policies.createdAt].toString(),
            updatedAt = row[Policies.updatedAt].toString()
        )
    }
}
