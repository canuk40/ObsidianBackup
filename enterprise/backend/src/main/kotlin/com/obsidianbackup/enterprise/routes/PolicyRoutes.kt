package com.obsidianbackup.enterprise.routes

import com.obsidianbackup.enterprise.models.Policy
import com.obsidianbackup.enterprise.services.PolicyService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class PolicyCreateRequest(
    val name: String,
    val type: String,
    val config: Map<String, String>,
    val targetDevices: List<String>
)

fun Application.configurePolicyRoutes() {
    val policyService = PolicyService()
    
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/policies") {
                get {
                    val policies = policyService.getAllPolicies()
                    call.respond(policies)
                }
                
                post {
                    val request = call.receive<PolicyCreateRequest>()
                    val policy = policyService.createPolicy(request)
                    call.respond(HttpStatusCode.Created, policy)
                }
                
                get("/{policyId}") {
                    val policyId = call.parameters["policyId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    
                    val policy = policyService.getPolicy(policyId)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    
                    call.respond(policy)
                }
                
                put("/{policyId}") {
                    val policyId = call.parameters["policyId"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                    
                    val request = call.receive<PolicyCreateRequest>()
                    val policy = policyService.updatePolicy(policyId, request)
                        ?: return@put call.respond(HttpStatusCode.NotFound)
                    
                    call.respond(policy)
                }
                
                delete("/{policyId}") {
                    val policyId = call.parameters["policyId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    
                    policyService.deletePolicy(policyId)
                    call.respond(HttpStatusCode.NoContent)
                }
                
                post("/{policyId}/enforce") {
                    val policyId = call.parameters["policyId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    
                    policyService.enforcePolicy(policyId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Policy enforcement initiated"))
                }
            }
        }
    }
}
