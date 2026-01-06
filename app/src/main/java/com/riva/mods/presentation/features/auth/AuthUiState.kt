package com.riva.mods.presentation.features.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val user: GoogleUser? = null,
    val errorMsg: String? = null
)