package com.navi.phantom.features.apps.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.error.AppError
import com.navi.phantom.domain.model.InstalledApp
import com.navi.phantom.domain.usecase.InstalledAppUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(private val installedAppUseCase: InstalledAppUseCase) : ViewModel() {
    private val log = Logger.withTag("AppViewModel")

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun onEvent(event: AppUiEvent) {
        when (event) {
            is AppUiEvent.GetAllInstalledApps -> getAllInstalledApps()
            is AppUiEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is AppUiEvent.SelectApp -> selectApp(event.app)
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredApps = installedAppUseCase.filterApps(state.allInstalledApps, query)
            )
        }
    }

    private fun selectApp(app: InstalledApp) {
        log.d { "App selected: ${app.appName} (${app.packageName})" }
    }

    private fun getAllInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, errorMessage = null) }
            installedAppUseCase.getAllInstalledApps()
                .onSuccess { apps ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingApps = false,
                            errorMessage = null,
                            allInstalledApps = apps,
                            filteredApps = installedAppUseCase.filterApps(apps, state.searchQuery)
                        )
                    }
                }
                .onFailure { throwable ->
                    log.e(throwable) { "getAllInstalledApps failed" }
                    val message = when (throwable) {
                        is SecurityException -> AppError.PermissionDenied.message
                        else -> AppError.Unknown.message
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingApps = false,
                            errorMessage = message
                        )
                    }
                }
        }
    }
}