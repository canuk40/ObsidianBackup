package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * Policy entity for compliance and security enforcement.
 * 
 * **Research Citation:** Finding 4 - Policy Enforcement Engine
 * 
 * **Table:** policies
 * **Schema:** V1__initial_schema.sql (lines 125-143)
 * 
 * **Purpose:**
 * - Define security and compliance requirements
 * - Automated compliance evaluation
 * - Policy-based device management
 * - Multi-framework compliance (HIPAA, SOC 2, GDPR)
 * 
 * **Policy Types:**
 * - SECURITY: OS version, encryption, screen lock
 * - COMPLIANCE: Backup frequency, data retention, audit logging
 * - OPERATIONAL: App version, network restrictions, VPN
 * - CUSTOM: Organization-specific rules
 * 
 * **Rules Structure (JSONB):**
 * ```json
 * {
 *   "minimumOsVersion": "13.0",
 *   "requireEncryption": true,
 *   "allowedCloudProviders": ["gdrive", "dropbox"],
 *   "minimumBackupInterval": "PT24H",
 *   "requireScreenLock": true,
 *   "allowedNetworks": ["wifi"],
 *   "customRules": {...}
 * }
 * ```
 * 
 * **Relationships:**
 * - ManyToOne: Organization (tenant)
 * - ManyToOne: User (createdBy)
 * - ManyToMany: Devices (via device_policies)
 * - OneToMany: PolicyEvaluations (compliance history)
 */
@Entity
@Table(
    name = "policies",
    indexes = [
        Index(name = "idx_policies_organization", columnList = "organization_id"),
        Index(name = "idx_policies_enabled", columnList = "enabled"),
        Index(name = "idx_policies_type", columnList = "policy_type"),
        Index(name = "idx_policies_priority", columnList = "priority")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "unq_policy_name_org", columnNames = ["organization_id", "name"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
data class Policy(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    
    @Column(name = "name", nullable = false, length = 255)
    @field:NotBlank(message = "Policy name is required")
    @field:Size(min = 2, max = 255, message = "Policy name must be between 2 and 255 characters")
    val name: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 50)
    val policyType: PolicyType,
    
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
    
    @Column(name = "priority", nullable = false)
    @field:Min(value = 0, message = "Priority must be non-negative")
    val priority: Int = 0,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", nullable = false, columnDefinition = "jsonb")
    val rules: Map<String, Any>,
    
    @Column(name = "compliance_frameworks", columnDefinition = "varchar(100)[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    val complianceFrameworks: Array<String>? = null,
    
    @Column(name = "auto_remediation", nullable = false)
    val autoRemediation: Boolean = false,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "remediation_actions", columnDefinition = "jsonb")
    val remediationActions: Map<String, Any>? = null,
    
    @Column(name = "created_by")
    val createdBy: UUID? = null,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * Policy type enum.
     * Matches database constraint: CHECK (policy_type IN ('SECURITY', 'COMPLIANCE', 'OPERATIONAL', 'CUSTOM'))
     */
    enum class PolicyType {
        /**
         * Security: Device security settings (encryption, OS version, screen lock)
         */
        SECURITY,
        
        /**
         * Compliance: Regulatory compliance (HIPAA, SOC 2, GDPR)
         */
        COMPLIANCE,
        
        /**
         * Operational: Operational requirements (backup frequency, app version)
         */
        OPERATIONAL,
        
        /**
         * Custom: Organization-specific rules
         */
        CUSTOM
    }
    
    /**
     * Common rule keys.
     * Used in rules JSONB field.
     */
    object RuleKeys {
        const val MINIMUM_OS_VERSION = "minimumOsVersion"
        const val REQUIRE_ENCRYPTION = "requireEncryption"
        const val ALLOWED_CLOUD_PROVIDERS = "allowedCloudProviders"
        const val MINIMUM_BACKUP_INTERVAL = "minimumBackupInterval"
        const val REQUIRE_SCREEN_LOCK = "requireScreenLock"
        const val ALLOWED_NETWORKS = "allowedNetworks"
        const val REQUIRE_VPN = "requireVpn"
        const val ALLOW_ROOTED_DEVICES = "allowRootedDevices"
        const val MAXIMUM_OFFLINE_DAYS = "maximumOfflineDays"
        const val REQUIRE_APP_VERSION = "requireAppVersion"
    }
    
    /**
     * Compliance framework constants.
     */
    object ComplianceFramework {
        const val HIPAA = "HIPAA"
        const val SOC2 = "SOC2"
        const val GDPR = "GDPR"
        const val PCI_DSS = "PCI_DSS"
        const val ISO_27001 = "ISO_27001"
    }
    
    /**
     * Get rule value by key.
     * 
     * @param key Rule key
     * @return Rule value or null
     */
    fun getRule(key: String): Any? = rules[key]
    
    /**
     * Get rule as string.
     * 
     * @param key Rule key
     * @return String value or null
     */
    fun getRuleAsString(key: String): String? = rules[key] as? String
    
    /**
     * Get rule as boolean.
     * 
     * @param key Rule key
     * @param defaultValue Default if not found
     * @return Boolean value
     */
    fun getRuleAsBoolean(key: String, defaultValue: Boolean = false): Boolean =
        rules[key] as? Boolean ?: defaultValue
    
    /**
     * Get rule as list.
     * 
     * @param key Rule key
     * @return List value or empty list
     */
    @Suppress("UNCHECKED_CAST")
    fun getRuleAsList(key: String): List<String> =
        rules[key] as? List<String> ?: emptyList()
    
    /**
     * Check if policy has compliance framework.
     * 
     * @param framework Framework name
     * @return True if policy tagged with framework
     */
    fun hasComplianceFramework(framework: String): Boolean =
        complianceFrameworks?.contains(framework) ?: false
    
    /**
     * Check if policy is active.
     */
    fun isActive(): Boolean = enabled
    
    /**
     * Enable policy.
     */
    fun enable() {
        enabled = true
    }
    
    /**
     * Disable policy.
     */
    fun disable() {
        enabled = false
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Policy) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Policy(id=$id, name='$name', type=$policyType, priority=$priority, enabled=$enabled)"
    }
}
