package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.InstallationState
import com.navi.phantom.domain.model.UninstallState
import kotlinx.coroutines.flow.Flow

interface ApkInstallationProvider {
    fun install(path: String, appName: String): Flow<InstallationState>
    fun uninstall(packageName: String): Flow<UninstallState>
    suspend fun cleanupOrphanedSessions()
}
