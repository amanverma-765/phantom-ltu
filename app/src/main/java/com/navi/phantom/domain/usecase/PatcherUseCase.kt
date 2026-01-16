package com.navi.phantom.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.model.BootstrapOptions
import com.navi.phantom.domain.model.BootstrapProgress
import com.navi.phantom.domain.repository.BootstrapProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BootstrapUseCase(
    private val context: Context,
    private val bootstrapProvider: BootstrapProvider
) {
    private val log = Logger.withTag("BootstrapUseCase")
    private val pm: PackageManager = context.packageManager

    fun bootstrap(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        splitApkPaths: List<String>,
        options: BootstrapOptions
    ): Flow<BootstrapProgress> = bootstrapProvider.bootstrap(
        packageName = packageName,
        versionCode = versionCode,
        apkPath = apkPath,
        splitApkPaths = splitApkPaths,
        options = options
    )

    fun cancel() = bootstrapProvider.cancel()

    suspend fun getSplitApkPaths(packageName: String): List<String> = withContext(Dispatchers.IO) {
        try {
            pm.getApplicationInfo(packageName, 0).splitSourceDirs?.toList() ?: emptyList()
        } catch (e: PackageManager.NameNotFoundException) {
            log.d(e) { "Package not found when getting split APKs: $packageName" }
            emptyList()
        } catch (e: Exception) {
            log.e(e) { "Failed to get split APKs for $packageName" }
            emptyList()
        }
    }
}