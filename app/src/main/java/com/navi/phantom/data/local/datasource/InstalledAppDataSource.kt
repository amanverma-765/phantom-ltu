package com.navi.phantom.data.local.datasource

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import co.touchlab.kermit.Logger
import com.navi.phantom.data.local.dto.DetailedAppInfoDto
import com.navi.phantom.data.local.dto.InstalledAppDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InstalledAppDataSource(
    private val context: Context
) {

    private val log = Logger.withTag("InstalledAppDataSource")

    private val packageManager: PackageManager
        get() = context.packageManager

    suspend fun getAllInstalledApps(): Result<List<InstalledAppDto>> = withContext(Dispatchers.IO) {
        runCatching {
            getInstalledApplications()
                .asSequence()
                .filter { appInfo ->
                    isUserInstalledApp(appInfo)
                }
                .mapNotNull { appInfo ->
                    toInstalledApp(appInfo)
                        .onFailure { log.w(it) { "Failed to load app: ${appInfo.packageName}" } }
                        .getOrNull()
                }
                .sortedBy { it.appName.lowercase() }
                .toList()
        }
    }

    suspend fun getAppDetails(packageName: String): Result<DetailedAppInfoDto> = withContext(Dispatchers.IO) {
        runCatching {
            val appInfo = getApplicationInfo(packageName)
            val packageInfo = getPackageInfo(packageName)

            DetailedAppInfoDto(
                packageName = packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = packageInfo.longVersionCode,
                icon = appInfo.loadIcon(packageManager),
                apkPath = appInfo.sourceDir,
                apkSizeBytes = File(appInfo.sourceDir).length(),
                installTimeMillis = packageInfo.firstInstallTime,
                lastUpdateTimeMillis = packageInfo.lastUpdateTime,
                targetSdk = appInfo.targetSdkVersion,
                minSdk = appInfo.minSdkVersion,
                usesLocation = hasLocationPermission(packageName)
            )
        }
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    }

    private fun getPackageInfo(packageName: String): android.content.pm.PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    }

    private fun getInstalledApplications(): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
    }

    private fun getVersionName(packageName: String): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        }.onFailure { log.d(it) { "Failed to get version for $packageName" } }
         .getOrNull().orEmpty()
    }

    private fun isUserInstalledApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
    }

    private fun toInstalledApp(appInfo: ApplicationInfo): Result<InstalledAppDto> {
        return runCatching {
            InstalledAppDto(
                packageName = appInfo.packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                versionName = getVersionName(appInfo.packageName),
                icon = appInfo.loadIcon(packageManager),
                apkPath = appInfo.sourceDir,
                usesLocation = hasLocationPermission(appInfo.packageName)
            )
        }
    }

    private fun hasLocationPermission(packageName: String): Boolean {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val permissions = packageInfo.requestedPermissions ?: return@runCatching false
            permissions.any { permission ->
                permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                permission == Manifest.permission.ACCESS_COARSE_LOCATION ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }.getOrDefault(false)
    }
}