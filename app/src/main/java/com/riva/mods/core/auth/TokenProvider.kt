package com.riva.mods.core.auth

interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun refreshTokens(): String?
    suspend fun clearTokens()
}
