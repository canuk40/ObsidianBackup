package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.database.*
import com.obsidianbackup.enterprise.models.*
import com.obsidianbackup.enterprise.routes.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReportService {
    
    fun getDashboardStats(): DashboardStats {
        return transaction {
            val totalDevices = Devices.selectAll().count().toInt()
            val activeDevices = Devices.select { Devices.status eq DeviceStatus.ACTIVE.name }.count().toInt()
            
            val totalBackups = BackupReports.selectAll().count().toInt()
            val successfulBackups = BackupReports.select { BackupReports.status eq BackupStatus.COMPLETED.name }.count().toInt()
            val failedBackups = BackupReports.select { BackupReports.status eq BackupStatus.FAILED.name }.count().toInt()
            
            val totalStorageUsed = BackupReports
                .slice(BackupReports.bytesBackedUp.sum())
                .selectAll()
                .first()[BackupReports.bytesBackedUp.sum()] ?: 0L
            
            val averageBackupSize = if (totalBackups > 0) totalStorageUsed / totalBackups else 0L
            
            val compliantDevices = Devices.selectAll().count { device ->
                val complianceStatus = kotlinx.serialization.json.Json.decodeFromString<ComplianceStatus>(
                    device[Devices.complianceStatus]
                )
                complianceStatus.isCompliant
            }
            val complianceRate = if (totalDevices > 0) compliantDevices.toDouble() / totalDevices else 0.0
            
            DashboardStats(
                totalDevices = totalDevices,
                activeDevices = activeDevices,
                totalBackups = totalBackups,
                successfulBackups = successfulBackups,
                failedBackups = failedBackups,
                totalStorageUsed = totalStorageUsed,
                averageBackupSize = averageBackupSize,
                complianceRate = complianceRate
            )
        }
    }
    
    fun getBackupSuccessRates(days: Int): List<BackupSuccessRate> {
        return transaction {
            val startDate = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
            
            val backups = BackupReports
                .select { BackupReports.startedAt greater startDate }
                .orderBy(BackupReports.startedAt, SortOrder.ASC)
                .toList()
            
            backups.groupBy { it[BackupReports.startedAt].toString().substringBefore("T") }
                .map { (date, reports) ->
                    val successCount = reports.count { it[BackupReports.status] == BackupStatus.COMPLETED.name }
                    val failureCount = reports.count { it[BackupReports.status] == BackupStatus.FAILED.name }
                    val total = successCount + failureCount
                    
                    BackupSuccessRate(
                        date = date,
                        successCount = successCount,
                        failureCount = failureCount,
                        successRate = if (total > 0) successCount.toDouble() / total else 0.0
                    )
                }
        }
    }
    
    fun getStorageUsageBreakdown(): Map<String, Long> {
        return transaction {
            val deviceUsage = Devices.innerJoin(BackupReports)
                .slice(Devices.name, BackupReports.bytesBackedUp.sum())
                .selectAll()
                .groupBy(Devices.id)
                .associate { 
                    it[Devices.name] to (it[BackupReports.bytesBackedUp.sum()] ?: 0L)
                }
            
            deviceUsage
        }
    }
    
    fun getComplianceReport(): Map<String, Any> {
        return transaction {
            val devices = Devices.selectAll().toList()
            
            val compliant = devices.count { device ->
                val status = kotlinx.serialization.json.Json.decodeFromString<ComplianceStatus>(
                    device[Devices.complianceStatus]
                )
                status.isCompliant
            }
            
            mapOf(
                "totalDevices" to devices.size,
                "compliantDevices" to compliant,
                "nonCompliantDevices" to (devices.size - compliant),
                "complianceRate" to if (devices.isNotEmpty()) compliant.toDouble() / devices.size else 0.0
            )
        }
    }
    
    fun getDeviceHealthReport(): List<Map<String, Any>> {
        return transaction {
            Devices.selectAll().map { device ->
                mapOf(
                    "deviceId" to device[Devices.id].value.toString(),
                    "deviceName" to device[Devices.name],
                    "status" to device[Devices.status],
                    "lastSync" to (device[Devices.lastSyncAt]?.toString() ?: "Never"),
                    "platform" to device[Devices.platform],
                    "osVersion" to device[Devices.osVersion]
                )
            }
        }
    }
    
    fun generatePDFReport(reportType: String): ByteArray {
        // M-13: Real PDF generation using Apache PDFBox
        val document = org.apache.pdfbox.pdmodel.PDDocument()
        return try {
            val page = org.apache.pdfbox.pdmodel.PDPage(
                org.apache.pdfbox.pdmodel.common.PDRectangle.A4
            )
            document.addPage(page)

            val contentStream = org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)
            contentStream.use { cs ->
                val titleFont = org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD
                )
                val bodyFont = org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA
                )

                // Title
                cs.beginText()
                cs.setFont(titleFont, 18f)
                cs.newLineAtOffset(50f, 770f)
                cs.showText("ObsidianBackup Enterprise Report")
                cs.endText()

                // Report type
                cs.beginText()
                cs.setFont(titleFont, 14f)
                cs.newLineAtOffset(50f, 740f)
                cs.showText("Type: $reportType")
                cs.endText()

                // Generated timestamp
                cs.beginText()
                cs.setFont(bodyFont, 10f)
                cs.newLineAtOffset(50f, 715f)
                cs.showText("Generated: ${Instant.now()}")
                cs.endText()

                // Separator line
                cs.moveTo(50f, 700f)
                cs.lineTo(545f, 700f)
                cs.stroke()

                // Report body from data
                val stats = try { getDashboardStats() } catch (_: Exception) { null }
                if (stats != null) {
                    var y = 680f
                    for (line in listOf(
                        "Total Devices:       ${stats.totalDevices}",
                        "Active Devices:      ${stats.activeDevices}",
                        "Compliant Devices:   ${stats.compliantDevices}",
                        "Total Backups:       ${stats.totalBackups}",
                        "Successful Backups:  ${stats.successfulBackups}",
                        "Failed Backups:      ${stats.failedBackups}",
                        "Total Storage Used:  ${stats.totalStorageUsed} bytes"
                    )) {
                        cs.beginText()
                        cs.setFont(bodyFont, 11f)
                        cs.newLineAtOffset(50f, y)
                        cs.showText(line)
                        cs.endText()
                        y -= 20f
                    }
                }
            }

            val outputStream = java.io.ByteArrayOutputStream()
            document.save(outputStream)
            outputStream.toByteArray()
        } finally {
            document.close()
        }
    }
}
