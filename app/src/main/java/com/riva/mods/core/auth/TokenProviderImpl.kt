package com.riva.mods.core.auth

import android.util.Log
import com.riva.mods.core.storage.TokenManager
import com.riva.mods.data.auth.dto.AuthResponse
import com.riva.mods.data.auth.dto.RefreshTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenProviderImpl(
    private val tokenManager: TokenManager,
    private val authClient: HttpClient,
    private val baseUrl: String
) : TokenProvider {

    companion object {
        private const val TAG = "TokenProvider"
        private const val REFRESH_ENDPOINT = "/api/v1/auth/refresh"
    }

    private val refreshMutex = Mutex()

    override suspend fun getAccessToken(): String? {
        return tokenManager.accessToken.first()
    }

    override suspend fun getRefreshToken(): String? {
        return tokenManager.refreshToken.first()
    }

    override suspend fun refreshTokens(): String? {
        return refreshMutex.withLock {
            try {
                val currentRefreshToken = getRefreshToken()
                if (currentRefreshToken == null) {
                    Log.w(TAG, "No refresh token available")
                    handleRefreshFailure()
                    return@withLock null
                }

                Log.d(TAG, "Attempting token refresh...")

                val response = authClient.post("$baseUrl$REFRESH_ENDPOINT") {
                    setBody(RefreshTokenRequest(currentRefreshToken))
                }.body<AuthResponse>()

                tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                Log.d(TAG, "Token refresh successful")

                response.data.accessToken
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed", e)
                handleRefreshFailure()
                null
            }
        }
    }

    override suspend fun clearTokens() {
        tokenManager.clearTokens()
    }

    private suspend fun handleRefreshFailure() {
        clearTokens()
        AuthEventBus.emitSessionExpired()
    }
}
