package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Repository for RefreshToken entity.
 * 
 * **Purpose:**
 * - CRUD operations for refresh tokens
 * - Token validation queries
 * - Token cleanup operations
 * - User/organization token queries
 */
@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    
    /**
     * Find refresh token by token string.
     * 
     * @param token JWT refresh token string
     * @return RefreshToken entity or null
     */
    fun findByToken(token: String): RefreshToken?
    
    /**
     * Find all active (non-revoked) tokens for a user.
     * 
     * @param userId User UUID
     * @return List of active refresh tokens
     */
    fun findByUserIdAndRevokedFalse(userId: UUID): List<RefreshToken>
    
    /**
     * Find all active (non-revoked) tokens for an organization.
     * 
     * @param organizationId Organization UUID
     * @return List of active refresh tokens
     */
    fun findByOrganizationIdAndRevokedFalse(organizationId: UUID): List<RefreshToken>
    
    /**
     * Delete token by token string.
     * 
     * @param token JWT refresh token string
     */
    @Modifying
    fun deleteByToken(token: String)
    
    /**
     * Delete tokens expired before cutoff date.
     * 
     * @param cutoffDate Expiration cutoff (e.g., 7 days ago)
     * @return Number of tokens deleted
     */
    @Modifying
    fun deleteByExpiresAtBefore(cutoffDate: Instant): Int
    
    /**
     * Count active (non-revoked) tokens.
     * 
     * @return Number of active tokens
     */
    fun countByRevokedFalse(): Long
    
    /**
     * Count revoked tokens.
     * 
     * @return Number of revoked tokens
     */
    fun countByRevokedTrue(): Long
    
    /**
     * Count expired but non-revoked tokens.
     * 
     * @param now Current timestamp
     * @return Number of expired tokens
     */
    fun countByExpiresAtBeforeAndRevokedFalse(now: Instant): Long
}
