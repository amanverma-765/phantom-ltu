package com.navi.phantom.shared

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApksBundleHelper {

    fun isBundle(path: String?): Boolean =
        path?.lowercase()?.endsWith(Constants.BUNDLE_EXTENSION) == true

    fun isBundleAny(path: String?): Boolean {
        val lower = path?.lowercase() ?: return false
        return lower.endsWith(Constants.APKS_EXTENSION) || lower.endsWith(Constants.BUNDLE_EXTENSION)
    }

    @Deprecated("Use isBundle", ReplaceWith("isBundle(path)"))
    fun isApksBundle(path: String?): Boolean = isBundle(path)

    @Deprecated("Use isBundleAny", ReplaceWith("isBundleAny(path)"))
    fun isApksBundleAny(path: String?): Boolean = isBundleAny(path)

    @Throws(IOException::class)
    fun extractBundle(bundleFile: File, destDir: File): List<String> {
        require(bundleFile.exists()) { "Bundle file does not exist: ${bundleFile.absolutePath}" }
        destDir.mkdirs()
        return bundleFile.inputStream().use { extractBundle(it, destDir) }
    }

    @Throws(IOException::class)
    fun extractBundle(bundleStream: InputStream, destDir: File): List<String> {
        destDir.mkdirs()

        val extractedPaths = ZipInputStream(bundleStream).use { zis ->
            generateSequence { zis.nextEntry }
                .filter { !it.isDirectory && it.name.lowercase().endsWith(Constants.APK_EXTENSION) }
                .map { entry ->
                    val outFile = File(destDir, File(entry.name).name)
                    outFile.outputStream().use { zis.copyTo(it) }
                    zis.closeEntry()
                    outFile.absolutePath
                }
                .toList()
        }

        if (extractedPaths.isEmpty()) {
            throw IOException("No APK files found in bundle")
        }
        return extractedPaths
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun createBundle(
        apkFiles: List<File>,
        bundleFile: File,
        originalNames: Map<File, String> = emptyMap(),
        deleteSource: Boolean = false
    ) {
        ZipOutputStream(bundleFile.outputStream()).use { zos ->
            zos.setMethod(ZipOutputStream.STORED)

            apkFiles.forEach { apkFile ->
                val entryName = originalNames[apkFile] ?: apkFile.name
                val entry = ZipEntry(entryName).apply {
                    method = ZipEntry.STORED
                    size = apkFile.length()
                    compressedSize = apkFile.length()
                    crc = apkFile.crc32()
                }

                zos.putNextEntry(entry)
                Files.copy(apkFile.toPath(), zos)
                zos.closeEntry()
            }
        }

        if (deleteSource) {
            apkFiles.forEach { it.delete() }
        }
    }

    fun File.crc32(): Long {
        val crc = CRC32()
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            var len: Int
            while (input.read(buffer).also { len = it } != -1) {
                crc.update(buffer, 0, len)
            }
        }
        return crc.value
    }

    @JvmStatic
    @Throws(IOException::class)
    fun calculateCrc32(file: File): Long = file.crc32()
}
