package com.navi.phantom.data.apps.mapper

import com.navi.phantom.data.apps.dto.DetailedAppInfoDto
import com.navi.phantom.data.apps.dto.InstalledAppDto
import com.navi.phantom.domain.model.DetailedAppInfo
import com.navi.phantom.domain.model.InstalledApp

object InstalledAppMapper {
    fun InstalledAppDto.toInstalledApp() =
        InstalledApp(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            icon = icon,
            apkPath = apkPath,
            usesLocation = usesLocation
        )

    fun DetailedAppInfoDto.toDetailedAppInfo() =
        DetailedAppInfo(
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