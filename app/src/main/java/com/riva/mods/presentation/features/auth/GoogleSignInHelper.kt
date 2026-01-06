package com.riva.mods.presentation.features.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom
import java.util.Base64

data class GoogleUser(
    val idToken: String,
    val nonce: String,
    val id: String,
    val displayName: String?,
    val email: String?,
    val profilePictureUri: String?
)

sealed class SignInResult {
    data class Success(val user: GoogleUser) : SignInResult()
    data class Cancelled(val message: String) : SignInResult()
    data class NoCredentials(val message: String) : SignInResult()
    data class Error(val message: String, val exception: Exception? = null) : SignInResult()
}

class GoogleSignInHelper(
    private val context: Context,
    private val webClientId: String
) {
    private val credentialManager = CredentialManager.create(context)
    private var currentNonce: String = ""

    companion object {
        private const val TAG = "GoogleSignInHelper"
    }

    /**
     * Generates a secure nonce for sign-in requests
     */
    private fun generateNonce(byteLength: Int = 32): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom.getInstanceStrong().nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    /**
     * Sign in with Google using the button flow
     * Shows Google's sign-in dialog for explicit user action
     */
    suspend fun signInWithGoogleButton(): SignInResult {
        currentNonce = generateNonce()
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(currentNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return executeSignIn(request)
    }

    private suspend fun executeSignIn(request: GetCredentialRequest): SignInResult {
        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            handleSignInResult(result)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials available", e)
            SignInResult.NoCredentials("No Google accounts available on this device")
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user", e)
            SignInResult.Cancelled("Sign-in was cancelled")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error getting credentials", e)
            SignInResult.Error("Failed to get credentials: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            SignInResult.Error("An unexpected error occurred: ${e.message}", e)
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): SignInResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                        val user = GoogleUser(
                            idToken = googleIdTokenCredential.idToken,
                            nonce = currentNonce,
                            id = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName,
                            email = googleIdTokenCredential.id, // ID is the email for Google
                            profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                        )

                        Log.i(TAG, "Sign-in successful for: ${user.displayName}")
                        SignInResult.Success(user)

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google ID token", e)
                        SignInResult.Error("Failed to parse sign-in response", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    SignInResult.Error("Unexpected credential type received")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential class: ${credential::class.java.simpleName}")
                SignInResult.Error("Unexpected credential type received")
            }
        }
    }

    /**
     * Sign out - clears credential state
     * Call this when user explicitly signs out
     */
    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.i(TAG, "Successfully signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
        }
    }
}