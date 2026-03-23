package com.navi.phantom.data.apps

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import co.touchlab.kermit.Logger
import com.navi.phantom.core.ext.getApplicationInfoCompat
import com.navi.phantom.core.ext.getInstalledApplicationsCompat
import com.navi.phantom.core.ext.getPackageInfoCompat
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.repository.DeviceAppProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeviceAppProviderImpl(private val context: Context) : DeviceAppProvider {

    private val log = Logger.withTag("DeviceAppProvider")
    private val pm: PackageManager get() = context.packageManager

    override suspend fun getAllDeviceApps(): Result<List<DeviceApp>> = withContext(Dispatchers.IO) {
        runCatching {
            pm.getInstalledApplicationsCompat(PackageManager.GET_META_DATA.toLong())
                .asSequence()
                .filter { it.isUserInstalled }
                .mapNotNull { app ->
                    app.toDeviceApp()
                        .onFailure { log.w(it) { "Failed to load: ${app.packageName}" } }
                        .getOrNull()
                }
                .sortedWith(
                    // Patched apps first, then alphabetically by name
                    compareByDescending<DeviceApp> { it.isPatched }
                        .thenBy { it.appName.lowercase() }
                )
                .toList()
        }
    }

    override suspend fun getAppByPackageName(packageName: String): Result<DeviceApp> = withContext(Dispatchers.IO) {
        runCatching {
            pm.getApplicationInfoCompat(packageName, PackageManager.GET_META_DATA.toLong())
        }.fold(
            onSuccess = { appInfo -> appInfo.toDeviceApp() },
            onFailure = { Result.failure(it) }
        )
    }

    private fun ApplicationInfo.toDeviceApp(): Result<DeviceApp> = runCatching {
        val pkgInfo = pm.getPackageInfoCompat(packageName, PackageManager.GET_PERMISSIONS.toLong())
        val permissions = pkgInfo.requestedPermissions.orEmpty()

        DeviceApp(
            packageName = packageName,
            appName = loadLabel(pm).toString(),
            versionName = pkgInfo.versionName.orEmpty(),
            versionCode = pkgInfo.longVersionCode,
            apkPath = sourceDir,
            apkSizeBytes = File(sourceDir).length(),
            installTimeMillis = pkgInfo.firstInstallTime,
            lastUpdateTimeMillis = pkgInfo.lastUpdateTime,
            targetSdk = targetSdkVersion,
            minSdk = minSdkVersion,
            usesLocation = permissions.any { it in LOCATION_PERMISSIONS },
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
            log.i(e) { "Invalid metadata for $packageName" }
            false
        }
    }

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
