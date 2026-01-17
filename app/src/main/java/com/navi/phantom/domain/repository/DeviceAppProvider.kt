package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.DeviceApp

interface DeviceAppProvider {
    suspend fun getAllDeviceApps(): Result<List<DeviceApp>>
    suspend fun getAppByPackageName(packageName: String): Result<DeviceApp>
}