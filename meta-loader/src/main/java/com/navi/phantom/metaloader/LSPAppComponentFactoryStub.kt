package com.navi.phantom.metaloader

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.app.AppComponentFactory
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Process
import android.os.ServiceManager
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.Constants
import org.json.JSONObject
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

@SuppressLint("UnsafeDynamicallyLoadedCode", "DiscouragedPrivateApi")
class LSPAppComponentFactoryStub : AppComponentFactory() {

    companion object {
        private val log = Logger.withTag("Phantom-MetaLoader")

        private val archToLib = mapOf(
            "arm" to "armeabi-v7a",
            "arm64" to "arm64-v8a",
            "x86" to "x86",
            "x86_64" to "x86_64"
        )

        @JvmField
        var dex: ByteArray? = null

        init {
            val appZygote = ActivityThread.currentActivityThread() == null
            if (appZygote) {
                log.i { "Skip loading libphantom.so for appZygote" }
            } else {
                bootstrap()
            }
        }

        private fun bootstrap() {
            try {
                bootstrapInternal()
            } catch (e: Throwable) {
                throw ExceptionInInitializerError(e)
            }
        }

        private fun bootstrapInternal() {
            val vmRuntime = Class.forName("dalvik.system.VMRuntime")
            val getRuntime = vmRuntime.getDeclaredMethod("getRuntime").apply { isAccessible = true }
            val vmInstructionSet = vmRuntime.getDeclaredMethod("vmInstructionSet").apply { isAccessible = true }
            val arch = vmInstructionSet.invoke(getRuntime.invoke(null)) as String
            val libName = archToLib[arch]

            log.i { "Bootstrap loader from manager" }
            val managerApkPath = findManagerApk()
            log.d { "Manager APK: $managerApkPath" }

            ZipFile(File(managerApkPath)).use { zip ->
                val entry = requireNotNull(zip.getEntry(Constants.LOADER_DEX_ASSET_PATH)) {
                    "Loader DEX not found in manager APK"
                }
                zip.getInputStream(entry).use { input ->
                    ByteArrayOutputStream().use { output ->
                        input.copyTo(output)
                        dex = output.toByteArray()
                    }
                }
            }
            val soPath = "$managerApkPath!/assets/phantom/so/$libName/libphantom.so"
            System.load(soPath)
        }

        /**
         * Find the manager APK path using a 3-tier fallback:
         * 1. Embedded path from patch config (fastest, works on all OEMs)
         * 2. Filesystem scan of /data/app/ (bypasses AppsFilter)
         * 3. IPackageManager query (may fail on first launch)
         */
        private fun findManagerApk(): String {
            // 1. Try embedded path from config
            readManagerApkPathFromConfig()?.let { path ->
                if (File(path).exists()) {
                    log.d { "Found manager APK via config" }
                    return path
                }
                log.d { "Config path exists but file not found: $path" }
            }

            // 2. Try filesystem scan
            findManagerApkFromFilesystem()?.let { path ->
                log.d { "Found manager APK via filesystem" }
                return path
            }

            // 3. Fallback to IPackageManager
            log.d { "Trying IPackageManager fallback" }
            return getManagerApkFromPackageManager()
        }

        /**
         * Read managerApkPath from the embedded config.json in the patched APK.
         */
        private fun readManagerApkPathFromConfig(): String? {
            return try {
                val cl = LSPAppComponentFactoryStub::class.java.classLoader ?: return null
                cl.getResourceAsStream(Constants.CONFIG_ASSET_PATH)?.use { stream ->
                    val json = JSONObject(stream.bufferedReader(StandardCharsets.UTF_8).readText())
                    json.optString("managerApkPath", null as String?)
                }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Scan /data/app/ for the manager package directory.
         */
        private fun findManagerApkFromFilesystem(): String? {
            return try {
                val dataApp = File("/data/app")
                if (!dataApp.exists()) return null

                dataApp.listFiles()?.forEach { randomDir ->
                    randomDir.listFiles()?.forEach { pkgDir ->
                        if (pkgDir.name.startsWith(Constants.MANAGER_PACKAGE_NAME)) {
                            val baseApk = File(pkgDir, "base.apk")
                            if (baseApk.exists()) return baseApk.absolutePath
                        }
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Query IPackageManager for the manager's ApplicationInfo.
         */
        private fun getManagerApkFromPackageManager(): String {
            val ipm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
            val manager: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                HiddenApiBypass.invoke(
                    IPackageManager::class.java, ipm, "getApplicationInfo",
                    Constants.MANAGER_PACKAGE_NAME, 0L, Process.myUid() / 100000
                ) as ApplicationInfo
            } else {
                ipm.getApplicationInfo(Constants.MANAGER_PACKAGE_NAME, 0, Process.myUid() / 100000)
            }
            return manager.sourceDir
        }
    }
}
