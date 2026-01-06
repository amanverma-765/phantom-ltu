package com.riva.mods.domain.repository

import com.riva.mods.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String, nonce: String): Result<User>
    suspend fun refreshToken(): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout()
    val isLoggedIn: Flow<Boolean>
}
