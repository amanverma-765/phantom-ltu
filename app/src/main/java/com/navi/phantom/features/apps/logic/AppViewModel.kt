package com.navi.phantom.features.apps.logic

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navi.phantom.domain.errors.AppError
import com.navi.phantom.domain.models.InstalledApp
import com.navi.phantom.domain.repository.InstalledAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(private val installedAppRepository: InstalledAppRepository) : ViewModel() {
    companion object {
        const val TAG = "AppViewModel"
    }

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
                filteredApps = filterApps(state.allInstalledApps, query)
            )
        }
    }

    private fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        return if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }
    }

    private fun selectApp(app: InstalledApp) {
        Log.d(TAG, "App selected: ${app.appName} (${app.packageName})")
    }

    private fun getAllInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }
            installedAppRepository.getAllInstalledApps()
                .onSuccess { apps ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            allInstalledApps = apps,
                            filteredApps = filterApps(apps, state.searchQuery)
                        )
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG, "getAllInstalledApps failed", throwable)
                    val errorMessage = when (throwable) {
                        is SecurityException ->
                            AppError.PermissionDenied.message
                        else ->
                            AppError.Unknown.message
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMsg = errorMessage
                        )
                    }
                }
        }
    }
}