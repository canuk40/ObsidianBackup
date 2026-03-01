package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for Organization entity.
 * 
 * **Purpose:**
 * - CRUD operations for organizations (tenants)
 * - Quota and statistics queries
 * - Subscription tier queries
 * - Slug-based lookups
 * 
 * **Multi-Tenancy:**
 * - Organizations are the root tenant entity
 * - All other entities reference organization_id
 * - RLS policies enforce tenant isolation
 */
@Repository
interface OrganizationRepository : JpaRepository<Organization, UUID> {
    
    /**
     * Find organization by slug.
     * 
     * **Use Case:** Tenant identification from subdomain or URL path
     * Example: acme.obsidianbackup.com → slug "acme"
     * 
     * @param slug Organization slug (unique identifier)
     * @return Organization or null
     */
    fun findBySlug(slug: String): Organization?
    
    /**
     * Find all active organizations.
     * 
     * **Use Case:** Admin dashboard showing all active tenants
     * 
     * @param enabled Enabled status (default: true)
     * @return List of active organizations
     */
    fun findByEnabled(enabled: Boolean = true): List<Organization>
    
    /**
     * Find organizations by plan type.
     * 
     * **Use Case:** Marketing analysis, feature usage tracking
     * 
     * @param planType Plan type (FREE, PROFESSIONAL, ENTERPRISE)
     * @return List of organizations with plan type
     */
    fun findByPlanType(planType: Organization.PlanType): List<Organization>
    
    /**
     * Check if slug exists (for uniqueness validation).
     * 
     * **Use Case:** Registration form validation
     * 
     * @param slug Organization slug
     * @return True if slug already exists
     */
    fun existsBySlug(slug: String): Boolean
    
    /**
     * Check if organization name exists (for uniqueness validation).
     * 
     * **Use Case:** Prevent duplicate organization names
     * 
     * @param name Organization name
     * @return True if name already exists
     */
    fun existsByName(name: String): Boolean
    
    /**
     * Count total organizations.
     * 
     * **Use Case:** System statistics
     * 
     * @return Total organization count
     */
    override fun count(): Long
    
    /**
     * Count organizations by plan type.
     * 
     * **Use Case:** Business metrics (conversion rates, upgrades)
     * 
     * @param planType Plan type
     * @return Count of organizations with plan type
     */
    fun countByPlanType(planType: Organization.PlanType): Long
    
    /**
     * Count active organizations.
     * 
     * **Use Case:** Active tenant metrics
     * 
     * @param enabled Enabled status
     * @return Count of active organizations
     */
    fun countByEnabled(enabled: Boolean = true): Long
    
    /**
     * Get organization statistics by plan type.
     * 
     * **Use Case:** Admin dashboard showing plan distribution
     * 
     * @return List of (planType, count) pairs
     */
    @Query(
        "SELECT o.planType, COUNT(o) FROM Organization o " +
        "GROUP BY o.planType " +
        "ORDER BY COUNT(o) DESC"
    )
    fun getPlanTypeStatistics(): List<Array<Any>>
    
    /**
     * Find organizations approaching device quota.
     * 
     * **Use Case:** Proactive upsell notifications
     * 
     * @param utilizationThreshold Utilization percentage (e.g., 0.8 for 80%)
     * @return List of organizations near quota
     */
    @Query(
        "SELECT o FROM Organization o " +
        "WHERE (SELECT COUNT(d) FROM Device d WHERE d.organizationId = o.id) >= (o.maxDevices * :threshold)"
    )
    fun findOrganizationsNearDeviceQuota(@Param("threshold") utilizationThreshold: Double): List<Organization>
    
    /**
     * Find organizations approaching user quota.
     * 
     * **Use Case:** Proactive upsell notifications
     * 
     * @param utilizationThreshold Utilization percentage (e.g., 0.8 for 80%)
     * @return List of organizations near quota
     */
    @Query(
        "SELECT o FROM Organization o " +
        "WHERE (SELECT COUNT(u) FROM User u WHERE u.organizationId = o.id) >= (o.maxUsers * :threshold)"
    )
    fun findOrganizationsNearUserQuota(@Param("threshold") utilizationThreshold: Double): List<Organization>
}
