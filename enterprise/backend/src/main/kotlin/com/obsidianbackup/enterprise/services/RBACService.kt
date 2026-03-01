package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.database.*
import com.obsidianbackup.enterprise.models.Role
import com.obsidianbackup.enterprise.routes.RoleCreateRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class RBACService {
    
    fun getAllRoles(): List<Role> {
        return transaction {
            Roles.selectAll().map { rowToRole(it) }
        }
    }
    
    fun getRole(roleId: String): Role? {
        return transaction {
            Roles.select { Roles.id eq UUID.fromString(roleId) }
                .singleOrNull()
                ?.let { rowToRole(it) }
        }
    }
    
    fun createRole(request: RoleCreateRequest): Role {
        return transaction {
            val orgId = Organizations.selectAll().limit(1).first()[Organizations.id].value
            
            val roleId = Roles.insertAndGetId {
                it[name] = request.name
                it[permissions] = Json.encodeToString(request.permissions)
                it[organizationId] = orgId
                it[description] = request.description
            }
            
            getRole(roleId.value.toString())!!
        }
    }
    
    fun deleteRole(roleId: String) {
        transaction {
            val uuid = UUID.fromString(roleId)
            UserRoles.deleteWhere { UserRoles.roleId eq uuid }
            Roles.deleteWhere { Roles.id eq uuid }
        }
    }
    
    fun getUserRoles(userId: String): List<Role> {
        return transaction {
            UserRoles.innerJoin(Roles)
                .select { UserRoles.userId eq UUID.fromString(userId) }
                .map { rowToRole(it) }
        }
    }
    
    fun assignRoles(userId: String, roleIds: List<String>) {
        transaction {
            val userUUID = UUID.fromString(userId)
            
            // Remove existing roles
            UserRoles.deleteWhere { UserRoles.userId eq userUUID }
            
            // Assign new roles
            roleIds.forEach { roleId ->
                UserRoles.insert {
                    it[UserRoles.userId] = userUUID
                    it[UserRoles.roleId] = UUID.fromString(roleId)
                }
            }
        }
    }
    
    fun getAllPermissions(): List<String> {
        return listOf(
            "devices:read",
            "devices:write",
            "devices:delete",
            "devices:wipe",
            "policies:read",
            "policies:write",
            "policies:delete",
            "policies:enforce",
            "audit:read",
            "audit:export",
            "reports:read",
            "reports:export",
            "users:read",
            "users:write",
            "users:delete",
            "roles:read",
            "roles:write",
            "roles:delete",
            "organization:read",
            "organization:write"
        )
    }
    
    private fun rowToRole(row: ResultRow): Role {
        return Role(
            id = row[Roles.id].value.toString(),
            name = row[Roles.name],
            permissions = Json.decodeFromString(row[Roles.permissions]),
            organizationId = row[Roles.organizationId].value.toString(),
            description = row[Roles.description]
        )
    }
}
