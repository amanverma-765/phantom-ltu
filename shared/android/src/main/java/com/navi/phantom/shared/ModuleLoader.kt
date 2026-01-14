package com.navi.phantom.shared

import android.os.SharedMemory
import android.system.ErrnoException
import android.system.OsConstants
import co.touchlab.kermit.Logger
import org.lsposed.lspd.models.PreLoadedApk
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

object ModuleLoader {

    private val log = Logger.withTag("ModuleLoader")

    private fun readDexes(apkFile: ZipFile, preLoadedDexes: MutableList<SharedMemory>) {
        var secondary = 2
        var dexFile = apkFile.getEntry("classes.dex")
        while (dexFile != null) {
            var memory: SharedMemory? = null
            try {
                apkFile.getInputStream(dexFile).use { input ->
                    memory = SharedMemory.create(null, input.available())
                    val byteBuffer = memory!!.mapReadWrite()
                    Channels.newChannel(input).read(byteBuffer)
                    SharedMemory.unmap(byteBuffer)
                    memory!!.setProtect(OsConstants.PROT_READ)
                    preLoadedDexes.add(memory!!)
                }
            } catch (e: IOException) {
                memory?.close()
                log.w(e) { "Can not load $dexFile in $apkFile" }
            } catch (e: ErrnoException) {
                memory?.close()
                log.w(e) { "Can not load $dexFile in $apkFile" }
            }
            dexFile = apkFile.getEntry("classes${secondary++}.dex")
        }
    }

    private fun readName(apkFile: ZipFile, initName: String, names: MutableList<String>) {
        val initEntry = apkFile.getEntry(initName) ?: return
        try {
            apkFile.getInputStream(initEntry).use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    reader.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .forEach { names.add(it) }
                }
            }
        } catch (e: IOException) {
            log.e(e) { "Can not open $initEntry" }
        }
    }

    fun loadModule(path: String?): PreLoadedApk? {
        if (path == null) return null

        val preLoadedDexes = mutableListOf<SharedMemory>()
        val moduleClassNames = mutableListOf<String>()
        val moduleLibraryNames = mutableListOf<String>()

        try {
            ZipFile(path).use { apkFile ->
                readDexes(apkFile, preLoadedDexes)
                readName(apkFile, "assets/xposed_init", moduleClassNames)
                readName(apkFile, "assets/native_init", moduleLibraryNames)
            }
        } catch (e: IOException) {
            log.e(e) { "Can not open $path" }
            return null
        }

        if (preLoadedDexes.isEmpty()) return null
        if (moduleClassNames.isEmpty()) return null

        return PreLoadedApk().apply {
            this.preLoadedDexes = preLoadedDexes
            this.moduleClassNames = moduleClassNames
            this.moduleLibraryNames = moduleLibraryNames
        }
    }
}