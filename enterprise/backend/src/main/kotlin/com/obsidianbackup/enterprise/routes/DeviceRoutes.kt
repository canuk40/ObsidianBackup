package com.obsidianbackup.enterprise.routes

import com.obsidianbackup.enterprise.models.Device
import com.obsidianbackup.enterprise.services.DeviceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegistrationRequest(
    val name: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String
)

@Serializable
data class RemoteWipeRequest(
    val deviceId: String,
    val reason: String
)

fun Application.configureDeviceRoutes() {
    val deviceService = DeviceService()
    
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/devices") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    
                    val devices = deviceService.getDevicesByUser(userId)
                    call.respond(devices)
                }
                
                post("/register") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    
                    val request = call.receive<DeviceRegistrationRequest>()
                    val device = deviceService.registerDevice(userId, request)
                    call.respond(HttpStatusCode.Created, device)
                }
                
                get("/{deviceId}") {
                    val deviceId = call.parameters["deviceId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    
                    val device = deviceService.getDevice(deviceId)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    
                    call.respond(device)
                }
                
                post("/{deviceId}/wipe") {
                    val deviceId = call.parameters["deviceId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    
                    val request = call.receive<RemoteWipeRequest>()
                    deviceService.wipeDevice(deviceId, request.reason)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Device wipe initiated"))
                }
                
                get("/{deviceId}/compliance") {
                    val deviceId = call.parameters["deviceId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    
                    val compliance = deviceService.checkCompliance(deviceId)
                    call.respond(compliance)
                }
            }
        }
    }
}
