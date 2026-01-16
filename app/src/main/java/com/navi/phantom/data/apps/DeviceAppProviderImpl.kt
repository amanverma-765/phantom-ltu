package com.navi.phantom.data.apps

import com.navi.phantom.data.apps.datasource.DeviceAppDataSource
import com.navi.phantom.data.apps.mapper.DeviceAppMapper.toDeviceAppDetails
import com.navi.phantom.data.apps.mapper.DeviceAppMapper.toDeviceApp
import com.navi.phantom.domain.model.DeviceAppDetails
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.repository.DeviceAppProvider

class DeviceAppProviderImpl(
    private val deviceAppDataSource: DeviceAppDataSource
) : DeviceAppProvider {
    override suspend fun getAllInstalledApps(): Result<List<DeviceApp>> =
        deviceAppDataSource.getAllInstalledApps()
            .map { apps -> apps.map { it.toDeviceApp() } }

    override suspend fun getAppDetails(packageName: String): Result<DeviceAppDetails> =
        deviceAppDataSource.getAppDetails(packageName)
            .map { it.toDeviceAppDetails() }
}
