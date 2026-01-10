package com.navi.phantom.data.local.mapper

import com.navi.phantom.data.local.dto.InstalledAppDto
import com.navi.phantom.domain.models.InstalledApp

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
}