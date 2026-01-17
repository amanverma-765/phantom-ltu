package com.navi.phantom.data.apps

import com.navi.phantom.data.apps.datasource.DeviceAppDataSource
import com.navi.phantom.data.apps.mapper.DeviceAppMapper.toDeviceApp
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.repository.DeviceAppProvider

class DeviceAppProviderImpl(
    private val deviceAppDataSource: DeviceAppDataSource
) : DeviceAppProvider {
    override suspend fun getAllDeviceApps(): Result<List<DeviceApp>> =
        deviceAppDataSource.getAllDeviceApps()
            .map { apps -> apps.map { it.toDeviceApp() } }

    override suspend fun getAppByPackageName(packageName: String): Result<DeviceApp> =
        deviceAppDataSource.getAppByPackageName(packageName)
            .map { it.toDeviceApp() }
}