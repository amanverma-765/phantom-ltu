package com.navi.phantom.data.apps.mapper

import com.navi.phantom.data.apps.dto.DeviceAppDto
import com.navi.phantom.domain.model.DeviceApp

object DeviceAppMapper {
    fun DeviceAppDto.toDeviceApp() =
        DeviceApp(
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
            usesLocation = usesLocation,
            isPatched = isPatched
        )
}
