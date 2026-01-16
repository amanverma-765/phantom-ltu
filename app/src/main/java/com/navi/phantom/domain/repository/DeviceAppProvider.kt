package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.DeviceAppDetails
import com.navi.phantom.domain.model.DeviceApp

interface DeviceAppProvider {
    suspend fun getAllInstalledApps(): Result<List<DeviceApp>>
    suspend fun getAppDetails(packageName: String): Result<DeviceAppDetails>
}
