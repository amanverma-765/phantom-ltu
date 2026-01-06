package com.riva.mods.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleIdTokenRequest(
    @SerialName("id_token")
    val idToken: String,
    @SerialName("nonce")
    val nonce: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)
