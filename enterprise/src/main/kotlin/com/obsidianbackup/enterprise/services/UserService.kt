// UserService.kt
// M-11: Handle login lockout on failed login attempts

package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.model.User
import com.obsidianbackup.enterprise.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserService(private val userRepository: UserRepository) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * M-11: Record a failed login attempt and lock account if threshold exceeded.
     * Lock duration: 30 minutes after MAX_FAILED_ATTEMPTS consecutive failures.
     */
    fun recordFailedLogin(userId: String) {
        val user = userRepository.findById(UUID.fromString(userId)).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }
        val newAttempts = user.failedLoginAttempts + 1
        val lockedUntil = if (newAttempts >= User.MAX_FAILED_ATTEMPTS) {
            logger.warn("Account locked for userId={} after {} failed attempts", userId, newAttempts)
            Instant.now().plus(User.LOCKOUT_DURATION)
        } else null
        userRepository.save(user.copy(
            failedLoginAttempts = newAttempts,
            accountLockedUntil = lockedUntil
        ))
    }

    /**
     * M-11: Reset failed login counter on successful authentication.
     */
    fun recordSuccessfulLogin(userId: String) {
        val user = userRepository.findById(UUID.fromString(userId)).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }
        userRepository.save(user.copy(failedLoginAttempts = 0, accountLockedUntil = null))
    }
}
