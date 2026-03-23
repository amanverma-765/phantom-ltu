package com.navi.phantom.manager

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import co.touchlab.kermit.Logger
import com.navi.phantom.data.database.dao.ActiveLocationDao
import com.navi.phantom.shared.ConfigKeys
import com.navi.phantom.shared.ModuleLoader
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.ILSPApplicationService
import java.io.File

/**
 * AIDL stub implementation of [ILSPApplicationService].
 * Patched apps use this to request their assigned Xposed modules.
 */
object ManagerService : ILSPApplicationService.Stub(), KoinComponent {

    private val log = Logger.withTag("ManagerService")
    private val context: Context by inject()
    private val activeLocationDao: ActiveLocationDao by inject()

    override fun isLogMuted(): Boolean = false

    override fun getLegacyModulesList(): List<Module> {
        val callerPackage = getCallerPackageName()
        log.i { "$callerPackage calls getLegacyModulesList" }

        val activeLocation = runBlocking {
            activeLocationDao.getActiveLocationSync(callerPackage)
        }

        if (activeLocation != null) {
            log.i {
                "$callerPackage has active location: " +
                    "${activeLocation.placeName} (${activeLocation.latitude}, ${activeLocation.longitude})"
            }
        } else {
            log.i { "$callerPackage has no active location assigned" }
        }

        return emptyList()
    }

    override fun getModulesList(): List<Module> {
        val callerPackage = getCallerPackageName()
        log.d { "$callerPackage calls getModulesList" }
        return emptyList()
    }

    override fun getPrefsPath(packageName: String): String {
        return File(Environment.getDataDirectory(), "data/$packageName/shared_prefs/").absolutePath
    }

    override fun requestInjectedManagerBinder(binder: MutableList<IBinder>?): ParcelFileDescriptor? {
        return null
    }

    override fun getConfigBundle(): Bundle {
        val callerPackage = getCallerPackageName()
        log.d { "$callerPackage requests config bundle" }

        val bundle = Bundle()
        val activeLocation = runBlocking {
            activeLocationDao.getActiveLocationSync(callerPackage)
        }

        if (activeLocation != null) {
            bundle.putBoolean(ConfigKeys.HAS_LOCATION, true)
            bundle.putDouble(ConfigKeys.LATITUDE, activeLocation.latitude)
            bundle.putDouble(ConfigKeys.LONGITUDE, activeLocation.longitude)
            activeLocation.accuracy?.let { bundle.putFloat(ConfigKeys.ACCURACY, it) }
            activeLocation.placeName?.let { bundle.putString(ConfigKeys.PLACE_NAME, it) }
        } else {
            bundle.putBoolean(ConfigKeys.HAS_LOCATION, false)
        }

        return bundle
    }

    /**
     * Helper to get the package name of the calling app.
     */
    private fun getCallerPackageName(): String {
        val callingUid = Binder.getCallingUid()
        return try {
            context.packageManager.getPackagesForUid(callingUid)?.firstOrNull() ?: "uid:$callingUid"
        } catch (e: Exception) {
            log.w(e) { "Failed to get package name for uid: $callingUid" }
            "uid:$callingUid"
        }
    }

    /**
     * Loads a module from the given APK path.
     * Returns a [Module] with DEX loaded into SharedMemory, or null if loading fails.
     *
     * @param apkPath Path to the module APK file
     * @param modulePackageName Package name of the module
     * @param appId Optional app ID (defaults to -1)
     */
    fun loadModule(apkPath: String, modulePackageName: String, appId: Int = -1): Module? {
        val preLoadedApk = ModuleLoader.loadModule(apkPath)
        if (preLoadedApk == null) {
            log.w { "Failed to load module: $modulePackageName from $apkPath" }
            return null
        }

        return Module().apply {
            this.packageName = modulePackageName
            this.appId = appId
            this.apkPath = apkPath
            this.file = preLoadedApk
            this.applicationInfo = null
            this.service = null
        }
    }
}
