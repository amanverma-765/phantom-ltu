package com.navi.phantom.domain.usecase

import com.navi.phantom.domain.model.DeviceAppDetails
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.repository.DeviceAppProvider

class DeviceAppUseCase(
    private val deviceAppProvider: DeviceAppProvider
) {
    suspend fun getAllInstalledApps(): Result<List<DeviceApp>> =
        deviceAppProvider.getAllInstalledApps()

    suspend fun getAppDetails(packageName: String): Result<DeviceAppDetails> =
        deviceAppProvider.getAppDetails(packageName)

    fun filterApps(apps: List<DeviceApp>, query: String): List<DeviceApp> =
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
}
