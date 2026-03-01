package com.obsidianbackup.enterprise

import com.obsidianbackup.enterprise.auth.configureAuth
import com.obsidianbackup.enterprise.database.DatabaseFactory
import com.obsidianbackup.enterprise.plugins.*
import com.obsidianbackup.enterprise.routes.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()
    
    // Configure plugins
    configureSerialization()
    configureCORS()
    configureAuth()
    configureLogging()
    configureStatusPages()
    
    // Configure routes
    configureRouting()
    configureAuthRoutes()
    configureDeviceRoutes()
    configurePolicyRoutes()
    configureAuditRoutes()
    configureReportRoutes()
    configureRBACRoutes()
}
