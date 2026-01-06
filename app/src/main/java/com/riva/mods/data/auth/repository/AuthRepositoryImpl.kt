package com.riva.mods.data.auth.repository

import com.riva.mods.core.storage.TokenManager
import com.riva.mods.data.auth.mapper.toDomain
import com.riva.mods.data.auth.mapper.toDomainUser
import com.riva.mods.data.auth.remote.AuthApiService
import com.riva.mods.domain.model.User
import com.riva.mods.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepositoryImpl(
    private val api: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedIn

    override suspend fun loginWithGoogle(idToken: String, nonce: String): Result<User> {
        return try {
            val response = api.loginWithGoogle(idToken, nonce)
            tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
            Result.success(response.data.toDomainUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<User> {
        return try {
            val currentRefreshToken = tokenManager.refreshToken.first()
                ?: return Result.failure(IllegalStateException("No refresh token available"))

            val response = api.refreshToken(currentRefreshToken)
            tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
            Result.success(response.data.toDomainUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = api.getCurrentUser()
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {
            // Ignore logout API errors
        } finally {
            tokenManager.clearTokens()
        }
    }
}
