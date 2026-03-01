package com.obsidianbackup.enterprise.repository

import com.obsidianbackup.enterprise.model.Policy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for Policy entity.
 * 
 * **Research Citation:** Finding 4 - Policy Enforcement Engine
 * 
 * **Purpose:**
 * - Policy CRUD operations
 * - Active policy queries
 * - Priority-based policy selection
 * - Compliance framework filtering
 * - Policy assignment queries
 */
@Repository
interface PolicyRepository : JpaRepository<Policy, UUID> {
    
    /**
     * Find all policies for organization.
     * 
     * **Use Case:** Policy management dashboard
     * 
     * @param organizationId Organization UUID
     * @return List of policies
     */
    fun findByOrganizationId(organizationId: UUID): List<Policy>
    
    /**
     * Find active (enabled) policies for organization.
     * 
     * **Use Case:** Compliance evaluation (only evaluate active policies)
     * 
     * @param organizationId Organization UUID
     * @param enabled Enabled status (default: true)
     * @return List of active policies
     */
    fun findByOrganizationIdAndEnabled(organizationId: UUID, enabled: Boolean = true): List<Policy>
    
    /**
     * Find policies by type.
     * 
     * **Use Case:** Filter policies by category
     * 
     * @param organizationId Organization UUID
     * @param policyType Policy type
     * @return List of policies
     */
    fun findByOrganizationIdAndPolicyType(
        organizationId: UUID,
        policyType: Policy.PolicyType
    ): List<Policy>
    
    /**
     * Find active policies by type.
     * 
     * **Use Case:** Evaluate specific policy category
     * 
     * @param organizationId Organization UUID
     * @param policyType Policy type
     * @param enabled Enabled status
     * @return List of active policies
     */
    fun findByOrganizationIdAndPolicyTypeAndEnabled(
        organizationId: UUID,
        policyType: Policy.PolicyType,
        enabled: Boolean = true
    ): List<Policy>
    
    /**
     * Find policies by name.
     * 
     * **Use Case:** Policy lookup by name
     * 
     * @param organizationId Organization UUID
     * @param name Policy name
     * @return Policy or null
     */
    fun findByOrganizationIdAndName(organizationId: UUID, name: String): Policy?
    
    /**
     * Find active policies ordered by priority (DESC).
     * 
     * **Research Citation:** Finding 4 - Priority-based policy evaluation
     * 
     * **Use Case:** Policy evaluation (highest priority first)
     * 
     * @param organizationId Organization UUID
     * @param enabled Enabled status
     * @return List of policies ordered by priority
     */
    fun findByOrganizationIdAndEnabledOrderByPriorityDesc(
        organizationId: UUID,
        enabled: Boolean = true
    ): List<Policy>
    
    /**
     * Find policies with compliance framework.
     * 
     * **Use Case:** Compliance reporting (HIPAA, SOC 2, GDPR)
     * 
     * @param organizationId Organization UUID
     * @param framework Compliance framework name
     * @return List of policies
     */
    @Query(
        "SELECT p FROM Policy p " +
        "WHERE p.organizationId = :orgId " +
        "AND :framework = ANY(p.complianceFrameworks)"
    )
    fun findByOrganizationIdAndComplianceFramework(
        @Param("orgId") organizationId: UUID,
        @Param("framework") framework: String
    ): List<Policy>
    
    /**
     * Find policies with auto-remediation enabled.
     * 
     * **Use Case:** Automated compliance remediation
     * 
     * @param organizationId Organization UUID
     * @param autoRemediation Auto-remediation status
     * @return List of policies
     */
    fun findByOrganizationIdAndAutoRemediation(
        organizationId: UUID,
        autoRemediation: Boolean
    ): List<Policy>
    
    /**
     * Check if policy name exists in organization.
     * 
     * **Use Case:** Policy creation validation
     * 
     * @param organizationId Organization UUID
     * @param name Policy name
     * @return True if name exists
     */
    fun existsByOrganizationIdAndName(organizationId: UUID, name: String): Boolean
    
    /**
     * Count policies in organization.
     * 
     * **Use Case:** Organization statistics
     * 
     * @param organizationId Organization UUID
     * @return Policy count
     */
    fun countByOrganizationId(organizationId: UUID): Long
    
    /**
     * Count policies by type.
     * 
     * **Use Case:** Policy distribution metrics
     * 
     * @param organizationId Organization UUID
     * @param policyType Policy type
     * @return Policy count
     */
    fun countByOrganizationIdAndPolicyType(
        organizationId: UUID,
        policyType: Policy.PolicyType
    ): Long
    
    /**
     * Count active policies.
     * 
     * **Use Case:** Active policy metrics
     * 
     * @param organizationId Organization UUID
     * @param enabled Enabled status
     * @return Active policy count
     */
    fun countByOrganizationIdAndEnabled(organizationId: UUID, enabled: Boolean = true): Long
    
    /**
     * Get policy type distribution.
     * 
     * **Use Case:** Admin dashboard showing policy breakdown
     * 
     * @param organizationId Organization UUID
     * @return List of (policyType, count) pairs
     */
    @Query(
        "SELECT p.policyType, COUNT(p) FROM Policy p " +
        "WHERE p.organizationId = :orgId " +
        "GROUP BY p.policyType " +
        "ORDER BY COUNT(p) DESC"
    )
    fun getPolicyTypeDistribution(@Param("orgId") organizationId: UUID): List<Array<Any>>
    
    /**
     * Get compliance framework coverage.
     * 
     * **Use Case:** Compliance dashboard
     * 
     * @param organizationId Organization UUID
     * @return List of (framework, policyCount) pairs
     */
    @Query(
        value = "SELECT unnest(compliance_frameworks) as framework, COUNT(*) as cnt " +
                "FROM policies " +
                "WHERE organization_id = :orgId " +
                "GROUP BY framework " +
                "ORDER BY cnt DESC",
        nativeQuery = true
    )
    fun getComplianceFrameworkCoverage(@Param("orgId") organizationId: UUID): List<Array<Any>>
}
