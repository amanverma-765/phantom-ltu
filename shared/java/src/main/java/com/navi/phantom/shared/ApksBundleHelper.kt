package com.navi.phantom.shared

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApksBundleHelper {

    fun isApksBundle(path: String?): Boolean {
        return path != null && path.lowercase(Locale.ROOT).endsWith(Constants.BUNDLE_EXTENSION)
    }

    fun isApksBundleAny(path: String?): Boolean {
        if (path == null) return false
        val lower = path.lowercase(Locale.ROOT)
        return lower.endsWith(Constants.APKS_EXTENSION) || lower.endsWith(Constants.BUNDLE_EXTENSION)
    }

    @Throws(IOException::class)
    fun extractBundle(bundleFile: File, destDir: File): List<String> {
        if (!bundleFile.exists()) {
            throw IOException("Bundle file does not exist: ${bundleFile.absolutePath}")
        }

        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        val extractedPaths = mutableListOf<String>()

        ZipInputStream(FileInputStream(bundleFile)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory || !name.lowercase(Locale.ROOT).endsWith(Constants.APK_EXTENSION)) {
                    zis.closeEntry()
                    continue
                }

                val fileName = File(name).name
                val outFile = File(destDir, fileName)

                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }

                extractedPaths.add(outFile.absolutePath)
                zis.closeEntry()
            }
        }

        if (extractedPaths.isEmpty()) {
            throw IOException("No APK files found in bundle: ${bundleFile.absolutePath}")
        }

        return extractedPaths
    }

    @Throws(IOException::class)
    fun extractBundle(bundleStream: InputStream, destDir: File): List<String> {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        val extractedPaths = mutableListOf<String>()

        ZipInputStream(bundleStream).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory || !name.lowercase(Locale.ROOT).endsWith(Constants.APK_EXTENSION)) {
                    zis.closeEntry()
                    continue
                }

                val fileName = File(name).name
                val outFile = File(destDir, fileName)

                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }

                extractedPaths.add(outFile.absolutePath)
                zis.closeEntry()
            }
        }

        if (extractedPaths.isEmpty()) {
            throw IOException("No APK files found in bundle")
        }

        return extractedPaths
    }

    @Throws(IOException::class)
    fun createBundle(apkFiles: List<File>, bundleFile: File) {
        createBundle(apkFiles, bundleFile, emptyMap())
    }

    @Throws(IOException::class)
    fun createBundle(apkFiles: List<File>, bundleFile: File, originalNames: Map<File, String>) {
        ZipOutputStream(FileOutputStream(bundleFile)).use { zos ->
            zos.setMethod(ZipOutputStream.STORED)

            for (apkFile in apkFiles) {
                // Use original name if provided, otherwise use current file name
                val entryName = originalNames[apkFile] ?: apkFile.name
                val entry = ZipEntry(entryName).apply {
                    method = ZipEntry.STORED
                    size = apkFile.length()
                    compressedSize = apkFile.length()
                    crc = calculateCrc32(apkFile)
                }

                zos.putNextEntry(entry)
                Files.copy(apkFile.toPath(), zos)
                zos.closeEntry()
            }
        }
    }

    @Throws(IOException::class)
    fun createBundle(apkFiles: List<File>, bundleFile: File, deleteSourceApks: Boolean) {
        createBundle(apkFiles, bundleFile, emptyMap())

        if (deleteSourceApks) {
            for (apkFile in apkFiles) {
                apkFile.delete()
            }
        }
    }

    @Throws(IOException::class)
    fun createBundle(apkFiles: List<File>, bundleFile: File, originalNames: Map<File, String>, deleteSourceApks: Boolean) {
        createBundle(apkFiles, bundleFile, originalNames)

        if (deleteSourceApks) {
            for (apkFile in apkFiles) {
                apkFile.delete()
            }
        }
    }

    @Throws(IOException::class)
    fun calculateCrc32(file: File): Long {
        val crc = CRC32()
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var len: Int
            while (fis.read(buffer).also { len = it } != -1) {
                crc.update(buffer, 0, len)
            }
        }
        return crc.value
    }
}