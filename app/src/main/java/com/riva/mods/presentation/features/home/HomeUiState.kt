package com.riva.mods.presentation.features.home

import com.riva.mods.domain.model.User

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: String? = null
)
