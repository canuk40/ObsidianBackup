package com.obsidianbackup.enterprise.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("ObsidianBackup Enterprise API v1.0.0")
        }
        
        get("/health") {
            call.respondText("OK")
        }
    }
}
