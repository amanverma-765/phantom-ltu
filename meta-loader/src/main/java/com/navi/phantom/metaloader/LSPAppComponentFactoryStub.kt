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
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.ByteArrayOutputStream
import java.io.File
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
                // First attempt may fail on fresh install due to AppsFilter
                // not yet recognizing QUERY_ALL_PACKAGES permission
                log.w { "First bootstrap attempt failed, retrying..." }
                try {
                    Thread.sleep(500)
                    bootstrapInternal()
                } catch (e2: Throwable) {
                    throw ExceptionInInitializerError(e2)
                }
            }
        }

        private fun bootstrapInternal() {
            val vmRuntime = Class.forName("dalvik.system.VMRuntime")
            val getRuntime = vmRuntime.getDeclaredMethod("getRuntime").apply { isAccessible = true }
            val vmInstructionSet = vmRuntime.getDeclaredMethod("vmInstructionSet").apply { isAccessible = true }
            val arch = vmInstructionSet.invoke(getRuntime.invoke(null)) as String
            val libName = archToLib[arch]

            log.i { "Bootstrap loader from manager" }
            val ipm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
            val manager: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                HiddenApiBypass.invoke(
                    IPackageManager::class.java, ipm, "getApplicationInfo",
                    Constants.MANAGER_PACKAGE_NAME, 0L, Process.myUid() / 100000
                ) as ApplicationInfo
            } else {
                ipm.getApplicationInfo(Constants.MANAGER_PACKAGE_NAME, 0, Process.myUid() / 100000)
            }
            ZipFile(File(manager.sourceDir)).use { zip ->
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
            val soPath = "${manager.sourceDir}!/assets/phantom/so/$libName/libphantom.so"
            System.load(soPath)
        }
    }
}