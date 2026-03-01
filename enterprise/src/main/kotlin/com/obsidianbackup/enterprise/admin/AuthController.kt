package com.obsidianbackup.enterprise.admin

import com.obsidianbackup.enterprise.audit.AuditLogService
import com.obsidianbackup.enterprise.auth.EmailVerificationService
import com.obsidianbackup.enterprise.auth.JwtService
import com.obsidianbackup.enterprise.auth.RefreshTokenService
import com.obsidianbackup.enterprise.model.Organization
import com.obsidianbackup.enterprise.model.User
import com.obsidianbackup.enterprise.repository.OrganizationRepository
import com.obsidianbackup.enterprise.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Authentication controller for user registration, login, and token management.
 * 
 * **Endpoints:**
 * - POST /api/v1/auth/register - User registration
 * - POST /api/v1/auth/login - User login
 * - POST /api/v1/auth/refresh - Token refresh
 * - POST /api/v1/auth/logout - User logout
 * - POST /api/v1/auth/verify-email - Email verification (future)
 * 
 * **Security:**
 * - BCrypt password hashing (cost factor 12)
 * - JWT access tokens (15 minutes)
 * - JWT refresh tokens (30 days)
 * - Rate limiting via headers
 * - Audit logging for all operations
 * 
 * **Error Handling:**
 * - 400 Bad Request: Validation errors
 * - 401 Unauthorized: Invalid credentials
 * - 403 Forbidden: Account disabled
 * - 409 Conflict: Email/username already exists
 * - 500 Internal Server Error: Unexpected errors
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val auditLogService: AuditLogService,
    private val emailVerificationService: EmailVerificationService  // M-9
) {
    
    private val logger = LoggerFactory.getLogger(AuthController::class.java)
    
    // ============================================================================
    // REGISTRATION
    // ============================================================================
    
    /**
     * Register new user.
     * 
     * **Process:**
     * 1. Validate request
     * 2. Check email/username uniqueness
     * 3. Hash password (BCrypt)
     * 4. Create user account
     * 5. Generate tokens
     * 6. Log audit event
     * 
     * **Rate Limiting:**
     * - X-RateLimit-Limit: 10 per hour
     * - X-RateLimit-Remaining: Remaining requests
     * 
     * @param request Registration request
     * @param httpRequest HTTP request for IP tracking
     * @return Authentication response with tokens
     */
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        return try {
            // Check if organization exists
            val organization = organizationRepository.findBySlug(request.organizationSlug)
                ?: return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.error("Organization not found"))
            
            // Check organization quota
            val currentUserCount = userRepository.countByOrganizationId(organization.id!!)
            if (!organization.canAddUser(currentUserCount.toInt())) {
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(AuthResponse.error("Organization user quota exceeded"))
            }
            
            // Check email uniqueness
            if (userRepository.existsByOrganizationIdAndEmail(organization.id, request.email)) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(AuthResponse.error("Email already registered"))
            }
            
            // Check username uniqueness
            if (userRepository.existsByOrganizationIdAndUsername(organization.id, request.username)) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(AuthResponse.error("Username already taken"))
            }
            
            // Hash password
            val passwordHash = passwordEncoder.encode(request.password)
            
            // Create user
            val user = User(
                organizationId = organization.id,
                email = request.email,
                username = request.username,
                passwordHash = passwordHash,
                role = User.Role.VIEWER,
                enabled = true
            )
            
            val savedUser = userRepository.save(user)
            
            // Generate tokens
            val roles = listOf(savedUser.role.name)
            val accessToken = jwtService.generateAccessToken(savedUser.email, organization.id, roles)
            
            val deviceInfo = mapOf(
                "userAgent" to (httpRequest.getHeader("User-Agent") ?: "Unknown")
            )
            val refreshToken = refreshTokenService.createRefreshToken(
                userId = savedUser.id!!,
                organizationId = organization.id,
                userEmail = savedUser.email,
                deviceInfo = deviceInfo,
                ipAddress = httpRequest.remoteAddr
            )
            
            // Log audit event
            auditLogService.logAuthenticationSuccess(
                userId = savedUser.id,
                organizationId = organization.id,
                ipAddress = httpRequest.remoteAddr,
                userAgent = httpRequest.getHeader("User-Agent"),
                sessionId = null,
                deviceInfo = deviceInfo
            )
            
            logger.info("User registered: email={}, org={}", savedUser.email, organization.slug)
            
            ResponseEntity.ok(
                AuthResponse(
                    success = true,
                    accessToken = accessToken,
                    refreshToken = refreshToken.token,
                    expiresIn = jwtService.getAccessTokenExpirationMs(),
                    user = UserDto.from(savedUser)
                )
            )
            
        } catch (e: Exception) {
            logger.error("Registration failed: ${request.email}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Registration failed: ${e.message}"))
        }
    }
    
    // ============================================================================
    // LOGIN
    // ============================================================================
    
    /**
     * User login with email and password.
     * 
     * **Process:**
     * 1. Validate credentials
     * 2. Check account enabled
     * 3. Verify password
     * 4. Generate tokens
     * 5. Update last login
     * 6. Log audit event
     * 
     * @param request Login request
     * @param httpRequest HTTP request for IP tracking
     * @return Authentication response with tokens
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        return try {
            // Find user
            val user = userRepository.findByEmail(request.email)
            
            if (user == null) {
                // Log failed attempt
                val organization = organizationRepository.findBySlug(request.organizationSlug)
                if (organization != null) {
                    auditLogService.logAuthenticationFailure(
                        email = request.email,
                        reason = "User not found",
                        organizationId = organization.id!!,
                        ipAddress = httpRequest.remoteAddr,
                        userAgent = httpRequest.getHeader("User-Agent")
                    )
                }
                
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid credentials"))
            }
            
            // Check account enabled
            if (!user.isEnabled) {
                auditLogService.logAuthenticationFailure(
                    email = request.email,
                    reason = "Account disabled",
                    organizationId = user.organizationId,
                    ipAddress = httpRequest.remoteAddr,
                    userAgent = httpRequest.getHeader("User-Agent")
                )
                
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(AuthResponse.error("Account disabled"))
            }
            
            // Verify password
            if (user.password == null || !passwordEncoder.matches(request.password, user.password)) {
                auditLogService.logAuthenticationFailure(
                    email = request.email,
                    reason = "Invalid password",
                    organizationId = user.organizationId,
                    ipAddress = httpRequest.remoteAddr,
                    userAgent = httpRequest.getHeader("User-Agent")
                )
                
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid credentials"))
            }
            
            // Generate tokens
            val roles = listOf(user.role.name)
            val accessToken = jwtService.generateAccessToken(user.email, user.organizationId, roles)
            
            val deviceInfo = mapOf(
                "userAgent" to (httpRequest.getHeader("User-Agent") ?: "Unknown")
            )
            val refreshToken = refreshTokenService.createRefreshToken(
                userId = user.id!!,
                organizationId = user.organizationId,
                userEmail = user.email,
                deviceInfo = deviceInfo,
                ipAddress = httpRequest.remoteAddr
            )
            
            // Update last login
            user.updateLastLogin(httpRequest.remoteAddr)
            userRepository.save(user)
            
            // Log audit event
            auditLogService.logAuthenticationSuccess(
                userId = user.id,
                organizationId = user.organizationId,
                ipAddress = httpRequest.remoteAddr,
                userAgent = httpRequest.getHeader("User-Agent"),
                sessionId = refreshToken.id.toString(),
                deviceInfo = deviceInfo
            )
            
            logger.info("User logged in: email={}", user.email)
            
            ResponseEntity.ok(
                AuthResponse(
                    success = true,
                    accessToken = accessToken,
                    refreshToken = refreshToken.token,
                    expiresIn = jwtService.getAccessTokenExpirationMs(),
                    user = UserDto.from(user)
                )
            )
            
        } catch (e: Exception) {
            logger.error("Login failed: ${request.email}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Login failed: ${e.message}"))
        }
    }
    
    // ============================================================================
    // TOKEN REFRESH
    // ============================================================================
    
    /**
     * Refresh access token using refresh token.
     * 
     * **Process:**
     * 1. Validate refresh token
     * 2. Revoke old refresh token
     * 3. Generate new tokens
     * 4. Log audit event
     * 
     * @param request Refresh request
     * @param httpRequest HTTP request for IP tracking
     * @return New tokens
     */
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        return try {
            // Validate and rotate tokens
            val deviceInfo = mapOf(
                "userAgent" to (httpRequest.getHeader("User-Agent") ?: "Unknown")
            )
            
            val (newAccessToken, newRefreshToken) = refreshTokenService.rotateRefreshToken(
                oldTokenString = request.refreshToken,
                deviceInfo = deviceInfo,
                ipAddress = httpRequest.remoteAddr
            )
            
            // Extract user info from access token
            val email = jwtService.extractEmail(newAccessToken)
            val organizationId = jwtService.extractOrganizationId(newAccessToken)
            
            val user = userRepository.findByEmail(email)
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("User not found"))
            
            // Log audit event
            auditLogService.logTokenRefresh(
                userId = user.id!!,
                organizationId = organizationId,
                ipAddress = httpRequest.remoteAddr
            )
            
            logger.info("Token refreshed: email={}", email)
            
            ResponseEntity.ok(
                AuthResponse(
                    success = true,
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    expiresIn = jwtService.getAccessTokenExpirationMs(),
                    user = UserDto.from(user)
                )
            )
            
        } catch (e: IllegalStateException) {
            logger.warn("Token refresh failed: ${e.message}")
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error("Invalid or expired refresh token"))
        } catch (e: Exception) {
            logger.error("Token refresh error", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Token refresh failed: ${e.message}"))
        }
    }
    
    // ============================================================================
    // LOGOUT
    // ============================================================================
    
    /**
     * Logout user by revoking refresh token.
     * 
     * **Process:**
     * 1. Validate refresh token
     * 2. Revoke token
     * 3. Blacklist access token (optional)
     * 4. Log audit event
     * 
     * @param request Logout request
     * @param httpRequest HTTP request for IP tracking
     * @return Success response
     */
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        return try {
            // Revoke refresh token
            refreshTokenService.revokeToken(request.refreshToken, "User logout")
            
            // Extract user info
            val email = jwtService.extractEmail(request.accessToken)
            val organizationId = jwtService.extractOrganizationId(request.accessToken)
            
            val user = userRepository.findByEmail(email)
            
            // Blacklist access token
            jwtService.revokeToken(request.accessToken, "User logout")
            
            // Log audit event
            if (user != null) {
                auditLogService.logLogout(
                    userId = user.id!!,
                    organizationId = organizationId,
                    reason = "User initiated",
                    sessionId = null
                )
            }
            
            logger.info("User logged out: email={}", email)
            
            ResponseEntity.ok(MessageResponse(success = true, message = "Logged out successfully"))
            
        } catch (e: Exception) {
            logger.error("Logout error", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponse(success = false, message = "Logout failed: ${e.message}"))
        }
    }
    
    // ============================================================================
    // EMAIL VERIFICATION (Future Implementation)
    // ============================================================================
    
    /**
     * Verify email address using the single-use token sent during registration.
     *
     * @param request Verification request containing the token
     * @return Success or error response
     */
    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ResponseEntity<MessageResponse> {
        val userId = emailVerificationService.verifyToken(request.token)
            ?: return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse(success = false, message = "Invalid or expired verification token"))

        userRepository.findById(userId).ifPresent { user ->
            userRepository.save(user.copy(emailVerified = true))
        }

        return ResponseEntity.ok(MessageResponse(success = true, message = "Email verified successfully"))
    }
}

// ============================================================================
// REQUEST DTOs
// ============================================================================

/**
 * User registration request.
 */
data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username must contain only letters, numbers, underscores, and hyphens"
    )
    val username: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    val password: String,
    
    @field:NotBlank(message = "Organization slug is required")
    val organizationSlug: String
)

/**
 * User login request.
 */
data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String,
    
    @field:NotBlank(message = "Organization slug is required")
    val organizationSlug: String
)

/**
 * Token refresh request.
 */
data class RefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

/**
 * Logout request.
 */
data class LogoutRequest(
    @field:NotBlank(message = "Access token is required")
    val accessToken: String,
    
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

/**
 * Email verification request.
 */
data class VerifyEmailRequest(
    @field:NotBlank(message = "Verification token is required")
    val token: String
)

// ============================================================================
// RESPONSE DTOs
// ============================================================================

/**
 * Authentication response.
 */
data class AuthResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val user: UserDto? = null,
    val error: String? = null
) {
    companion object {
        fun error(message: String) = AuthResponse(success = false, error = message)
    }
}

/**
 * Generic message response.
 */
data class MessageResponse(
    val success: Boolean,
    val message: String
)

/**
 * User data transfer object.
 */
data class UserDto(
    val id: UUID,
    val email: String,
    val username: String,
    val role: String,
    val organizationId: UUID,
    val mfaEnabled: Boolean
) {
    companion object {
        fun from(user: User): UserDto {
            return UserDto(
                id = user.id!!,
                email = user.email,
                username = user.username,
                role = user.role.name,
                organizationId = user.organizationId,
                mfaEnabled = user.mfaEnabled
            )
        }
    }
}
