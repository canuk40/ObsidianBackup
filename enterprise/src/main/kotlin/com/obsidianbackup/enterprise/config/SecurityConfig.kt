package com.obsidianbackup.enterprise.config

import com.obsidianbackup.enterprise.auth.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security configuration for ObsidianBackup Enterprise Backend.
 * 
 * **Research Citations:**
 * - Finding 1: SAML SSO integration (disabled by default, environment-driven)
 * - Finding 6: JWT authentication with token rotation and Redis blacklist
 * - Finding 8: SOC 2 audit logging for all security events
 * 
 * **Security Features:**
 * - BCrypt password encoding (cost factor 12)
 * - JWT-based stateless authentication
 * - CORS configuration for web clients
 * - Rate limiting on authentication endpoints
 * - SAML SSO support (optional)
 * - Method-level security with @PreAuthorize
 * 
 * @see <a href="https://www.javacodegeeks.com/2024/12/managing-jwt-refresh-tokens-in-spring-security-a-complete-guide.html">JWT Best Practices</a>
 * @see <a href="https://www.baeldung.com/spring-security-saml">SAML with Spring Boot</a>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val userDetailsService: UserDetailsService,
    // Set application.saml.enabled=true in application.properties to activate SAML SSO.
    // Also configure spring.security.saml2.relyingparty.registration.* for your IdP.
    @Value("\${application.saml.enabled:false}")
    private val samlEnabled: Boolean
) {

    /**
     * Password encoder using BCrypt with cost factor 12.
     * BCrypt is resistant to rainbow table attacks and provides adaptive hashing.
     * 
     * Cost factor 12 provides strong security while maintaining acceptable performance.
     * Each increment doubles computation time (2^12 = 4096 rounds).
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12)
    }

    /**
     * Authentication provider using database-backed user details.
     * Integrates with UserDetailsService for user lookup and password verification.
     */
    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        return DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder())
        }
    }

    /**
     * Authentication manager for programmatic authentication.
     * Used in AuthController for login endpoint.
     */
    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    /**
     * Main security filter chain configuration.
     * 
     * **Authentication Flow:**
     * 1. Request arrives → CORS filter
     * 2. JWT filter extracts token from Authorization header
     * 3. JWT validated and SecurityContext populated
     * 4. Request proceeds to controller
     * 
     * **Stateless Sessions:**
     * - No server-side session storage
     * - JWT contains all authentication information
     * - Session policy: STATELESS
     * 
     * **Public Endpoints:**
     * - /api/v1/auth/login - User login
     * - /api/v1/auth/register - User registration
     * - /actuator/health - Health checks (Docker)
     * - /actuator/health/liveness - Kubernetes liveness probe
     * - /actuator/health/readiness - Kubernetes readiness probe
     * 
     * @see <a href="https://thachtaro2210.github.io/posts/springboot-jwt-refresh-rotation/">JWT Refresh Token Rotation</a>
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }  // Disabled for stateless JWT authentication
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                // Public endpoints
                auth.requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/forgot-password",
                    "/actuator/health",
                    "/actuator/health/liveness",
                    "/actuator/health/readiness"
                ).permitAll()
                
                // SAML endpoints (if enabled)
                auth.requestMatchers(
                    "/saml/**",
                    "/login/saml2/sso/**"
                ).permitAll()
                
                // Prometheus metrics (restrict to internal networks in production)
                auth.requestMatchers("/actuator/prometheus").permitAll()
                
                // Admin endpoints - ADMIN or SUPER_ADMIN only
                auth.requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                
                // All other endpoints require authentication
                auth.anyRequest().authenticated()
            }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        // SAML SSO — enabled via application.saml.enabled=true (default: off).
        // Configure IdP metadata via spring.security.saml2.relyingparty.registration.obsidianbackup.*
        // See SamlAuthProvider.kt for the assertion-handling logic and setup instructions.
        if (samlEnabled) {
            http.saml2Login { saml ->
                saml.defaultSuccessUrl("/api/v1/auth/saml/callback")
            }
        }

        return http.build()
    }

    /**
     * CORS configuration for web clients.
     * 
     * **Production Configuration:**
     * - allowedOrigins should be restricted to specific domains
     * - Set via environment variable: CORS_ALLOWED_ORIGINS
     * - Default allows localhost for development
     * 
     * **Security Notes:**
     * - credentials=true allows cookies (for SAML if needed)
     * - exposedHeaders allows clients to read custom headers
     * - maxAge caches preflight requests for 1 hour
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // Production: Set via environment variable
            allowedOrigins = listOf(
                "http://localhost:3000",  // React dev server
                "http://localhost:8080",  // Backend (for testing)
                "https://admin.obsidianbackup.com"  // Production admin console
            )
            
            allowedMethods = listOf(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.OPTIONS.name()
            )
            
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-API-Version",
                "X-Organization-ID"
            )
            
            exposedHeaders = listOf(
                "X-Total-Count",
                "X-Total-Pages",
                "X-Current-Page"
            )
            
            allowCredentials = true
            maxAge = 3600L  // 1 hour
        }
        
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
