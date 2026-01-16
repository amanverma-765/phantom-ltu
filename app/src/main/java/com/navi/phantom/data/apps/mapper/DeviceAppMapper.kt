package com.navi.phantom.data.apps.mapper

import com.navi.phantom.data.apps.dto.DeviceAppDetailsDto
import com.navi.phantom.data.apps.dto.DeviceAppDto
import com.navi.phantom.domain.model.DeviceAppDetails
import com.navi.phantom.domain.model.DeviceApp

object DeviceAppMapper {
    fun DeviceAppDto.toDeviceApp() =
        DeviceApp(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            icon = icon,
            apkPath = apkPath,
            usesLocation = usesLocation
        )

    fun DeviceAppDetailsDto.toDeviceAppDetails() =
        DeviceAppDetails(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            versionCode = versionCode,
            icon = icon,
            apkPath = apkPath,
            apkSizeBytes = apkSizeBytes,
            installTimeMillis = installTimeMillis,
            lastUpdateTimeMillis = lastUpdateTimeMillis,
            targetSdk = targetSdk,
            minSdk = minSdk,
            usesLocation = usesLocation
        )
}
