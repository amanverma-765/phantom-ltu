package com.riva.mods.data.auth.remote

import com.riva.mods.data.auth.dto.AuthResponse
import com.riva.mods.data.auth.dto.GoogleIdTokenRequest
import com.riva.mods.data.auth.dto.RefreshTokenRequest
import com.riva.mods.data.auth.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApiService(
    private val publicClient: HttpClient,
    private val secureClient: HttpClient,
    private val baseUrl: String
) {
    companion object {
        private const val AUTH_PATH = "/api/v1/auth"
    }

    // Public endpoints (no auth needed)
    suspend fun loginWithGoogle(idToken: String, nonce: String): AuthResponse {
        return publicClient.post("$baseUrl$AUTH_PATH/google/id-token") {
            setBody(GoogleIdTokenRequest(idToken, nonce))
        }.body()
    }

    suspend fun refreshToken(refreshToken: String): AuthResponse {
        return publicClient.post("$baseUrl$AUTH_PATH/refresh") {
            setBody(RefreshTokenRequest(refreshToken))
        }.body()
    }

    // Protected endpoints (auto auth via secureClient)
    suspend fun getCurrentUser(): UserResponse {
        return secureClient.get("$baseUrl$AUTH_PATH/me").body()
    }

    suspend fun logout() {
        secureClient.post("$baseUrl$AUTH_PATH/logout")
    }
}