package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * Organization entity for multi-tenant management.
 * 
 * **Table:** organizations
 * **Schema:** V1__initial_schema.sql (lines 13-25)
 * 
 * **Purpose:**
 * - Multi-tenant root entity
 * - Subscription tier management
 * - Resource quotas (devices, users, policies)
 * - SAML SSO configuration (enterprise)
 * 
 * **Plan Types:**
 * - FREE: Basic features, limited devices
 * - PROFESSIONAL: Advanced features, higher quotas
 * - ENTERPRISE: Full features, unlimited resources, SAML SSO
 * 
 * **Relationships:**
 * - OneToMany: Users (organization members)
 * - OneToMany: Devices (enrolled devices)
 * - OneToMany: Policies (security policies)
 * - OneToMany: AuditLogs (audit trail)
 * 
 * **Auditing:**
 * - Uses Spring Data JPA auditing for createdAt/updatedAt
 * - Requires @EnableJpaAuditing in configuration
 */
@Entity
@Table(
    name = "organizations",
    indexes = [
        Index(name = "idx_organizations_slug", columnList = "slug"),
        Index(name = "idx_organizations_enabled", columnList = "enabled")
    ]
)
@EntityListeners(AuditingEntityListener::class)
data class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "name", nullable = false, length = 255)
    @field:NotBlank(message = "Organization name is required")
    @field:Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    val name: String,
    
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    @field:NotBlank(message = "Organization slug is required")
    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Slug must contain only lowercase letters, numbers, and hyphens"
    )
    @field:Size(min = 2, max = 100, message = "Slug must be between 2 and 100 characters")
    val slug: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 50)
    val planType: PlanType = PlanType.FREE,
    
    @Column(name = "max_devices", nullable = false)
    @field:Min(value = 1, message = "Max devices must be at least 1")
    @field:Max(value = 999999, message = "Max devices cannot exceed 999999")
    val maxDevices: Int = 100,
    
    @Column(name = "max_users", nullable = false)
    @field:Min(value = 1, message = "Max users must be at least 1")
    @field:Max(value = 999999, message = "Max users cannot exceed 999999")
    val maxUsers: Int = 10,
    
    @Column(name = "enabled", nullable = false)
    val enabled: Boolean = true,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * Plan type enum.
     * Matches database constraint: CHECK (plan_type IN ('FREE', 'PROFESSIONAL', 'ENTERPRISE'))
     */
    enum class PlanType {
        /**
         * Free tier:
         * - Up to 100 devices
         * - Up to 10 users
         * - Basic backup features
         * - No SAML SSO
         */
        FREE,
        
        /**
         * Professional tier:
         * - Up to 500 devices
         * - Up to 50 users
         * - Advanced backup features
         * - Cloud providers
         * - Priority support
         */
        PROFESSIONAL,
        
        /**
         * Enterprise tier:
         * - Unlimited devices
         * - Unlimited users
         * - Full feature access
         * - SAML SSO
         * - Dedicated support
         * - Custom SLAs
         */
        ENTERPRISE
    }
    
    /**
     * Check if organization can add more devices.
     * 
     * @param currentDeviceCount Current number of devices
     * @return True if under quota
     */
    fun canAddDevice(currentDeviceCount: Int): Boolean {
        return currentDeviceCount < maxDevices
    }
    
    /**
     * Check if organization can add more users.
     * 
     * @param currentUserCount Current number of users
     * @return True if under quota
     */
    fun canAddUser(currentUserCount: Int): Boolean {
        return currentUserCount < maxUsers
    }
    
    /**
     * Check if organization has enterprise features.
     * 
     * @return True if plan type is ENTERPRISE
     */
    fun hasEnterpriseFeatures(): Boolean {
        return planType == PlanType.ENTERPRISE
    }
    
    /**
     * Check if organization is active.
     * 
     * @return True if enabled
     */
    fun isActive(): Boolean {
        return enabled
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Organization) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Organization(id=$id, name='$name', slug='$slug', planType=$planType, enabled=$enabled)"
    }
}
