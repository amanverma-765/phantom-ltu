package com.navi.phantom.domain.repository

import com.navi.phantom.domain.models.DetailedAppInfo
import com.navi.phantom.domain.models.InstalledApp

interface InstalledAppRepository {
    suspend fun getAllInstalledApps(): Result<List<InstalledApp>>
    suspend fun getAppDetails(packageName: String): Result<DetailedAppInfo>
}