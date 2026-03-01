package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * User entity for authentication and authorization.
 * 
 * **Table:** users
 * **Schema:** V1__initial_schema.sql (lines 34-56)
 * 
 * **Purpose:**
 * - User account management
 * - Spring Security integration (implements UserDetails)
 * - Multi-factor authentication support
 * - Organization membership
 * - Role-based access control
 * 
 * **Roles:**
 * - SUPER_ADMIN: System-wide administration
 * - ADMIN: Organization administration
 * - DEVICE_MANAGER: Device management only
 * - VIEWER: Read-only access
 * 
 * **Authentication:**
 * - Password-based (BCrypt hashed)
 * - SAML SSO (passwordHash nullable for SAML-only users)
 * - MFA support (TOTP)
 * 
 * **Relationships:**
 * - ManyToOne: Organization (tenant)
 * - OneToMany: RefreshTokens (session management)
 * - OneToMany: AuditLogs (activity tracking)
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_organization", columnList = "organization_id"),
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_enabled", columnList = "enabled")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "unq_user_email_org", columnNames = ["organization_id", "email"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    
    @Column(name = "email", nullable = false, length = 255)
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    private val email: String,
    
    @Column(name = "username", nullable = false, length = 100)
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username must contain only letters, numbers, underscores, and hyphens"
    )
    private val username: String,
    
    @Column(name = "password_hash", length = 255)
    private val passwordHash: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    val role: Role = Role.VIEWER,
    
    @Column(name = "enabled", nullable = false)
    private val enabled: Boolean = true,
    
    @Column(name = "mfa_enabled", nullable = false)
    val mfaEnabled: Boolean = false,
    
    @Column(name = "mfa_secret", length = 255)
    val mfaSecret: String? = null,
    
    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,
    
    @Column(name = "last_login_ip", columnDefinition = "inet")
    var lastLoginIp: String? = null,

    // M-11: Real account locking and password expiration fields
    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int = 0,

    @Column(name = "account_locked_until")
    val accountLockedUntil: Instant? = null,

    @Column(name = "email_verified", nullable = false)
    val emailVerified: Boolean = false,

    @Column(name = "password_changed_at", nullable = false)
    val passwordChangedAt: Instant = Instant.now(),
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) : UserDetails {
    
    /**
     * Role enum.
     * Matches database constraint: CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'DEVICE_MANAGER', 'VIEWER'))
     */
    enum class Role(val displayName: String) {
        /**
         * Super admin: System-wide administration.
         * Can manage all organizations and system configuration.
         */
        SUPER_ADMIN("Super Administrator"),
        
        /**
         * Admin: Organization administration.
         * Can manage organization settings, users, devices, and policies.
         */
        ADMIN("Administrator"),
        
        /**
         * Device manager: Device management only.
         * Can enroll devices, issue commands, view policies.
         */
        DEVICE_MANAGER("Device Manager"),
        
        /**
         * Viewer: Read-only access.
         * Can view devices, policies, and audit logs.
         */
        VIEWER("Viewer");
        
        /**
         * Check if role has admin privileges.
         */
        fun isAdmin(): Boolean = this == SUPER_ADMIN || this == ADMIN
        
        /**
         * Check if role can manage devices.
         */
        fun canManageDevices(): Boolean = this != VIEWER
        
        /**
         * Check if role can manage users.
         */
        fun canManageUsers(): Boolean = isAdmin()
        
        /**
         * Check if role can manage policies.
         */
        fun canManagePolicies(): Boolean = isAdmin()
    }
    
    // ============================================================================
    // UserDetails Implementation (Spring Security)
    // ============================================================================

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val PASSWORD_EXPIRY_DAYS = 90L
        val LOCKOUT_DURATION: Duration = Duration.ofMinutes(30)
    }
    
    /**
     * Returns the authorities granted to the user.
     * Maps role to Spring Security GrantedAuthority.
     */
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }
    
    /**
     * Returns the password used to authenticate the user.
     */
    override fun getPassword(): String? = passwordHash
    
    /**
     * Returns the username used to authenticate the user.
     */
    override fun getUsername(): String = username
    
    /**
     * Returns the email address (for Spring Security).
     */
    fun getEmail(): String = email
    
    /**
     * Indicates whether the user's account has expired.
     * Always returns true (accounts don't expire).
     */
    override fun isAccountNonExpired(): Boolean = true
    
    /**
     * M-11: Real account locking — checks accountLockedUntil timestamp.
     * Previously: Always returned true (locking not implemented).
     */
    override fun isAccountNonLocked(): Boolean {
        if (accountLockedUntil == null) return true
        return Instant.now().isAfter(accountLockedUntil)
    }
    
    /**
     * M-11: Real password expiration — 90-day policy.
     * Previously: Always returned true (password expiration not implemented).
     */
    override fun isCredentialsNonExpired(): Boolean {
        val expiryDate = passwordChangedAt.plus(PASSWORD_EXPIRY_DAYS, ChronoUnit.DAYS)
        return Instant.now().isBefore(expiryDate)
    }
    
    /**
     * Indicates whether the user is enabled or disabled.
     */
    override fun isEnabled(): Boolean = enabled
    
    // ============================================================================
    // Business Methods
    // ============================================================================
    
    /**
     * Check if user has admin role.
     */
    fun isAdmin(): Boolean = role.isAdmin()
    
    /**
     * Check if user can manage devices.
     */
    fun canManageDevices(): Boolean = role.canManageDevices()
    
    /**
     * Check if user can manage other users.
     */
    fun canManageUsers(): Boolean = role.canManageUsers()
    
    /**
     * Check if user can manage policies.
     */
    fun canManagePolicies(): Boolean = role.canManagePolicies()
    
    /**
     * Check if user requires password (SAML users don't).
     */
    fun requiresPassword(): Boolean = passwordHash != null
    
    /**
     * Check if user is SAML-only (no password).
     */
    fun isSamlOnly(): Boolean = passwordHash == null
    
    /**
     * Update last login information.
     */
    fun updateLastLogin(ipAddress: String) {
        lastLoginAt = Instant.now()
        lastLoginIp = ipAddress
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "User(id=$id, email='$email', username='$username', role=$role, enabled=$enabled)"
    }
}
