package com.riva.mods.presentation.features.auth

sealed class AuthUiEvent {
    data object SignInWithGoogle : AuthUiEvent()
    data object SignOut : AuthUiEvent()
    data object ClearError : AuthUiEvent()
}