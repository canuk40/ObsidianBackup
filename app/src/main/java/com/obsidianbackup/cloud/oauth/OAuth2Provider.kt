// cloud/oauth/OAuth2Provider.kt
package com.obsidianbackup.cloud.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.obsidianbackup.crypto.KeystoreManager
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Base class for OAuth2 provider implementations
 * Supports multi-account management and token refresh
 */
abstract class OAuth2Provider(
    protected val context: Context,
    protected val keystoreManager: KeystoreManager,
    protected val logger: ObsidianLogger
) {
    abstract val providerId: String
    abstract val displayName: String
    abstract val authorizationEndpoint: String
    abstract val tokenEndpoint: String
    abstract val clientId: String
    abstract val clientSecret: String
    abstract val scopes: List<String>
    abstract val redirectUri: String

    protected val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Build authorization URL for OAuth2 flow
     */
    fun buildAuthorizationUrl(state: String = UUID.randomUUID().toString()): Uri {
        return Uri.parse(authorizationEndpoint).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .appendQueryParameter("state", state)
            .appendQueryParameter("access_type", "offline")
            .build()
    }

    /**
     * Get authorization intent for launching browser
     */
    fun getAuthorizationIntent(state: String = UUID.randomUUID().toString()): Intent {
        val authUrl = buildAuthorizationUrl(state)
        return Intent(Intent.ACTION_VIEW, authUrl)
    }

    /**
     * Exchange authorization code for access token
     */
    suspend fun exchangeCodeForToken(code: String, accountId: String = "default"): OAuth2Result<OAuth2Token> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", redirectUri)
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .build()

                val request = Request.Builder()
                    .url(tokenEndpoint)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext OAuth2Result.Error("Empty response")

                if (!response.isSuccessful) {
                    logger.e(TAG, "Token exchange failed: $responseBody")
                    return@withContext OAuth2Result.Error("Token exchange failed: ${response.code}")
                }

                val json = JSONObject(responseBody)
                val token = OAuth2Token(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token"),
                    expiresIn = json.optLong("expires_in", 3600),
                    tokenType = json.optString("token_type", "Bearer"),
                    scope = json.optString("scope", scopes.joinToString(" ")),
                    issuedAt = System.currentTimeMillis()
                )

                storeToken(accountId, token)

                logger.i(TAG, "Successfully exchanged code for token")
                OAuth2Result.Success(token)
            } catch (e: Exception) {
                logger.e(TAG, "Token exchange error", e)
                OAuth2Result.Error("Token exchange failed: ${e.message}")
            }
        }
    }

    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshToken(accountId: String = "default"): OAuth2Result<OAuth2Token> {
        return withContext(Dispatchers.IO) {
            try {
                val currentToken = getStoredToken(accountId) ?: return@withContext OAuth2Result.Error("No stored token")

                if (currentToken.refreshToken.isEmpty()) {
                    return@withContext OAuth2Result.Error("No refresh token available")
                }

                val requestBody = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", currentToken.refreshToken)
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .build()

                val request = Request.Builder()
                    .url(tokenEndpoint)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext OAuth2Result.Error("Empty response")

                if (!response.isSuccessful) {
                    logger.e(TAG, "Token refresh failed: $responseBody")
                    return@withContext OAuth2Result.Error("Token refresh failed: ${response.code}")
                }

                val json = JSONObject(responseBody)
                val newToken = OAuth2Token(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token", currentToken.refreshToken),
                    expiresIn = json.optLong("expires_in", 3600),
                    tokenType = json.optString("token_type", "Bearer"),
                    scope = json.optString("scope", currentToken.scope),
                    issuedAt = System.currentTimeMillis()
                )

                storeToken(accountId, newToken)

                logger.i(TAG, "Successfully refreshed token")
                OAuth2Result.Success(newToken)
            } catch (e: Exception) {
                logger.e(TAG, "Token refresh error", e)
                OAuth2Result.Error("Token refresh failed: ${e.message}")
            }
        }
    }

    /**
     * Get valid access token, refreshing if necessary
     */
    suspend fun getValidToken(accountId: String = "default"): OAuth2Result<String> {
        val token = getStoredToken(accountId) ?: return OAuth2Result.Error("Not authenticated")

        return if (token.isExpired()) {
            when (val refreshResult = refreshToken(accountId)) {
                is OAuth2Result.Success -> OAuth2Result.Success(refreshResult.data.accessToken)
                is OAuth2Result.Error -> refreshResult
            }
        } else {
            OAuth2Result.Success(token.accessToken)
        }
    }

    /**
     * Store token securely in keystore
     */
    private suspend fun storeToken(accountId: String, token: OAuth2Token) {
        val tokenKey = "${providerId}_${accountId}_token"
        val tokenJson = JSONObject().apply {
            put("access_token", token.accessToken)
            put("refresh_token", token.refreshToken)
            put("expires_in", token.expiresIn)
            put("token_type", token.tokenType)
            put("scope", token.scope)
            put("issued_at", token.issuedAt)
        }.toString()

        keystoreManager.storeToken(tokenKey, tokenJson)
    }

    /**
     * Retrieve stored token from keystore
     */
    suspend fun getStoredToken(accountId: String = "default"): OAuth2Token? {
        return try {
            val tokenKey = "${providerId}_${accountId}_token"
            val tokenJson = keystoreManager.getToken(tokenKey) ?: return null

            val json = JSONObject(tokenJson)
            OAuth2Token(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token", ""),
                expiresIn = json.getLong("expires_in"),
                tokenType = json.getString("token_type"),
                scope = json.getString("scope"),
                issuedAt = json.getLong("issued_at")
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to retrieve token", e)
            null
        }
    }

    /**
     * List all accounts for this provider
     */
    suspend fun listAccounts(): List<String> {
        // Implementation depends on how we store account list
        // For now, simplified version
        return listOf("default")
    }

    /**
     * Remove account and its token
     */
    suspend fun removeAccount(accountId: String = "default") {
        val tokenKey = "${providerId}_${accountId}_token"
        keystoreManager.deleteToken(tokenKey)
        logger.i(TAG, "Removed account: $accountId")
    }

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(accountId: String = "default"): Boolean {
        return getStoredToken(accountId) != null
    }

    companion object {
        private const val TAG = "OAuth2Provider"
    }
}

/**
 * OAuth2 token data class
 */
data class OAuth2Token(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
    val scope: String,
    val issuedAt: Long
) {
    fun isExpired(): Boolean {
        val expirationTime = issuedAt + (expiresIn * 1000)
        val bufferTime = 5 * 60 * 1000 // 5 minutes buffer
        return System.currentTimeMillis() >= (expirationTime - bufferTime)
    }
}

/**
 * OAuth2 operation result
 */
sealed class OAuth2Result<out T> {
    data class Success<T>(val data: T) : OAuth2Result<T>()
    data class Error(val message: String) : OAuth2Result<Nothing>()
}
