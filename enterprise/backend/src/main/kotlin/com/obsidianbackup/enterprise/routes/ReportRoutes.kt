package com.obsidianbackup.enterprise.routes

import com.obsidianbackup.enterprise.services.ReportService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class DashboardStats(
    val totalDevices: Int,
    val activeDevices: Int,
    val totalBackups: Int,
    val successfulBackups: Int,
    val failedBackups: Int,
    val totalStorageUsed: Long,
    val averageBackupSize: Long,
    val complianceRate: Double
)

@Serializable
data class BackupSuccessRate(
    val date: String,
    val successCount: Int,
    val failureCount: Int,
    val successRate: Double
)

fun Application.configureReportRoutes() {
    val reportService = ReportService()
    
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/reports") {
                get("/dashboard") {
                    val stats = reportService.getDashboardStats()
                    call.respond(stats)
                }
                
                get("/backup-success-rates") {
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
                    val rates = reportService.getBackupSuccessRates(days)
                    call.respond(rates)
                }
                
                get("/storage-usage") {
                    val breakdown = reportService.getStorageUsageBreakdown()
                    call.respond(breakdown)
                }
                
                get("/compliance") {
                    val report = reportService.getComplianceReport()
                    call.respond(report)
                }
                
                get("/device-health") {
                    val health = reportService.getDeviceHealthReport()
                    call.respond(health)
                }
                
                get("/export/pdf") {
                    val reportType = call.request.queryParameters["type"] ?: "dashboard"
                    val pdfData = reportService.generatePDFReport(reportType)
                    
                    call.response.header("Content-Disposition", "attachment; filename=report.pdf")
                    call.respondBytes(pdfData, ContentType.Application.Pdf)
                }
            }
        }
    }
}
