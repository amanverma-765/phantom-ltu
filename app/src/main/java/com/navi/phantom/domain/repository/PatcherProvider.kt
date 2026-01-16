package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.BootstrapOptions
import com.navi.phantom.domain.model.BootstrapProgress
import kotlinx.coroutines.flow.Flow

interface BootstrapProvider {
    fun bootstrap(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        splitApkPaths: List<String>,
        options: BootstrapOptions
    ): Flow<BootstrapProgress>

    fun cancel()
}
