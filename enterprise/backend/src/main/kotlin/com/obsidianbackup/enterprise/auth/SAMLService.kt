package com.obsidianbackup.enterprise.auth

import com.onelogin.saml2.Auth
import com.onelogin.saml2.settings.Saml2Settings
import com.onelogin.saml2.settings.SettingsBuilder
import kotlinx.serialization.Serializable
import java.util.HashMap

@Serializable
data class SAMLAuthRequest(
    val organizationId: String
)

@Serializable
data class SAMLAuthResponse(
    val email: String,
    val name: String,
    val attributes: Map<String, String>
)

class SAMLService {
    
    fun createAuthRequest(organizationId: String, samlConfig: Map<String, String>): String {
        val settings = buildSettings(samlConfig)
        val auth = Auth(settings, null, null)
        
        return auth.login()
    }
    
    fun processAuthResponse(samlResponse: String, samlConfig: Map<String, String>): SAMLAuthResponse {
        val settings = buildSettings(samlConfig)
        val auth = Auth(settings, null, null)
        
        auth.processResponse()
        
        if (!auth.isAuthenticated) {
            throw Exception("SAML authentication failed: ${auth.lastErrorReason}")
        }
        
        val attributes = auth.attributes
        val email = attributes["email"]?.firstOrNull() ?: throw Exception("Email not found in SAML response")
        val name = attributes["name"]?.firstOrNull() ?: attributes["displayName"]?.firstOrNull() ?: email
        
        return SAMLAuthResponse(
            email = email,
            name = name,
            attributes = attributes.mapValues { it.value.firstOrNull() ?: "" }
        )
    }
    
    private fun buildSettings(config: Map<String, String>): Saml2Settings {
        val samlData = HashMap<String, Any>()
        
        // SP settings
        samlData["sp.entityId"] = config["spEntityId"] ?: "obsidian-enterprise"
        samlData["sp.assertionConsumerService.url"] = config["acsUrl"] ?: "https://enterprise.obsidianbackup.com/saml/acs"
        samlData["sp.singleLogoutService.url"] = config["sloUrl"] ?: "https://enterprise.obsidianbackup.com/saml/slo"
        
        // IdP settings
        samlData["idp.entityId"] = config["idpEntityId"] ?: ""
        samlData["idp.singleSignOnService.url"] = config["idpSsoUrl"] ?: ""
        samlData["idp.singleLogoutService.url"] = config["idpSloUrl"] ?: ""
        samlData["idp.x509cert"] = config["idpCertificate"] ?: ""
        
        // Security settings
        samlData["security.nameIdEncrypted"] = false
        samlData["security.authnRequestsSigned"] = true
        samlData["security.logoutRequestSigned"] = true
        samlData["security.logoutResponseSigned"] = true
        samlData["security.signMetadata"] = true
        samlData["security.wantMessagesSigned"] = true
        samlData["security.wantAssertionsSigned"] = true
        samlData["security.wantNameIdEncrypted"] = false
        
        val builder = SettingsBuilder()
        return builder.fromValues(samlData).build()
    }
}
