package com.riva.mods.presentation.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.mods.domain.repository.AuthRepository
import com.riva.mods.presentation.navigation.Navigator
import com.riva.mods.presentation.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val googleSignInHelper: GoogleSignInHelper,
    private val authRepository: AuthRepository,
    private val navigator: Navigator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.SignInWithGoogle -> signInWithGoogle()
            is AuthUiEvent.SignOut -> signOut()
            is AuthUiEvent.ClearError -> clearError()
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }

            when (val result = googleSignInHelper.signInWithGoogleButton()) {
                is SignInResult.Success -> {
                    // Send idToken and nonce to backend for verification
                    loginWithBackend(result.user.idToken, result.user.nonce)
                }

                is SignInResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                }

                is SignInResult.NoCredentials -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMsg = result.message)
                    }
                }

                is SignInResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMsg = result.message)
                    }
                }
            }
        }
    }

    private suspend fun loginWithBackend(idToken: String, nonce: String) {
        authRepository.loginWithGoogle(idToken, nonce)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSignedIn = true
                    )
                }
                navigator.switchTab(Screen.Home)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMsg = error.message ?: "Login failed"
                    )
                }
            }
    }

    private fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            googleSignInHelper.signOut()
            authRepository.logout()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSignedIn = false,
                    user = null
                )
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMsg = null) }
    }
}