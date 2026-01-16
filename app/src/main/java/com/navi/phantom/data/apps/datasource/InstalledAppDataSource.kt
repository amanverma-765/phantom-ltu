package com.navi.phantom.data.apps.datasource

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import co.touchlab.kermit.Logger
import com.navi.phantom.core.ext.getApplicationInfoCompat
import com.navi.phantom.core.ext.getInstalledApplicationsCompat
import com.navi.phantom.core.ext.getPackageInfoCompat
import com.navi.phantom.data.apps.dto.DetailedAppInfoDto
import com.navi.phantom.data.apps.dto.InstalledAppDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InstalledAppDataSource(private val context: Context) {

    private val log = Logger.withTag("InstalledAppDataSource")
    private val pm: PackageManager get() = context.packageManager

    suspend fun getAllInstalledApps(): Result<List<InstalledAppDto>> = withContext(Dispatchers.IO) {
        runCatching {
            pm.getInstalledApplicationsCompat()
                .asSequence()
                .filter { it.isUserInstalled }
                .mapNotNull { app ->
                    app.toInstalledAppDto()
                        .onFailure { log.w(it) { "Failed to load: ${app.packageName}" } }
                        .getOrNull()
                }
                .sortedBy { it.appName.lowercase() }
                .toList()
        }
    }

    suspend fun getAppDetails(packageName: String): Result<DetailedAppInfoDto> = withContext(Dispatchers.IO) {
        runCatching {
            val appInfo = pm.getApplicationInfoCompat(packageName)
            val pkgInfo = pm.getPackageInfoCompat(packageName, PackageManager.GET_PERMISSIONS.toLong())

            DetailedAppInfoDto(
                packageName = packageName,
                appName = appInfo.loadLabel(pm).toString(),
                versionName = pkgInfo.versionName.orEmpty(),
                versionCode = pkgInfo.longVersionCode,
                icon = appInfo.loadIcon(pm),
                apkPath = appInfo.sourceDir,
                apkSizeBytes = File(appInfo.sourceDir).length(),
                installTimeMillis = pkgInfo.firstInstallTime,
                lastUpdateTimeMillis = pkgInfo.lastUpdateTime,
                targetSdk = appInfo.targetSdkVersion,
                minSdk = appInfo.minSdkVersion,
                usesLocation = packageName.hasLocationPermission()
            )
        }
    }

    private fun ApplicationInfo.toInstalledAppDto(): Result<InstalledAppDto> = runCatching {
        InstalledAppDto(
            packageName = packageName,
            appName = loadLabel(pm).toString(),
            versionName = pm.getPackageInfoCompat(packageName).versionName.orEmpty(),
            icon = loadIcon(pm),
            apkPath = sourceDir,
            usesLocation = packageName.hasLocationPermission()
        )
    }

    private fun String.hasLocationPermission(): Boolean = runCatching {
        val permissions = pm.getPackageInfoCompat(this, PackageManager.GET_PERMISSIONS.toLong())
            .requestedPermissions
            ?: return@runCatching false

        permissions.any { it in LOCATION_PERMISSIONS }
    }.getOrDefault(false)

    private val ApplicationInfo.isUserInstalled: Boolean
        get() = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

    companion object {
        private val LOCATION_PERMISSIONS = buildSet {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }
}
