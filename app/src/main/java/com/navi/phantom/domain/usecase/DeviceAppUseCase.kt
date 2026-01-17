package com.navi.phantom.domain.usecase

import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.repository.DeviceAppProvider

class DeviceAppUseCase(
    private val deviceAppProvider: DeviceAppProvider
) {
    suspend fun getAllInstalledApps(): Result<List<DeviceApp>> =
        deviceAppProvider.getAllDeviceApps()

    suspend fun getAppByPackageName(packageName: String): Result<DeviceApp> =
        deviceAppProvider.getAppByPackageName(packageName)
}