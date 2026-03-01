package com.obsidianbackup.enterprise.services

import com.obsidianbackup.enterprise.database.*
import com.obsidianbackup.enterprise.models.User
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.Instant
import java.util.*

class UserService {
    
    fun authenticate(email: String, password: String): User? {
        return transaction {
            val userRow = Users.select { Users.email eq email }.singleOrNull() ?: return@transaction null
            
            val passwordHash = userRow[Users.passwordHash] ?: return@transaction null
            if (!verifyPassword(password, passwordHash)) return@transaction null
            
            val roles = UserRoles
                .innerJoin(Roles)
                .select { UserRoles.userId eq userRow[Users.id].value }
                .map { it[Roles.name] }
            
            Users.update({ Users.id eq userRow[Users.id] }) {
                it[lastLoginAt] = Instant.now()
            }
            
            User(
                id = userRow[Users.id].value.toString(),
                email = userRow[Users.email],
                name = userRow[Users.name],
                roles = roles,
                organizationId = userRow[Users.organizationId].value.toString(),
                isActive = userRow[Users.isActive],
                createdAt = userRow[Users.createdAt].toString(),
                lastLoginAt = Instant.now().toString()
            )
        }
    }
    
    fun findOrCreateSAMLUser(email: String, name: String): User {
        return transaction {
            val existingUser = Users.select { Users.email eq email }.singleOrNull()
            
            if (existingUser != null) {
                val roles = UserRoles
                    .innerJoin(Roles)
                    .select { UserRoles.userId eq existingUser[Users.id].value }
                    .map { it[Roles.name] }
                
                return@transaction User(
                    id = existingUser[Users.id].value.toString(),
                    email = existingUser[Users.email],
                    name = existingUser[Users.name],
                    roles = roles,
                    organizationId = existingUser[Users.organizationId].value.toString(),
                    isActive = existingUser[Users.isActive],
                    createdAt = existingUser[Users.createdAt].toString()
                )
            }
            
            // Create new user
            val orgId = Organizations.selectAll().limit(1).first()[Organizations.id].value
            val userId = Users.insertAndGetId {
                it[Users.email] = email
                it[Users.name] = name
                it[organizationId] = orgId
                it[isActive] = true
                it[createdAt] = Instant.now()
            }
            
            User(
                id = userId.value.toString(),
                email = email,
                name = name,
                roles = emptyList(),
                organizationId = orgId.toString(),
                isActive = true,
                createdAt = Instant.now().toString()
            )
        }
    }
    
    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}
