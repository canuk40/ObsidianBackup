package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for User entity.
 * 
 * **Purpose:**
 * - User authentication queries
 * - Organization membership queries
 * - Role-based queries
 * - User statistics
 * 
 * **Security:**
 * - Email lookup for authentication
 * - Organization isolation enforcement
 * - Role-based filtering
 */
@Repository
interface UserRepository : JpaRepository<User, UUID> {
    
    /**
     * Find user by email address.
     * 
     * **Use Case:** Login authentication
     * 
     * @param email User email address
     * @return User or null
     */
    fun findByEmail(email: String): User?
    
    /**
     * Find user by username.
     * 
     * **Use Case:** Username-based authentication
     * 
     * @param username Username
     * @return User or null
     */
    fun findByUsername(username: String): User?
    
    /**
     * Find user by email within organization.
     * 
     * **Use Case:** Organization-scoped user lookup
     * 
     * @param organizationId Organization UUID
     * @param email User email
     * @return User or null
     */
    fun findByOrganizationIdAndEmail(organizationId: UUID, email: String): User?
    
    /**
     * Find all users in an organization.
     * 
     * **Use Case:** Admin user management
     * 
     * @param organizationId Organization UUID
     * @return List of users
     */
    fun findByOrganizationId(organizationId: UUID): List<User>
    
    /**
     * Find active users in an organization.
     * 
     * **Use Case:** Active user listing
     * 
     * @param organizationId Organization UUID
     * @param enabled Enabled status (default: true)
     * @return List of active users
     */
    fun findByOrganizationIdAndEnabled(organizationId: UUID, enabled: Boolean = true): List<User>
    
    /**
     * Find users by role within organization.
     * 
     * **Use Case:** Admin listing, permission audits
     * 
     * @param organizationId Organization UUID
     * @param role User role
     * @return List of users with role
     */
    fun findByOrganizationIdAndRole(organizationId: UUID, role: User.Role): List<User>
    
    /**
     * Find users by role and enabled status.
     * 
     * **Use Case:** Active admin listing
     * 
     * @param organizationId Organization UUID
     * @param role User role
     * @param enabled Enabled status
     * @return List of users
     */
    fun findByOrganizationIdAndRoleAndEnabled(
        organizationId: UUID,
        role: User.Role,
        enabled: Boolean = true
    ): List<User>
    
    /**
     * Check if email exists in organization.
     * 
     * **Use Case:** Registration validation
     * 
     * @param organizationId Organization UUID
     * @param email Email address
     * @return True if email already exists
     */
    fun existsByOrganizationIdAndEmail(organizationId: UUID, email: String): Boolean
    
    /**
     * Check if username exists in organization.
     * 
     * **Use Case:** Registration validation
     * 
     * @param organizationId Organization UUID
     * @param username Username
     * @return True if username already exists
     */
    fun existsByOrganizationIdAndUsername(organizationId: UUID, username: String): Boolean
    
    /**
     * Count users in organization.
     * 
     * **Use Case:** Quota enforcement
     * 
     * @param organizationId Organization UUID
     * @return User count
     */
    fun countByOrganizationId(organizationId: UUID): Long
    
    /**
     * Count active users in organization.
     * 
     * **Use Case:** Active user metrics
     * 
     * @param organizationId Organization UUID
     * @param enabled Enabled status
     * @return Active user count
     */
    fun countByOrganizationIdAndEnabled(organizationId: UUID, enabled: Boolean = true): Long
    
    /**
     * Count users by role in organization.
     * 
     * **Use Case:** Role distribution metrics
     * 
     * @param organizationId Organization UUID
     * @param role User role
     * @return User count with role
     */
    fun countByOrganizationIdAndRole(organizationId: UUID, role: User.Role): Long
    
    /**
     * Get role distribution for organization.
     * 
     * **Use Case:** Admin dashboard showing role breakdown
     * 
     * @param organizationId Organization UUID
     * @return List of (role, count) pairs
     */
    @Query(
        "SELECT u.role, COUNT(u) FROM User u " +
        "WHERE u.organizationId = :orgId " +
        "GROUP BY u.role " +
        "ORDER BY COUNT(u) DESC"
    )
    fun getRoleDistribution(@Param("orgId") organizationId: UUID): List<Array<Any>>
    
    /**
     * Find users with MFA enabled.
     * 
     * **Use Case:** Security compliance reporting
     * 
     * @param organizationId Organization UUID
     * @param mfaEnabled MFA status
     * @return List of users
     */
    fun findByOrganizationIdAndMfaEnabled(organizationId: UUID, mfaEnabled: Boolean): List<User>
    
    /**
     * Count users with MFA enabled.
     * 
     * **Use Case:** MFA adoption metrics
     * 
     * @param organizationId Organization UUID
     * @param mfaEnabled MFA status
     * @return User count with MFA
     */
    fun countByOrganizationIdAndMfaEnabled(organizationId: UUID, mfaEnabled: Boolean): Long
    
    /**
     * Find SAML-only users (no password).
     * 
     * **Use Case:** SAML migration tracking
     * 
     * @param organizationId Organization UUID
     * @return List of SAML-only users
     */
    @Query(
        "SELECT u FROM User u " +
        "WHERE u.organizationId = :orgId " +
        "AND u.passwordHash IS NULL"
    )
    fun findSamlOnlyUsers(@Param("orgId") organizationId: UUID): List<User>
}
