// cloud/OAuth2Manager.kt
package com.obsidianbackup.cloud

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes
import com.obsidianbackup.crypto.KeystoreManager
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.Result
import com.obsidianbackup.model.Result.Success
import com.obsidianbackup.model.Result.Error
import com.obsidianbackup.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OAuth2Manager(
    private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val logger: ObsidianLogger
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // The web client ID allows backend verification of the Google ID token.
            // Value set in app/src/main/res/values/oauth_credentials.xml (gitignored).
            // Copy oauth_credentials.xml.example → oauth_credentials.xml and fill in the
            // Client ID from GCP Console → APIs & Services → Credentials → Web client.
            .requestIdToken(context.getString(R.string.google_oauth_client_id))
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    private val credential: GoogleAccountCredential by lazy {
        GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(task: Task<GoogleSignInAccount>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val account = task.getResult(ApiException::class.java)
                val token = account.idToken ?: return@withContext Result.Error("No ID token")

                // Store the account for credential
                credential.selectedAccount = account.account

                // Encrypt and store the token
                keystoreManager.storeToken("google_drive_token", token)

                logger.i(TAG, "Successfully signed in with Google Drive")
                Result.Success(token)
            } catch (e: ApiException) {
                logger.e(TAG, "Google sign-in failed", e)
                Result.Error("Sign-in failed: ${e.message}")
            } catch (e: Exception) {
                logger.e(TAG, "Unexpected error during sign-in", e)
                Result.Error("Unexpected error: ${e.message}")
            }
        }
    }

    suspend fun getStoredToken(): String? {
        return keystoreManager.getToken("google_drive_token")
    }

    suspend fun refreshTokenIfNeeded(): Result<String> {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                // For simplicity, we'll use the stored token
                // In production, implement proper token refresh
                val token = getStoredToken()
                if (token != null) {
                    Result.Success(token)
                } else {
                    Result.Error("No stored token")
                }
            } else {
                Result.Error("No signed-in account")
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to refresh token", e)
            Result.Error("Token refresh failed: ${e.message}")
        }
    }

    fun getDriveCredential(): GoogleAccountCredential {
        return credential
    }

    suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut().await()
                keystoreManager.deleteToken("google_drive_token")
                credential.selectedAccount = null
                logger.i(TAG, "Successfully signed out from Google Drive")
                Result.Success(Unit)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to sign out", e)
                Result.Error("Sign-out failed: ${e.message}")
            }
        }
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    companion object {
        private const val TAG = "OAuth2Manager"
    }
}

// Extension function to await Task
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
