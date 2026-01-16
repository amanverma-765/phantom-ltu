package com.navi.phantom.data.apps.repository

import com.navi.phantom.data.apps.datasource.InstalledAppDataSource
import com.navi.phantom.data.apps.mapper.InstalledAppMapper.toDetailedAppInfo
import com.navi.phantom.data.apps.mapper.InstalledAppMapper.toInstalledApp
import com.navi.phantom.domain.models.DetailedAppInfo
import com.navi.phantom.domain.models.InstalledApp
import com.navi.phantom.domain.repository.InstalledAppRepository

class InstalledAppRepoImpl(
    private val installedAppDataSource: InstalledAppDataSource
) : InstalledAppRepository {
    override suspend fun getAllInstalledApps(): Result<List<InstalledApp>> =
        installedAppDataSource.getAllInstalledApps()
            .map { apps -> apps.map { it.toInstalledApp() } }

    override suspend fun getAppDetails(packageName: String): Result<DetailedAppInfo> =
        installedAppDataSource.getAppDetails(packageName)
            .map { it.toDetailedAppInfo() }
}