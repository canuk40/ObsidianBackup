package com.obsidianbackup.enterprise.auth

import com.obsidianbackup.enterprise.model.RefreshToken
import com.obsidianbackup.enterprise.repository.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Refresh Token Service for database-backed token management.
 * 
 * **Research Citations:**
 * - Finding 6: Single-use refresh tokens with rotation on every refresh
 * - Finding 6: Database storage for revocation support and audit trail
 * - Finding 6: Automatic cleanup of expired tokens
 * 
 * **Token Lifecycle:**
 * 1. Login → Create refresh token (30 days)
 * 2. Refresh → Revoke old token, create new token
 * 3. Logout → Revoke token immediately
 * 4. Expired → Automatic cleanup via scheduled job
 * 
 * **Single-Use Enforcement:**
 * Refresh tokens can only be used once. After successful refresh:
 * - Old token is marked as revoked
 * - New token is created
 * - Attempting to reuse old token returns error
 * 
 * **Security Features:**
 * - Database persistence for audit trail
 * - Revocation tracking (timestamp + reason)
 * - Device fingerprinting (user agent, IP)
 * - Organization isolation (multi-tenant)
 * - Automatic cleanup of expired tokens
 * 
 * @see <a href="https://bootify.io/spring-security/refresh-tokens-spring-boot.html">Refresh Tokens with Spring Boot</a>
 * @see <a href="https://thachtaro2210.github.io/posts/springboot-jwt-refresh-rotation/">JWT Refresh Token Rotation</a>
 */
@Service
@Transactional
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService
) {
    
    private val logger = LoggerFactory.getLogger(RefreshTokenService::class.java)
    
    companion object {
        private const val REFRESH_TOKEN_EXPIRATION_DAYS = 30L
    }
    
    /**
     * Create new refresh token and persist to database.
     * 
     * **Token Storage:**
     * - Token string stored in database (for validation)
     * - Linked to user and organization (multi-tenant)
     * - Device info captured (user agent, IP address)
     * - Expiration set to 30 days from creation
     * 
     * **Use Cases:**
     * - User login (initial token creation)
     * - Token rotation (after successful refresh)
     * 
     * @param userId User UUID
     * @param organizationId Organization UUID
     * @param userEmail User email (for JWT claims)
     * @param deviceInfo Device information (user agent, device type)
     * @param ipAddress Client IP address
     * @return Persisted RefreshToken entity
     * @throws IllegalStateException if token creation fails
     */
    fun createRefreshToken(
        userId: UUID,
        organizationId: UUID,
        userEmail: String,
        deviceInfo: Map<String, Any>? = null,
        ipAddress: String? = null
    ): RefreshToken {
        try {
            // Generate JWT refresh token
            val tokenString = jwtService.generateRefreshToken(userEmail, organizationId)
            
            // Calculate expiration
            val expiresAt = Instant.now().plus(REFRESH_TOKEN_EXPIRATION_DAYS, ChronoUnit.DAYS)
            
            // Create entity
            val refreshToken = RefreshToken(
                userId = userId,
                organizationId = organizationId,
                token = tokenString,
                expiresAt = expiresAt,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress
            )
            
            // Persist to database
            val savedToken = refreshTokenRepository.save(refreshToken)
            
            logger.info(
                "Created refresh token for user: {} (org: {}, expires: {}, device: {})",
                userEmail,
                organizationId,
                expiresAt,
                deviceInfo?.get("userAgent") ?: "unknown"
            )
            
            return savedToken
            
        } catch (e: Exception) {
            logger.error("Failed to create refresh token for user: {}", userEmail, e)
            throw IllegalStateException("Failed to create refresh token", e)
        }
    }
    
    /**
     * Validate refresh token and return entity if valid.
     * 
     * **Validation Steps:**
     * 1. Check token exists in database
     * 2. Check token is not revoked
     * 3. Check token is not expired
     * 4. Validate JWT signature and claims
     * 
     * @param tokenString Refresh token string
     * @return RefreshToken entity if valid
     * @throws RefreshTokenException if validation fails
     */
    fun validateRefreshToken(tokenString: String): RefreshToken {
        // Find token in database
        val refreshToken = refreshTokenRepository.findByToken(tokenString)
            ?: throw RefreshTokenException("Refresh token not found")
        
        // Check if revoked
        if (refreshToken.revoked) {
            logger.warn(
                "Attempted to use revoked refresh token (revoked: {}, reason: {})",
                refreshToken.revokedAt,
                refreshToken.revokedReason
            )
            throw RefreshTokenException("Refresh token has been revoked: ${refreshToken.revokedReason}")
        }
        
        // Check if expired
        if (refreshToken.expiresAt.isBefore(Instant.now())) {
            logger.warn("Attempted to use expired refresh token (expired: {})", refreshToken.expiresAt)
            throw RefreshTokenException("Refresh token has expired")
        }
        
        // Validate JWT signature and claims
        if (!jwtService.validateToken(tokenString)) {
            logger.error("Refresh token JWT validation failed")
            throw RefreshTokenException("Invalid refresh token signature or claims")
        }
        
        logger.debug("Refresh token validated successfully: {}", refreshToken.id)
        return refreshToken
    }
    
    /**
     * Rotate refresh token (single-use enforcement).
     * 
     * **Token Rotation Flow (Finding 6):**
     * 1. Validate old refresh token
     * 2. Extract user info from old token
     * 3. Revoke old refresh token (mark as used)
     * 4. Generate new access token (15 minutes)
     * 5. Generate new refresh token (30 days)
     * 6. Persist new refresh token to database
     * 7. Return new token pair
     * 
     * **Security:**
     * - Old token immediately revoked (prevents replay)
     * - New tokens have fresh expiration times
     * - All operations in single transaction (atomic)
     * 
     * @param oldTokenString Old refresh token string
     * @param deviceInfo Device information for new token
     * @param ipAddress Client IP address for new token
     * @return Pair of (new access token, new refresh token)
     * @throws RefreshTokenException if rotation fails
     */
    fun rotateRefreshToken(
        oldTokenString: String,
        deviceInfo: Map<String, Any>? = null,
        ipAddress: String? = null
    ): Pair<String, String> {
        try {
            // Validate old token
            val oldToken = validateRefreshToken(oldTokenString)
            
            // Extract user info from JWT
            val userEmail = jwtService.extractUsername(oldTokenString)
            val organizationId = jwtService.extractOrganizationId(oldTokenString)
            val roles = jwtService.extractRoles(oldTokenString)
            
            // Revoke old token (single-use enforcement)
            revokeToken(oldTokenString, "TOKEN_ROTATION")
            
            // Generate new access token (15 minutes)
            val newAccessToken = jwtService.generateAccessToken(userEmail, organizationId, roles)
            
            // Create new refresh token (30 days)
            val newRefreshToken = createRefreshToken(
                userId = oldToken.userId,
                organizationId = organizationId,
                userEmail = userEmail,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress
            )
            
            logger.info(
                "Rotated refresh token for user: {} (old: {}, new: {})",
                userEmail,
                oldToken.id,
                newRefreshToken.id
            )
            
            return Pair(newAccessToken, newRefreshToken.token)
            
        } catch (e: RefreshTokenException) {
            // Re-throw validation errors
            throw e
        } catch (e: Exception) {
            logger.error("Failed to rotate refresh token", e)
            throw IllegalStateException("Failed to rotate refresh token", e)
        }
    }
    
    /**
     * Revoke refresh token.
     * 
     * **Revocation:**
     * - Mark token as revoked in database
     * - Set revocation timestamp
     * - Record revocation reason
     * - Add JWT to Redis blacklist (if not expired)
     * 
     * **Use Cases:**
     * - User logout
     * - Token rotation (old token)
     * - Suspicious activity detected
     * - Admin forced logout
     * 
     * @param tokenString Refresh token to revoke
     * @param reason Revocation reason (for audit)
     */
    fun revokeToken(tokenString: String, reason: String) {
        try {
            val refreshToken = refreshTokenRepository.findByToken(tokenString)
            
            if (refreshToken == null) {
                logger.warn("Attempted to revoke non-existent token")
                return
            }
            
            // Mark as revoked in database
            refreshToken.revoked = true
            refreshToken.revokedAt = Instant.now()
            refreshToken.revokedReason = reason
            refreshTokenRepository.save(refreshToken)
            
            // Add to JWT blacklist in Redis (if not expired)
            if (refreshToken.expiresAt.isAfter(Instant.now())) {
                jwtService.revokeToken(tokenString, reason)
            }
            
            logger.info(
                "Revoked refresh token: {} (user: {}, reason: {})",
                refreshToken.id,
                refreshToken.userId,
                reason
            )
            
        } catch (e: Exception) {
            logger.error("Failed to revoke token: {}", e.message, e)
            throw IllegalStateException("Failed to revoke token", e)
        }
    }
    
    /**
     * Revoke all refresh tokens for a user.
     * 
     * **Use Cases:**
     * - Password change (invalidate all sessions)
     * - Account locked/disabled
     * - Suspicious activity (force re-authentication)
     * - User requests "logout all devices"
     * 
     * @param userId User UUID
     * @param reason Revocation reason
     * @return Number of tokens revoked
     */
    fun revokeAllUserTokens(userId: UUID, reason: String): Int {
        try {
            val activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
            
            var revokedCount = 0
            for (token in activeTokens) {
                revokeToken(token.token, reason)
                revokedCount++
            }
            
            logger.info(
                "Revoked all refresh tokens for user: {} (count: {}, reason: {})",
                userId,
                revokedCount,
                reason
            )
            
            return revokedCount
            
        } catch (e: Exception) {
            logger.error("Failed to revoke all user tokens: userId={}", userId, e)
            throw IllegalStateException("Failed to revoke all user tokens", e)
        }
    }
    
    /**
     * Revoke all refresh tokens for an organization.
     * 
     * **Use Cases:**
     * - Organization subscription cancelled
     * - Security breach detected
     * - Organization disabled
     * 
     * @param organizationId Organization UUID
     * @param reason Revocation reason
     * @return Number of tokens revoked
     */
    fun revokeAllOrganizationTokens(organizationId: UUID, reason: String): Int {
        try {
            val activeTokens = refreshTokenRepository.findByOrganizationIdAndRevokedFalse(organizationId)
            
            var revokedCount = 0
            for (token in activeTokens) {
                revokeToken(token.token, reason)
                revokedCount++
            }
            
            logger.info(
                "Revoked all refresh tokens for organization: {} (count: {}, reason: {})",
                organizationId,
                revokedCount,
                reason
            )
            
            return revokedCount
            
        } catch (e: Exception) {
            logger.error("Failed to revoke all organization tokens: orgId={}", organizationId, e)
            throw IllegalStateException("Failed to revoke all organization tokens", e)
        }
    }
    
    /**
     * Delete refresh token from database.
     * 
     * **Note:**
     * Prefer revokeToken() over deleteToken() to maintain audit trail.
     * Only use deleteToken() for expired token cleanup.
     * 
     * @param tokenString Refresh token to delete
     */
    fun deleteToken(tokenString: String) {
        try {
            refreshTokenRepository.deleteByToken(tokenString)
            logger.debug("Deleted refresh token from database")
        } catch (e: Exception) {
            logger.error("Failed to delete token: {}", e.message, e)
            throw IllegalStateException("Failed to delete token", e)
        }
    }
    
    /**
     * Cleanup expired refresh tokens (scheduled job).
     * 
     * **Scheduled Execution:**
     * Runs daily at 2:00 AM server time.
     * Deletes tokens expired more than 7 days ago (retention for audit).
     * 
     * **SOC 2 Compliance:**
     * Expired tokens retained for 7 days before deletion (allows audit investigation).
     * 
     * @return Number of tokens deleted
     */
    @Scheduled(cron = "0 0 2 * * *")  // Daily at 2:00 AM
    fun cleanupExpiredTokens(): Int {
        try {
            // Delete tokens expired more than 7 days ago
            val cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS)
            val deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(cutoffDate)
            
            logger.info(
                "Cleaned up expired refresh tokens (deleted: {}, cutoff: {})",
                deletedCount,
                cutoffDate
            )
            
            return deletedCount
            
        } catch (e: Exception) {
            logger.error("Failed to cleanup expired tokens", e)
            throw IllegalStateException("Failed to cleanup expired tokens", e)
        }
    }
    
    /**
     * Get all active refresh tokens for a user.
     * 
     * **Use Case:**
     * Display active sessions to user ("Logged in from 3 devices").
     * 
     * @param userId User UUID
     * @return List of active refresh tokens
     */
    fun getActiveUserTokens(userId: UUID): List<RefreshToken> {
        return refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
    }
    
    /**
     * Get token statistics for monitoring.
     * 
     * @return Map of statistics (total, active, expired, revoked)
     */
    fun getTokenStatistics(): Map<String, Long> {
        val total = refreshTokenRepository.count()
        val active = refreshTokenRepository.countByRevokedFalse()
        val revoked = refreshTokenRepository.countByRevokedTrue()
        val expired = refreshTokenRepository.countByExpiresAtBeforeAndRevokedFalse(Instant.now())
        
        return mapOf(
            "total" to total,
            "active" to active,
            "revoked" to revoked,
            "expired" to expired
        )
    }
}

/**
 * Custom exception for refresh token validation errors.
 * 
 * **Use Cases:**
 * - Token not found
 * - Token revoked
 * - Token expired
 * - Invalid signature
 */
class RefreshTokenException(message: String) : RuntimeException(message)
