package com.obsidianbackup.enterprise.routes

import com.obsidianbackup.enterprise.models.Role
import com.obsidianbackup.enterprise.services.RBACService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RoleCreateRequest(
    val name: String,
    val permissions: List<String>,
    val description: String?
)

@Serializable
data class UserRoleAssignment(
    val userId: String,
    val roleIds: List<String>
)

fun Application.configureRBACRoutes() {
    val rbacService = RBACService()
    
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/rbac") {
                route("/roles") {
                    get {
                        val roles = rbacService.getAllRoles()
                        call.respond(roles)
                    }
                    
                    post {
                        val request = call.receive<RoleCreateRequest>()
                        val role = rbacService.createRole(request)
                        call.respond(HttpStatusCode.Created, role)
                    }
                    
                    get("/{roleId}") {
                        val roleId = call.parameters["roleId"]
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        
                        val role = rbacService.getRole(roleId)
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                        
                        call.respond(role)
                    }
                    
                    delete("/{roleId}") {
                        val roleId = call.parameters["roleId"]
                            ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        
                        rbacService.deleteRole(roleId)
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                
                route("/users/{userId}/roles") {
                    get {
                        val userId = call.parameters["userId"]
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        
                        val roles = rbacService.getUserRoles(userId)
                        call.respond(roles)
                    }
                    
                    post {
                        val userId = call.parameters["userId"]
                            ?: return@post call.respond(HttpStatusCode.BadRequest)
                        
                        val assignment = call.receive<UserRoleAssignment>()
                        rbacService.assignRoles(userId, assignment.roleIds)
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Roles assigned successfully"))
                    }
                }
                
                get("/permissions") {
                    val permissions = rbacService.getAllPermissions()
                    call.respond(permissions)
                }
            }
        }
    }
}
