package com.obsidianbackup.enterprise.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

fun Application.configureAuth() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()
    
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

object JWTConfig {
    private val SECRET: String = System.getenv("JWT_SECRET")
        ?: error("JWT_SECRET environment variable is required but not set. " +
                 "Generate with: openssl rand -base64 32")
    private const val ISSUER = "obsidian-enterprise"
    private const val AUDIENCE = "obsidian-clients"
    private const val VALIDITY = 36_000_00 * 24 // 24 hours
    
    fun generateToken(userId: String, email: String, roles: List<String>): String {
        return JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("roles", roles)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY))
            .sign(Algorithm.HMAC256(SECRET))
    }
}
