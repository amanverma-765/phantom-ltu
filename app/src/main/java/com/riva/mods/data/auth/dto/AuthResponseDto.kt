package com.riva.mods.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val timestamp: String,
    val message: String,
    @SerialName("request_id")
    val requestId: String = "",
    val data: T
)

@Serializable
data class AuthData(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    val user: UserDto
)

typealias AuthResponse = ApiResponse<AuthData>
typealias UserResponse = ApiResponse<UserDto>

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val role: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val statusCode: Int,
    val details: String? = null
)
