package com.obsidianbackup.enterprise.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String>,
    val organizationId: String,
    val isActive: Boolean = true,
    val createdAt: String,
    val lastLoginAt: String? = null
)

@Serializable
data class Role(
    val id: String,
    val name: String,
    val permissions: List<String>,
    val organizationId: String,
    val description: String? = null
)

@Serializable
data class Device(
    val id: String,
    val name: String,
    val userId: String,
    val organizationId: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String,
    val lastSyncAt: String?,
    val status: DeviceStatus,
    val policies: List<String>,
    val complianceStatus: ComplianceStatus,
    val registeredAt: String
)

@Serializable
enum class DeviceStatus {
    ACTIVE, INACTIVE, WIPED, SUSPENDED
}

@Serializable
data class ComplianceStatus(
    val isCompliant: Boolean,
    val violations: List<String>,
    val lastCheckAt: String
)

@Serializable
data class Policy(
    val id: String,
    val name: String,
    val organizationId: String,
    val type: PolicyType,
    val config: Map<String, String>,
    val targetDevices: List<String>,
    val isEnforced: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
enum class PolicyType {
    BACKUP_SCHEDULE, 
    RETENTION, 
    ENCRYPTION, 
    NETWORK, 
    COMPLIANCE
}

@Serializable
data class AuditLog(
    val id: String,
    val timestamp: String,
    val userId: String,
    val deviceId: String? = null,
    val action: String,
    val resourceType: String,
    val resourceId: String,
    val status: String,
    val details: Map<String, String>,
    val ipAddress: String,
    val userAgent: String
)

@Serializable
data class Organization(
    val id: String,
    val name: String,
    val domain: String,
    val plan: String,
    val maxDevices: Int,
    val features: List<String>,
    val samlConfig: SamlConfig? = null,
    val createdAt: String
)

@Serializable
data class SamlConfig(
    val entityId: String,
    val ssoUrl: String,
    val certificate: String,
    val attributeMapping: Map<String, String>
)

@Serializable
data class BackupReport(
    val deviceId: String,
    val backupId: String,
    val startedAt: String,
    val completedAt: String?,
    val status: BackupStatus,
    val bytesBackedUp: Long,
    val filesBackedUp: Int,
    val errorMessage: String? = null
)

@Serializable
enum class BackupStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}
