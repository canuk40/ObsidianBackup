package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.database.AuditLogs
import com.obsidianbackup.enterprise.database.Users
import com.obsidianbackup.enterprise.models.AuditLog
import com.obsidianbackup.enterprise.routes.AuditQueryParams
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class AuditService {
    
    fun log(
        userId: String,
        action: String,
        resourceType: String,
        resourceId: String,
        status: String,
        details: Map<String, String>,
        ipAddress: String,
        userAgent: String,
        deviceId: String? = null
    ) {
        transaction {
            AuditLogs.insert {
                it[timestamp] = Instant.now()
                it[AuditLogs.userId] = UUID.fromString(userId)
                it[AuditLogs.deviceId] = deviceId?.let { id -> UUID.fromString(id) }
                it[AuditLogs.action] = action
                it[AuditLogs.resourceType] = resourceType
                it[AuditLogs.resourceId] = resourceId
                it[AuditLogs.status] = status
                it[AuditLogs.details] = Json.encodeToString(details)
                it[AuditLogs.ipAddress] = ipAddress
                it[AuditLogs.userAgent] = userAgent
            }
        }
    }
    
    fun queryLogs(params: AuditQueryParams): List<AuditLog> {
        return transaction {
            val query = AuditLogs.selectAll()
            
            // Apply filters
            params.userId?.let { query.andWhere { AuditLogs.userId eq UUID.fromString(it) } }
            params.deviceId?.let { query.andWhere { AuditLogs.deviceId eq UUID.fromString(it) } }
            params.action?.let { query.andWhere { AuditLogs.action eq it } }
            
            // Pagination
            val offset = (params.page - 1) * params.pageSize
            query.limit(params.pageSize, offset.toLong())
                .orderBy(AuditLogs.timestamp, SortOrder.DESC)
                .map { rowToAuditLog(it) }
        }
    }
    
    fun getLog(logId: String): AuditLog? {
        return transaction {
            AuditLogs.select { AuditLogs.id eq UUID.fromString(logId) }
                .singleOrNull()
                ?.let { rowToAuditLog(it) }
        }
    }
    
    fun exportLogs(params: AuditQueryParams): String {
        val logs = queryLogs(params)
        val csv = StringBuilder()
        
        csv.appendLine("Timestamp,User ID,Device ID,Action,Resource Type,Resource ID,Status,IP Address")
        logs.forEach { log ->
            csv.appendLine("${log.timestamp},${log.userId},${log.deviceId ?: ""},${log.action},${log.resourceType},${log.resourceId},${log.status},${log.ipAddress}")
        }
        
        return csv.toString()
    }
    
    private fun rowToAuditLog(row: ResultRow): AuditLog {
        return AuditLog(
            id = row[AuditLogs.id].value.toString(),
            timestamp = row[AuditLogs.timestamp].toString(),
            userId = row[AuditLogs.userId].value.toString(),
            deviceId = row[AuditLogs.deviceId]?.value?.toString(),
            action = row[AuditLogs.action],
            resourceType = row[AuditLogs.resourceType],
            resourceId = row[AuditLogs.resourceId],
            status = row[AuditLogs.status],
            details = Json.decodeFromString(row[AuditLogs.details]),
            ipAddress = row[AuditLogs.ipAddress],
            userAgent = row[AuditLogs.userAgent]
        )
    }
}
