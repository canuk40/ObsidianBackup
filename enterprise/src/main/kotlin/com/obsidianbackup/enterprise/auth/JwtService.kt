package com.obsidianbackup.enterprise.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

/**
 * JWT Service for token generation, validation, and management.
 * 
 * **Research Citations:**
 * - Finding 6: JWT token rotation with 15-minute access tokens and 30-day refresh tokens
 * - Finding 6: Redis blacklist for revoked tokens (logout, suspicious activity)
 * - Finding 6: Single-use refresh tokens (rotate on every refresh)
 * 
 * **Security Features:**
 * - HS256 signing algorithm with 256-bit secret key
 * - Short-lived access tokens (15 minutes)
 * - Long-lived refresh tokens (30 days)
 * - Token blacklist using Redis
 * - Claims validation (issuer, audience, expiration)
 * - Secure random token generation for refresh tokens
 * 
 * **Token Structure:**
 * - Header: alg=HS256, typ=JWT
 * - Payload: sub (user email), org (organization ID), roles, iss, aud, iat, exp
 * - Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
 * 
 * @see <a href="https://www.javacodegeeks.com/2024/12/managing-jwt-refresh-tokens-in-spring-security-a-complete-guide.html">JWT Best Practices</a>
 * @see <a href="https://thachtaro2210.github.io/posts/springboot-jwt-refresh-rotation/">Token Rotation Strategy</a>
 */
@Service
class JwtService(
    @Value("\${application.jwt.secret}") private val jwtSecret: String,
    @Value("\${application.jwt.access-token-expiration-ms}") private val accessTokenExpirationMs: Long,
    @Value("\${application.jwt.refresh-token-expiration-ms}") private val refreshTokenExpirationMs: Long,
    @Value("\${application.jwt.issuer}") private val issuer: String,
    @Value("\${application.jwt.audience}") private val audience: String,
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    
    /**
     * Generate signing key from JWT secret.
     * Uses HMAC-SHA256 algorithm with 256-bit key.
     * 
     * **Security Note:**
     * In production, jwtSecret must be a strong random 256-bit key.
     * Generate with: openssl rand -base64 32
     */
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }
    
    /**
     * Generate access token (15 minutes).
     * 
     * **Token Claims:**
     * - sub: User email (unique identifier)
     * - org: Organization UUID (for RLS)
     * - roles: User roles array
     * - iss: Issuer (ObsidianEnterprise)
     * - aud: Audience (ObsidianBackupApp)
     * - iat: Issued at timestamp
     * - exp: Expiration timestamp
     * 
     * @param email User email (subject)
     * @param organizationId Organization UUID for multi-tenant isolation
     * @param roles User roles (ADMIN, DEVICE_MANAGER, VIEWER)
     * @return JWT access token string
     */
    fun generateAccessToken(email: String, organizationId: UUID, roles: List<String>): String {
        val now = Instant.now()
        val expiresAt = now.plusMillis(accessTokenExpirationMs)
        
        logger.debug("Generating access token for user: {} (org: {})", email, organizationId)
        
        return Jwts.builder()
            .subject(email)
            .claim("org", organizationId.toString())
            .claim("roles", roles)
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact()
    }
    
    /**
     * Generate refresh token (30 days).
     * 
     * **Single-Use Tokens (Finding 6):**
     * Refresh tokens are single-use only. After successful refresh:
     * 1. Old refresh token is revoked (blacklisted)
     * 2. New refresh token is generated
     * 3. This prevents replay attacks
     * 
     * Refresh tokens have minimal claims to reduce size:
     * - sub: User email
     * - org: Organization ID
     * - jti: Unique token ID (for revocation tracking)
     * 
     * @param email User email
     * @param organizationId Organization UUID
     * @return JWT refresh token string
     */
    fun generateRefreshToken(email: String, organizationId: UUID): String {
        val now = Instant.now()
        val expiresAt = now.plusMillis(refreshTokenExpirationMs)
        val tokenId = UUID.randomUUID().toString()  // Unique token ID for revocation
        
        logger.debug("Generating refresh token for user: {} (org: {}, jti: {})", email, organizationId, tokenId)
        
        return Jwts.builder()
            .subject(email)
            .claim("org", organizationId.toString())
            .claim("jti", tokenId)  // Token ID for revocation tracking
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact()
    }
    
    /**
     * Validate JWT token.
     * 
     * **Validation Steps:**
     * 1. Check signature (tamper detection)
     * 2. Check expiration (expired tokens rejected)
     * 3. Check issuer (must be ObsidianEnterprise)
     * 4. Check audience (must be ObsidianBackupApp)
     * 5. Check blacklist (revoked tokens rejected)
     * 
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    fun validateToken(token: String): Boolean {
        try {
            // Check if token is blacklisted (revoked)
            if (isTokenBlacklisted(token)) {
                logger.warn("Token validation failed: token is blacklisted")
                return false
            }
            
            // Parse and validate token
            val claims = extractAllClaims(token)
            
            // Validate issuer
            if (claims.issuer != issuer) {
                logger.warn("Token validation failed: invalid issuer (expected: {}, got: {})", issuer, claims.issuer)
                return false
            }
            
            // Validate audience
            if (!claims.audience.contains(audience)) {
                logger.warn("Token validation failed: invalid audience (expected: {}, got: {})", audience, claims.audience)
                return false
            }
            
            logger.debug("Token validated successfully for user: {}", claims.subject)
            return true
            
        } catch (e: ExpiredJwtException) {
            logger.warn("Token validation failed: token expired (exp: {})", e.claims.expiration)
            return false
        } catch (e: SignatureException) {
            logger.error("Token validation failed: invalid signature", e)
            return false
        } catch (e: MalformedJwtException) {
            logger.error("Token validation failed: malformed token", e)
            return false
        } catch (e: UnsupportedJwtException) {
            logger.error("Token validation failed: unsupported token type", e)
            return false
        } catch (e: IllegalArgumentException) {
            logger.error("Token validation failed: empty token", e)
            return false
        } catch (e: Exception) {
            logger.error("Token validation failed: unexpected error", e)
            return false
        }
    }
    
    /**
     * Extract username (email) from token.
     * 
     * @param token JWT token string
     * @return User email (subject claim)
     */
    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }
    
    /**
     * Extract organization ID from token.
     * 
     * **Multi-Tenant Isolation:**
     * Organization ID is used to set PostgreSQL session variable:
     * SET LOCAL app.current_organization = '<uuid>';
     * This enforces Row-Level Security (RLS) at database level.
     * 
     * @param token JWT token string
     * @return Organization UUID
     */
    fun extractOrganizationId(token: String): UUID {
        val orgId = extractAllClaims(token)["org"] as String
        return UUID.fromString(orgId)
    }
    
    /**
     * Extract user roles from token.
     * 
     * @param token JWT token string
     * @return List of role strings (ADMIN, DEVICE_MANAGER, VIEWER)
     */
    @Suppress("UNCHECKED_CAST")
    fun extractRoles(token: String): List<String> {
        return extractAllClaims(token)["roles"] as? List<String> ?: emptyList()
    }
    
    /**
     * Extract token ID (jti) from refresh token.
     * Used for revocation tracking.
     * 
     * @param token JWT refresh token
     * @return Token ID (UUID string)
     */
    fun extractTokenId(token: String): String? {
        return extractAllClaims(token)["jti"] as? String
    }
    
    /**
     * Extract expiration timestamp from token.
     * 
     * @param token JWT token string
     * @return Expiration date
     */
    fun extractExpiration(token: String): Date {
        return extractAllClaims(token).expiration
    }
    
    /**
     * Check if token is expired.
     * 
     * @param token JWT token string
     * @return true if expired
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            extractExpiration(token).before(Date())
        } catch (e: ExpiredJwtException) {
            true
        }
    }
    
    /**
     * Validate token for specific user.
     * 
     * @param token JWT token string
     * @param userDetails User details from database
     * @return true if token is valid and belongs to user
     */
    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && validateToken(token)
    }
    
    /**
     * Revoke token by adding to Redis blacklist.
     * 
     * **Token Revocation Strategy (Finding 6):**
     * - Blacklisted tokens stored in Redis with TTL = token expiration
     * - Key format: "jwt:blacklist:<token>"
     * - Value: revocation timestamp
     * - Automatic cleanup when token expires (Redis TTL)
     * 
     * **Use Cases:**
     * - User logout
     * - Suspicious activity detected
     * - Refresh token rotation (old token revoked)
     * - Account locked/disabled
     * 
     * @param token JWT token to revoke
     * @param reason Revocation reason (for audit logging)
     */
    fun revokeToken(token: String, reason: String) {
        try {
            val expiration = extractExpiration(token)
            val now = Date()
            val ttl = (expiration.time - now.time) / 1000  // Seconds until expiration
            
            if (ttl > 0) {
                val blacklistKey = "jwt:blacklist:$token"
                val revocationInfo = "${Instant.now()}|$reason"
                
                redisTemplate.opsForValue().set(blacklistKey, revocationInfo, ttl, TimeUnit.SECONDS)
                logger.info("Token revoked: reason={}, ttl={}s", reason, ttl)
            } else {
                logger.debug("Token already expired, skipping blacklist")
            }
        } catch (e: Exception) {
            logger.error("Failed to revoke token: {}", e.message, e)
            throw IllegalStateException("Failed to revoke token", e)
        }
    }
    
    /**
     * Check if token is blacklisted (revoked).
     * 
     * @param token JWT token string
     * @return true if blacklisted
     */
    fun isTokenBlacklisted(token: String): Boolean {
        val blacklistKey = "jwt:blacklist:$token"
        return redisTemplate.hasKey(blacklistKey) ?: false
    }
    
    /**
     * Extract all claims from token.
     * 
     * **Error Handling:**
     * - ExpiredJwtException: Token expired
     * - SignatureException: Invalid signature (tampered token)
     * - MalformedJwtException: Invalid token format
     * - UnsupportedJwtException: Unsupported token type
     * 
     * @param token JWT token string
     * @return Claims object containing all token data
     * @throws ExpiredJwtException if token expired
     * @throws SignatureException if signature invalid
     */
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
    
    /**
     * Get remaining time until token expires.
     * 
     * @param token JWT token string
     * @return Milliseconds until expiration (negative if expired)
     */
    fun getTokenRemainingTime(token: String): Long {
        val expiration = extractExpiration(token)
        return expiration.time - System.currentTimeMillis()
    }
    
    /**
     * Cleanup expired blacklist entries (maintenance task).
     * Redis automatically removes expired keys, but this provides manual cleanup if needed.
     * 
     * Run periodically via scheduled task (e.g., daily).
     */
    fun cleanupExpiredBlacklistEntries() {
        logger.info("Starting expired blacklist cleanup (Redis TTL handles this automatically)")
        // Redis TTL automatically removes expired keys, no action needed
    }
}
