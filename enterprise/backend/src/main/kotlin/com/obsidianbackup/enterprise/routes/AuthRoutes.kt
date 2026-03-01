package com.obsidianbackup.enterprise.routes

import com.obsidianbackup.enterprise.auth.JWTConfig
import com.obsidianbackup.enterprise.auth.SAMLService
import com.obsidianbackup.enterprise.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String>
)

fun Application.configureAuthRoutes() {
    val userService = UserService()
    val samlService = SAMLService()
    
    routing {
        route("/api/v1/auth") {
            post("/login") {
                val request = call.receive<LoginRequest>()
                val user = userService.authenticate(request.email, request.password)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                
                val token = JWTConfig.generateToken(user.id, user.email, user.roles)
                
                call.respond(LoginResponse(
                    token = token,
                    user = UserInfo(user.id, user.email, user.name, user.roles)
                ))
            }
            
            post("/saml/initiate") {
                val orgId = call.receive<Map<String, String>>()["organizationId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                
                val authUrl = samlService.createAuthRequest(orgId, emptyMap())
                call.respond(mapOf("authUrl" to authUrl))
            }
            
            post("/saml/callback") {
                val samlResponse = call.receive<Map<String, String>>()["SAMLResponse"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "SAML response required"))
                
                val authResponse = samlService.processAuthResponse(samlResponse, emptyMap())
                val user = userService.findOrCreateSAMLUser(authResponse.email, authResponse.name)
                
                val token = JWTConfig.generateToken(user.id, user.email, user.roles)
                
                call.respond(LoginResponse(
                    token = token,
                    user = UserInfo(user.id, user.email, user.name, user.roles)
                ))
            }
            
            post("/logout") {
                // Invalidate token (implement token blacklist if needed)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
            }
        }
    }
}
