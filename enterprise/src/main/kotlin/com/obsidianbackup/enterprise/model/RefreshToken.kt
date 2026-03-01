package com.obsidianbackup.enterprise.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

/**
 * RefreshToken entity for database-backed token management.
 * 
 * **Table:** refresh_tokens
 * **Schema:** V1__initial_schema.sql
 * 
 * **Purpose:**
 * - Store refresh tokens for validation and revocation
 * - Track token usage and device information
 * - Support audit trail for security events
 * - Enable "logout all devices" functionality
 * 
 * **Relationships:**
 * - ManyToOne: Organization (multi-tenant)
 * - ManyToOne: User (token owner)
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_tokens_org", columnList = "organization_id"),
        Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
        Index(name = "idx_refresh_tokens_token", columnList = "token"),
        Index(name = "idx_refresh_tokens_expires", columnList = "expires_at"),
        Index(name = "idx_refresh_tokens_revoked", columnList = "revoked")
    ]
)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(name = "token", nullable = false, unique = true, length = 500)
    val token: String,
    
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    
    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false,
    
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
    
    @Column(name = "revoked_reason", length = 255)
    var revokedReason: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    val deviceInfo: Map<String, Any>? = null,
    
    @Column(name = "ip_address")
    val ipAddress: String? = null
)
