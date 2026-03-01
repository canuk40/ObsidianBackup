package com.obsidianbackup.enterprise.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val code: Int
)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Internal Server Error",
                    message = cause.message ?: "Unknown error",
                    code = 500
                )
            )
        }
        
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "Bad Request",
                    message = cause.message ?: "Invalid request",
                    code = 400
                )
            )
        }
        
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = "Unauthorized",
                    message = "Authentication required",
                    code = 401
                )
            )
        }
        
        status(HttpStatusCode.Forbidden) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = "Forbidden",
                    message = "Insufficient permissions",
                    code = 403
                )
            )
        }
    }
}
