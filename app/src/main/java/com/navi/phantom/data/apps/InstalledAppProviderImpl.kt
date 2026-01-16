package com.navi.phantom.data.apps

import com.navi.phantom.data.apps.datasource.InstalledAppDataSource
import com.navi.phantom.data.apps.mapper.InstalledAppMapper.toDetailedAppInfo
import com.navi.phantom.data.apps.mapper.InstalledAppMapper.toInstalledApp
import com.navi.phantom.domain.model.DetailedAppInfo
import com.navi.phantom.domain.model.InstalledApp
import com.navi.phantom.domain.repository.InstalledAppProvider

class InstalledAppProviderImpl(
    private val installedAppDataSource: InstalledAppDataSource
) : InstalledAppProvider {
    override suspend fun getAllInstalledApps(): Result<List<InstalledApp>> =
        installedAppDataSource.getAllInstalledApps()
            .map { apps -> apps.map { it.toInstalledApp() } }

    override suspend fun getAppDetails(packageName: String): Result<DetailedAppInfo> =
        installedAppDataSource.getAppDetails(packageName)
            .map { it.toDetailedAppInfo() }
}
