@file:Suppress("unused")

package com.navi.phantom.loader

import android.app.ActivityThread
import android.app.LoadedApk
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.CompatibilityInfo
import android.os.Build
import android.os.RemoteException
import android.system.Os
import co.touchlab.kermit.Logger
import com.navi.phantom.loader.util.FileUtils
import com.navi.phantom.service.RemoteApplicationService
import com.navi.phantom.shared.Constants.CONFIG_ASSET_PATH
import com.navi.phantom.shared.Constants.ORIGINAL_APK_ASSET_PATH
import de.robv.android.xposed.XposedHelpers
import hidden.HiddenApiBridge
import org.json.JSONObject
import org.lsposed.lspd.core.Startup
import org.lsposed.lspd.service.ILSPApplicationService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.function.BiConsumer
import java.util.zip.ZipFile

object LSPApplication {

    private val log = Logger.withTag("Phantom")

    private const val FIRST_APP_ZYGOTE_ISOLATED_UID = 90000
    private const val PER_USER_RANGE = 100000

    private lateinit var activityThread: ActivityThread
    private lateinit var stubLoadedApk: LoadedApk
    private lateinit var appLoadedApk: LoadedApk
    private lateinit var config: JSONObject

    @JvmStatic
    fun isIsolated(): Boolean =
        (android.os.Process.myUid() % PER_USER_RANGE) >= FIRST_APP_ZYGOTE_ISOLATED_UID

    @JvmStatic
    @Throws(RemoteException::class, IOException::class)
    fun onLoad() {
        if (isIsolated()) {
            log.d { "Skip isolated process" }
            return
        }

        activityThread = ActivityThread.currentActivityThread()
        val context = createLoadedApkWithContext()
        if (context == null) {
            log.e { "Error when creating context" }
            return
        }

        log.d { "Initialize service client" }
        val service: ILSPApplicationService = RemoteApplicationService(context)

        disableProfile(context)
        Startup.initXposed(false, ActivityThread.currentProcessName(), context.applicationInfo.dataDir, service)
        Startup.bootstrapXposed()

        // WARN: Since it uses `XResource`, the following class should not be initialized
        // before forkPostCommon is invoke. Otherwise, you will get failure of XResources
        log.i { "Load modules" }
        LSPLoader.initModules(appLoadedApk)
        log.i { "Modules initialized" }

        switchAllClassLoader()
        SigBypass.doSigBypass(context, config.optInt("sigBypassLevel"))

        log.i { "Phantom bootstrap completed" }
    }

    private fun createLoadedApkWithContext(): Context? {
        return try {
            val mBoundApplication = XposedHelpers.getObjectField(activityThread, "mBoundApplication")

            stubLoadedApk = XposedHelpers.getObjectField(mBoundApplication, "info") as LoadedApk
            val appInfo = XposedHelpers.getObjectField(mBoundApplication, "appInfo") as ApplicationInfo
            val compatInfo = XposedHelpers.getObjectField(mBoundApplication, "compatInfo") as CompatibilityInfo
            val baseClassLoader = stubLoadedApk.classLoader

            baseClassLoader.getResourceAsStream(CONFIG_ASSET_PATH).use { inputStream ->
                config = JSONObject(inputStream.bufferedReader(StandardCharsets.UTF_8).readText())
            }

            log.i { "Signature bypass level: ${config.optInt("sigBypassLevel")}" }

            val originPath = Paths.get(appInfo.dataDir, "cache/phantom/origin/")
            val cacheApkPath = ZipFile(appInfo.sourceDir).use { sourceFile ->
                val entry = sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH)
                    ?: throw IOException("Original APK asset not found in patched APK")
                originPath.resolve("${entry.crc}.apk")
            }

            appInfo.sourceDir = cacheApkPath.toString()
            appInfo.publicSourceDir = cacheApkPath.toString()
            if (config.has("appComponentFactory")) {
                appInfo.appComponentFactory = config.optString("appComponentFactory")
            }

            if (!Files.exists(cacheApkPath)) {
                log.i { "Extract original apk" }
                FileUtils.deleteFolderIfExists(originPath)
                Files.createDirectories(originPath)
                baseClassLoader.getResourceAsStream(ORIGINAL_APK_ASSET_PATH).use { inputStream ->
                    Files.copy(inputStream, cacheApkPath)
                }
            }
            cacheApkPath.toFile().setWritable(false)

            @Suppress("UNCHECKED_CAST")
            val mPackages = XposedHelpers.getObjectField(activityThread, "mPackages") as MutableMap<Any?, Any?>
            mPackages.remove(appInfo.packageName)
            appLoadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo)
            XposedHelpers.setObjectField(mBoundApplication, "info", appLoadedApk)

            val activityClientRecordClass = XposedHelpers.findClass(
                $$"android.app.ActivityThread$ActivityClientRecord",
                ActivityThread::class.java.classLoader
            )
            val fixActivityClientRecord = BiConsumer<Any?, Any?> { _, v ->
                if (activityClientRecordClass.isInstance(v)) {
                    val pkgInfo = XposedHelpers.getObjectField(v, "packageInfo")
                    if (pkgInfo === stubLoadedApk) {
                        log.d { "fix loadedapk from ActivityClientRecord" }
                        XposedHelpers.setObjectField(v, "packageInfo", appLoadedApk)
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            val mActivities = XposedHelpers.getObjectField(activityThread, "mActivities") as Map<Any?, Any?>
            mActivities.forEach(fixActivityClientRecord)

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    @Suppress("UNCHECKED_CAST")
                    val mLaunchingActivities = XposedHelpers.getObjectField(activityThread, "mLaunchingActivities") as Map<Any?, Any?>
                    mLaunchingActivities.forEach(fixActivityClientRecord)
                }
            }

            log.i { "hooked app initialized: $appLoadedApk" }

            val context = XposedHelpers.callStaticMethod(
                Class.forName("android.app.ContextImpl"),
                "createAppContext",
                activityThread,
                stubLoadedApk
            ) as Context

            if (config.has("appComponentFactory")) {
                runCatching {
                    context.classLoader.loadClass(appInfo.appComponentFactory)
                }.onFailure {
                    // This will happen on some strange shells like 360
                    log.w { "Original AppComponentFactory not found: ${appInfo.appComponentFactory}" }
                    appInfo.appComponentFactory = null
                }
            }

            context
        } catch (e: Exception) {
            log.e(e) { "createLoadedApk failed" }
            null
        }
    }

    @JvmStatic
    fun disableProfile(context: Context) {
        val appInfo = context.applicationInfo ?: return
        val pkgName = context.packageName

        val codePaths = mutableListOf<String>()
        if ((appInfo.flags and ApplicationInfo.FLAG_HAS_CODE) != 0) {
            codePaths.add(appInfo.sourceDir)
        }
        appInfo.splitSourceDirs?.let { codePaths.addAll(it) }

        if (codePaths.isEmpty()) {
            // If there are no code paths there's no need to setup a profile file and register with
            // the runtime,
            return
        }

        val profileDir = HiddenApiBridge.Environment_getDataProfilesDePackageDirectory(
            appInfo.uid / PER_USER_RANGE,
            pkgName
        )

        val attrs = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--------"))

        for (i in codePaths.indices.reversed()) {
            val splitName = if (i == 0) null else appInfo.splitNames?.get(i - 1)
            val curProfileFile = File(
                profileDir,
                if (splitName == null) "primary.prof" else "$splitName.split.prof"
            ).absoluteFile

            log.d { "Processing ${curProfileFile.absolutePath}" }

            runCatching {
                if (!curProfileFile.exists()) {
                    Files.createFile(curProfileFile.toPath(), attrs)
                    return@runCatching
                }
                if (!curProfileFile.canWrite() && Files.size(curProfileFile.toPath()) == 0L) {
                    log.d { "Skip profile ${curProfileFile.absolutePath}" }
                    return@runCatching
                }
                if (curProfileFile.exists() && !curProfileFile.delete()) {
                    runCatching {
                        FileOutputStream(curProfileFile).use {
                            log.d { "Failed to delete, try to clear content ${curProfileFile.absolutePath}" }
                        }
                    }.onFailure { e ->
                        log.e(e) { "Failed to delete and clear profile file ${curProfileFile.absolutePath}" }
                    }
                    Os.chmod(curProfileFile.absolutePath, 256) // 00400 octal
                }
            }.onFailure { e ->
                log.e(e) { "Failed to disable profile file ${curProfileFile.absolutePath}" }
            }
        }
    }

    private fun switchAllClassLoader() {
        LoadedApk::class.java.declaredFields
            .filter { it.type == ClassLoader::class.java }
            .forEach { field ->
                val obj = XposedHelpers.getObjectField(appLoadedApk, field.name)
                XposedHelpers.setObjectField(stubLoadedApk, field.name, obj)
            }
    }
}