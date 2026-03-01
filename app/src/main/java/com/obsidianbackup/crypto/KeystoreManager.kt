// crypto/KeystoreManager.kt
package com.obsidianbackup.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.obsidianbackup.logging.ObsidianLogger
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeystoreManager(
    private val context: Context,
    private val logger: ObsidianLogger
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun storeToken(key: String, token: String) {
        try {
            encryptedPrefs.edit().putString(key, token).apply()
            logger.d(TAG, "Token stored securely for key: $key")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to store token", e)
            throw e
        }
    }

    fun getToken(key: String): String? {
        return try {
            encryptedPrefs.getString(key, null)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to retrieve token", e)
            null
        }
    }

    fun deleteToken(key: String) {
        try {
            encryptedPrefs.edit().remove(key).apply()
            logger.d(TAG, "Token deleted for key: $key")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete token", e)
        }
    }

    companion object {
        private const val TAG = "KeystoreManager"
    }
}
