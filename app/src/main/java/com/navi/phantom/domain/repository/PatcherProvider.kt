package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.PatchingOptions
import com.navi.phantom.domain.model.PatchingProgress
import kotlinx.coroutines.flow.Flow

interface PatcherProvider {
    fun patch(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        splitApkPaths: List<String>,
        options: PatchingOptions
    ): Flow<PatchingProgress>

    fun cancel()
}
