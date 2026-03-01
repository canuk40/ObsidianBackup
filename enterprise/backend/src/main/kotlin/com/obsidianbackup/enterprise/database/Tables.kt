package com.obsidianbackup.enterprise.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Organizations : UUIDTable("organizations") {
    val name = varchar("name", 255)
    val domain = varchar("domain", 255)
    val plan = varchar("plan", 50)
    val maxDevices = integer("max_devices")
    val features = text("features") // JSON array
    val samlConfig = text("saml_config").nullable() // JSON
    val createdAt = timestamp("created_at")
}

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255).nullable()
    val organizationId = uuid("organization_id").references(Organizations.id)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val lastLoginAt = timestamp("last_login_at").nullable()
}

object Roles : UUIDTable("roles") {
    val name = varchar("name", 100)
    val permissions = text("permissions") // JSON array
    val organizationId = uuid("organization_id").references(Organizations.id)
    val description = text("description").nullable()
}

object UserRoles : Table("user_roles") {
    val userId = uuid("user_id").references(Users.id)
    val roleId = uuid("role_id").references(Roles.id)
    override val primaryKey = PrimaryKey(userId, roleId)
}

object Devices : UUIDTable("devices") {
    val name = varchar("name", 255)
    val userId = uuid("user_id").references(Users.id)
    val organizationId = uuid("organization_id").references(Organizations.id)
    val platform = varchar("platform", 50)
    val osVersion = varchar("os_version", 50)
    val appVersion = varchar("app_version", 50)
    val lastSyncAt = timestamp("last_sync_at").nullable()
    val status = varchar("status", 50)
    val complianceStatus = text("compliance_status") // JSON
    val registeredAt = timestamp("registered_at")
}

object Policies : UUIDTable("policies") {
    val name = varchar("name", 255)
    val organizationId = uuid("organization_id").references(Organizations.id)
    val type = varchar("type", 50)
    val config = text("config") // JSON
    val isEnforced = bool("is_enforced").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object DevicePolicies : Table("device_policies") {
    val deviceId = uuid("device_id").references(Devices.id)
    val policyId = uuid("policy_id").references(Policies.id)
    override val primaryKey = PrimaryKey(deviceId, policyId)
}

object AuditLogs : UUIDTable("audit_logs") {
    val timestamp = timestamp("timestamp")
    val userId = uuid("user_id").references(Users.id)
    val deviceId = uuid("device_id").references(Devices.id).nullable()
    val action = varchar("action", 100)
    val resourceType = varchar("resource_type", 100)
    val resourceId = varchar("resource_id", 255)
    val status = varchar("status", 50)
    val details = text("details") // JSON
    val ipAddress = varchar("ip_address", 45)
    val userAgent = text("user_agent")
}

object BackupReports : UUIDTable("backup_reports") {
    val deviceId = uuid("device_id").references(Devices.id)
    val backupId = varchar("backup_id", 255)
    val startedAt = timestamp("started_at")
    val completedAt = timestamp("completed_at").nullable()
    val status = varchar("status", 50)
    val bytesBackedUp = long("bytes_backed_up")
    val filesBackedUp = integer("files_backed_up")
    val errorMessage = text("error_message").nullable()
}
