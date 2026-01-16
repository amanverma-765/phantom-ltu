package com.navi.phantom.domain.usecase

import com.navi.phantom.domain.model.DetailedAppInfo
import com.navi.phantom.domain.model.InstalledApp
import com.navi.phantom.domain.repository.InstalledAppProvider

class InstalledAppUseCase(
    private val installedAppProvider: InstalledAppProvider
) {
    suspend fun getAllInstalledApps(): Result<List<InstalledApp>> =
        installedAppProvider.getAllInstalledApps()

    suspend fun getAppDetails(packageName: String): Result<DetailedAppInfo> =
        installedAppProvider.getAppDetails(packageName)

    fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> =
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
}
