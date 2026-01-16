package com.navi.phantom.data.bootstrap

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.ApksBundleHelper
import com.navi.phantom.shared.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ApkInstaller(private val context: Context) {

    private val log = Logger.withTag("ApkInstaller")

    suspend fun install(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (!file.exists()) {
                throw IllegalArgumentException("File does not exist: $path")
            }

            if (ApksBundleHelper.isApksBundleAny(path)) {
                installBundle(file)
            } else {
                installSingleApk(file)
            }
        }
    }

    private fun installSingleApk(file: File) {
        log.d { "Installing single APK: ${file.name}" }

        // Android package installer may reject non-.apk extensions
        // Copy .phn files to temp with .apk extension
        val installFile = if (file.name.endsWith(Constants.PATCH_EXTENSION, ignoreCase = true)) {
            val tempApk = context.cacheDir.resolve("install-${System.currentTimeMillis()}.apk")
            file.copyTo(tempApk, overwrite = true)
            log.d { "Copied .phn to temp .apk: ${tempApk.name}" }
            tempApk
        } else {
            file
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            installFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    private fun installBundle(bundleFile: File) {
        log.d { "Installing bundle: ${bundleFile.name}" }

        val tempDir = context.cacheDir.resolve("install-temp-${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            val apkPaths = ApksBundleHelper.extractBundle(bundleFile, tempDir)
            log.d { "Extracted ${apkPaths.size} APKs from bundle" }

            // For split APKs, we need to use PackageInstaller API
            // For now, we'll just install the base APK and inform user about split APKs
            val baseApk = apkPaths.find { !File(it).name.startsWith("split_") }
            if (baseApk != null) {
                installSingleApk(File(baseApk))
            } else if (apkPaths.isNotEmpty()) {
                installSingleApk(File(apkPaths.first()))
            }
        } finally {
            // Cleanup will happen when cache is cleared
            // We can't delete immediately as the installer needs the file
        }
    }
}
