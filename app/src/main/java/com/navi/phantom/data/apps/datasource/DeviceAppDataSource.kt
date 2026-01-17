package com.navi.phantom.data.apps.datasource

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import co.touchlab.kermit.Logger
import com.navi.phantom.core.ext.getInstalledApplicationsCompat
import com.navi.phantom.core.ext.getPackageInfoCompat
import com.navi.phantom.data.apps.dto.DeviceAppDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeviceAppDataSource(private val context: Context) {

    private val log = Logger.withTag("DeviceAppDataSource")
    private val pm: PackageManager get() = context.packageManager

    suspend fun getAllDeviceApps(): Result<List<DeviceAppDto>> = withContext(Dispatchers.IO) {
        runCatching {
            pm.getInstalledApplicationsCompat(PackageManager.GET_META_DATA.toLong())
                .asSequence()
                .filter { it.isUserInstalled }
                .mapNotNull { app ->
                    app.toDeviceAppDto()
                        .onFailure { log.w(it) { "Failed to load: ${app.packageName}" } }
                        .getOrNull()
                }
                .sortedWith(
                    // Patched apps first, then alphabetically by name
                    compareByDescending<DeviceAppDto> { it.isPatched }
                        .thenBy { it.appName.lowercase() }
                )
                .toList()
        }
    }

    suspend fun getAppByPackageName(packageName: String): Result<DeviceAppDto> = withContext(Dispatchers.IO) {
        runCatching {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }.fold(
            onSuccess = { appInfo -> appInfo.toDeviceAppDto() },
            onFailure = { Result.failure(it) }
        )
    }

    private fun ApplicationInfo.toDeviceAppDto(): Result<DeviceAppDto> = runCatching {
        val pkgInfo = pm.getPackageInfoCompat(packageName, PackageManager.GET_PERMISSIONS.toLong())

        DeviceAppDto(
            packageName = packageName,
            appName = loadLabel(pm).toString(),
            versionName = pkgInfo.versionName.orEmpty(),
            versionCode = pkgInfo.longVersionCode,
            icon = loadIcon(pm),
            apkPath = sourceDir,
            apkSizeBytes = File(sourceDir).length(),
            installTimeMillis = pkgInfo.firstInstallTime,
            lastUpdateTimeMillis = pkgInfo.lastUpdateTime,
            targetSdk = targetSdkVersion,
            minSdk = minSdkVersion,
            usesLocation = packageName.hasLocationPermission(),
            isPatched = hasValidPhantomMetadata()
        )
    }

    private fun ApplicationInfo.hasValidPhantomMetadata(): Boolean {
        val metaValue = metaData?.getString("phantom") ?: return false
        return try {
            val decoded = Base64.decode(metaValue, Base64.DEFAULT)
            val json = String(decoded, Charsets.UTF_8)
            json.trimStart().startsWith("{") && json.trimEnd().endsWith("}")
        } catch (e: Exception) {
            log.i(e) {"Invalid metadata for $packageName"}
            false
        }
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