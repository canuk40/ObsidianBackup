package com.obsidianbackup.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obsidianbackup.crypto.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudProviderDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_providers")

@Singleton
class CloudProviderRepository @Inject constructor(
    private val context: Context,
    private val keystoreManager: KeystoreManager
) {
    private val dataStore = context.cloudProviderDataStore
    
    object Keys {
        // WebDAV Settings
        val WEBDAV_BASE_URL = stringPreferencesKey("webdav_base_url")
        val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val WEBDAV_USE_DIGEST_AUTH = booleanPreferencesKey("webdav_use_digest_auth")
        
        // Filecoin Settings
        val FILECOIN_ENABLE_DEALS = booleanPreferencesKey("filecoin_enable_deals")
        val FILECOIN_PINNING_SERVICE = stringPreferencesKey("filecoin_pinning_service")
        val FILECOIN_IPFS_GATEWAYS = stringPreferencesKey("filecoin_ipfs_gateways")
    }
    
    // Secure token keys for KeystoreManager
    private object SecureKeys {
        const val WEBDAV_PASSWORD = "webdav_password"
        const val FILECOIN_WEB3_TOKEN = "filecoin_web3_storage_token"
    }
    
    // WebDAV Configuration
    val webdavBaseUrl: Flow<String> = dataStore.data
        .map { preferences -> preferences[Keys.WEBDAV_BASE_URL] ?: "" }
    
    suspend fun setWebdavBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[Keys.WEBDAV_BASE_URL] = url
        }
    }
    
    val webdavUsername: Flow<String> = dataStore.data
        .map { preferences -> preferences[Keys.WEBDAV_USERNAME] ?: "" }
    
    suspend fun setWebdavUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[Keys.WEBDAV_USERNAME] = username
        }
    }
    
    fun getWebdavPassword(): String? {
        return keystoreManager.getToken(SecureKeys.WEBDAV_PASSWORD)
    }
    
    fun setWebdavPassword(password: String) {
        keystoreManager.storeToken(SecureKeys.WEBDAV_PASSWORD, password)
    }
    
    val webdavUseDigestAuth: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.WEBDAV_USE_DIGEST_AUTH] ?: false }
    
    suspend fun setWebdavUseDigestAuth(useDigest: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.WEBDAV_USE_DIGEST_AUTH] = useDigest
        }
    }
    
    // Filecoin Configuration
    fun getFilecoinWeb3Token(): String? {
        return keystoreManager.getToken(SecureKeys.FILECOIN_WEB3_TOKEN)
    }
    
    fun setFilecoinWeb3Token(token: String) {
        keystoreManager.storeToken(SecureKeys.FILECOIN_WEB3_TOKEN, token)
    }
    
    val filecoinIpfsGateways: Flow<List<String>> = dataStore.data
        .map { preferences ->
            preferences[Keys.FILECOIN_IPFS_GATEWAYS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: listOf(
                    "https://dweb.link",
                    "https://ipfs.io",
                    "https://cloudflare-ipfs.com",
                    "https://gateway.pinata.cloud"
                )
        }
    
    suspend fun setFilecoinIpfsGateways(gateways: List<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.FILECOIN_IPFS_GATEWAYS] = gateways.joinToString(",")
        }
    }
    
    val filecoinEnableDeals: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.FILECOIN_ENABLE_DEALS] ?: true }
    
    suspend fun setFilecoinEnableDeals(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.FILECOIN_ENABLE_DEALS] = enable
        }
    }
    
    val filecoinPinningService: Flow<String> = dataStore.data
        .map { preferences -> preferences[Keys.FILECOIN_PINNING_SERVICE] ?: "web3.storage" }
    
    suspend fun setFilecoinPinningService(service: String) {
        dataStore.edit { preferences ->
            preferences[Keys.FILECOIN_PINNING_SERVICE] = service
        }
    }
}
