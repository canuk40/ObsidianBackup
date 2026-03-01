// EmailVerificationService.kt
// M-9: Implements Redis-backed single-use email verification token
// SOURCE: Standard email verification token flow

package com.obsidianbackup.enterprise.auth

import com.obsidianbackup.enterprise.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class EmailVerificationService(
    private val redisTemplate: StringRedisTemplate,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(EmailVerificationService::class.java)
    private val TOKEN_TTL = Duration.ofHours(24)

    fun createVerificationToken(userId: String): String {
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set("email_verify:$token", userId, TOKEN_TTL)
        logger.debug("Created email verification token for userId={}", userId)
        return token
    }

    /**
     * Validate and consume an email verification token.
     * Single-use: token is deleted after first successful use.
     *
     * @return userId if token is valid, null otherwise
     */
    fun verifyToken(token: String): String? {
        val key = "email_verify:$token"
        val userId = redisTemplate.opsForValue().get(key) ?: run {
            logger.warn("Email verification token not found or expired: {}", token)
            return null
        }
        redisTemplate.delete(key)  // Single-use token
        logger.debug("Email verification token consumed for userId={}", userId)
        return userId
    }
}
