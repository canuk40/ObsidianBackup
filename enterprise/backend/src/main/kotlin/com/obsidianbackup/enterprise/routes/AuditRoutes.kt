package com.obsidianbackup.enterprise.routes

import com.obsidianbackup.enterprise.services.AuditService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class AuditQueryParams(
    val startDate: String? = null,
    val endDate: String? = null,
    val userId: String? = null,
    val deviceId: String? = null,
    val action: String? = null,
    val page: Int = 1,
    val pageSize: Int = 50
)

fun Application.configureAuditRoutes() {
    val auditService = AuditService()
    
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/audit") {
                get {
                    val params = AuditQueryParams(
                        startDate = call.request.queryParameters["startDate"],
                        endDate = call.request.queryParameters["endDate"],
                        userId = call.request.queryParameters["userId"],
                        deviceId = call.request.queryParameters["deviceId"],
                        action = call.request.queryParameters["action"],
                        page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                        pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                    )
                    
                    val logs = auditService.queryLogs(params)
                    call.respond(logs)
                }
                
                get("/{logId}") {
                    val logId = call.parameters["logId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    
                    val log = auditService.getLog(logId)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    
                    call.respond(log)
                }
                
                get("/export") {
                    val params = AuditQueryParams(
                        startDate = call.request.queryParameters["startDate"],
                        endDate = call.request.queryParameters["endDate"]
                    )
                    
                    val csvData = auditService.exportLogs(params)
                    call.response.header("Content-Disposition", "attachment; filename=audit-logs.csv")
                    call.respondText(csvData, ContentType.Text.CSV)
                }
            }
        }
    }
}
