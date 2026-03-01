package com.obsidianbackup.enterprise.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/obsidian_enterprise"
            username = System.getenv("DATABASE_USER") ?: "postgres"
            password = System.getenv("DATABASE_PASSWORD") ?: "postgres"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        
        Database.connect(HikariDataSource(config))
        
        transaction {
            SchemaUtils.create(
                Users,
                Roles,
                UserRoles,
                Devices,
                Policies,
                DevicePolicies,
                AuditLogs,
                Organizations,
                BackupReports
            )
        }
    }
}
