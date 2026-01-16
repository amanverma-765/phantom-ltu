package com.navi.phantom.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.model.PatchingOptions
import com.navi.phantom.domain.model.PatchingProgress
import com.navi.phantom.domain.repository.PatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PatcherUseCase(
    private val context: Context,
    private val patcherProvider: PatcherProvider
) {
    private val log = Logger.withTag("PatcherUseCase")
    private val pm: PackageManager = context.packageManager

    fun patch(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        splitApkPaths: List<String>,
        options: PatchingOptions
    ): Flow<PatchingProgress> = patcherProvider.patch(
        packageName = packageName,
        versionCode = versionCode,
        apkPath = apkPath,
        splitApkPaths = splitApkPaths,
        options = options
    )

    fun cancel() = patcherProvider.cancel()

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
