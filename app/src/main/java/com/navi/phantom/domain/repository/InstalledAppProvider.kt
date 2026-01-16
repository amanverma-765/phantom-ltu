package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.DetailedAppInfo
import com.navi.phantom.domain.model.InstalledApp

interface InstalledAppProvider {
    suspend fun getAllInstalledApps(): Result<List<InstalledApp>>
    suspend fun getAppDetails(packageName: String): Result<DetailedAppInfo>
}
