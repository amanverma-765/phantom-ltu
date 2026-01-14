package com.navi.phantom.service

import android.content.Context
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import co.touchlab.kermit.Logger
import com.navi.phantom.loader.util.FileUtils
import com.navi.phantom.shared.Constants
import com.navi.phantom.shared.ModuleLoader
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.ILSPApplicationService
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile

class LocalApplicationService(context: Context) : ILSPApplicationService.Stub() {

    private val log = Logger.withTag("Phantom")

    private val modules = mutableListOf<Module>()

    init {
        context.assets.list("phantom/modules")?.forEach { name ->
            runCatching {
                val packageName = name.dropLast(4)
                val modulePath = "${context.cacheDir}/phantom/$packageName/"

                val cacheApkPath = ZipFile(context.packageResourcePath).use { sourceFile ->
                    val entry = sourceFile.getEntry(Constants.EMBEDDED_MODULES_ASSET_PATH + name)
                        ?: throw IOException("Module asset not found: $name")
                    "$modulePath${entry.crc}.apk"
                }

                if (!Files.exists(Paths.get(cacheApkPath))) {
                    log.i { "Extract module apk: $packageName" }
                    FileUtils.deleteFolderIfExists(Paths.get(modulePath))
                    Files.createDirectories(Paths.get(modulePath))
                    context.assets.open("phantom/modules/$name").use { input ->
                        Files.copy(input, Paths.get(cacheApkPath))
                    }
                }

                val module = Module().apply {
                    apkPath = cacheApkPath
                    this.packageName = packageName
                    file = ModuleLoader.loadModule(cacheApkPath)
                }
                modules.add(module)
                log.i { "Loaded module: $packageName" }
            }.onFailure { e ->
                log.e(e) { "Failed to load module: $name" }
            }
        }
    }

    override fun isLogMuted(): Boolean = false

    override fun getLegacyModulesList(): List<Module> = modules

    override fun getModulesList(): List<Module> = emptyList()

    override fun getPrefsPath(packageName: String): String =
        File(Environment.getDataDirectory(), "data/$packageName/shared_prefs/").absolutePath

    override fun requestInjectedManagerBinder(binder: MutableList<IBinder>?): ParcelFileDescriptor? = null

    override fun asBinder(): IBinder = this
}