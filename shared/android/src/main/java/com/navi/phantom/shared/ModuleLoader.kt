package com.navi.phantom.shared

import android.os.SharedMemory
import android.system.ErrnoException
import android.system.OsConstants
import android.util.Log
import org.lsposed.lspd.models.PreLoadedApk
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

object ModuleLoader {

    private const val TAG = "LSPatch"

    private fun readDexes(apkFile: ZipFile, preLoadedDexes: MutableList<SharedMemory>) {
        var secondary = 2
        var dexFile = apkFile.getEntry("classes.dex")
        while (dexFile != null) {
            try {
                apkFile.getInputStream(dexFile).use { input ->
                    val memory = SharedMemory.create(null, input.available())
                    val byteBuffer = memory.mapReadWrite()
                    Channels.newChannel(input).read(byteBuffer)
                    SharedMemory.unmap(byteBuffer)
                    memory.setProtect(OsConstants.PROT_READ)
                    preLoadedDexes.add(memory)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Can not load $dexFile in $apkFile", e)
            } catch (e: ErrnoException) {
                Log.w(TAG, "Can not load $dexFile in $apkFile", e)
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
            Log.e(TAG, "Can not open $initEntry", e)
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
            Log.e(TAG, "Can not open $path", e)
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