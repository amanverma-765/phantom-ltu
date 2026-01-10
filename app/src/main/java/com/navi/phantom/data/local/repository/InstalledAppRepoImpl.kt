package com.navi.phantom.data.local.repository

import com.navi.phantom.data.local.datasource.InstalledAppDataSource
import com.navi.phantom.data.local.mapper.InstalledAppMapper.toInstalledApp
import com.navi.phantom.domain.models.InstalledApp
import com.navi.phantom.domain.repository.InstalledAppRepository

class InstalledAppRepoImpl(
    private val installedAppDataSource: InstalledAppDataSource
) : InstalledAppRepository {
    override suspend fun getAllInstalledApps(): Result<List<InstalledApp>> =
        installedAppDataSource.getAllInstalledApps()
            .map { apps -> apps.map { it.toInstalledApp() } }
}