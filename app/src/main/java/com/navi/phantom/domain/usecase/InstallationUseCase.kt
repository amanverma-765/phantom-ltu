package com.navi.phantom.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import co.touchlab.kermit.Logger
import com.navi.phantom.core.ext.getPackageInfoCompat
import com.navi.phantom.core.ext.isPackageInstalled
import com.navi.phantom.domain.error.InstallationError
import com.navi.phantom.domain.model.InstallationState
import com.navi.phantom.domain.model.UninstallState
import com.navi.phantom.domain.repository.ApkInstallationProvider
import com.navi.phantom.shared.ApksBundleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class InstallationUseCase(
    private val context: Context,
    private val installationProvider: ApkInstallationProvider
) {
    private val log = Logger.withTag("InstallationUseCase")
    private val pm: PackageManager get() = context.packageManager

    sealed interface PreflightResult {
        data object CanInstall : PreflightResult
        data class RequiresUninstall(val packageName: String, val reason: String) : PreflightResult
    }

    suspend fun runPreflightCheck(apkPath: String): PreflightResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "preflight-${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            val extractedPath = extractApkForPreflight(File(apkPath), tempDir)
                ?: return@withContext PreflightResult.CanInstall

            val apkInfo = pm.getPackageArchiveInfo(extractedPath, PackageManager.GET_SIGNING_CERTIFICATES)
                ?: run {
                    log.w { "Could not parse APK: $extractedPath" }
                    return@withContext PreflightResult.CanInstall
                }

            val packageName = apkInfo.packageName
            log.d { "Preflight: $packageName (v${apkInfo.longVersionCode})" }

            val installedInfo = try {
                pm.getPackageInfoCompat(packageName, PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            } catch (_: PackageManager.NameNotFoundException) {
                log.d { "Not installed, can proceed" }
                return@withContext PreflightResult.CanInstall
            }

            val apkSig = apkInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            val installedSig = installedInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            
            if (apkSig != null && installedSig != null && !apkSig.contentEquals(installedSig)) {
                log.d { "Signature mismatch for $packageName" }
                return@withContext PreflightResult.RequiresUninstall(packageName, "Different signature")
            }

            if (apkInfo.longVersionCode < installedInfo.longVersionCode) {
                log.d { "Downgrade: ${installedInfo.longVersionCode} -> ${apkInfo.longVersionCode}" }
                return@withContext PreflightResult.RequiresUninstall(packageName, "Version downgrade")
            }

            log.d { "Preflight passed" }
            PreflightResult.CanInstall
        } catch (e: Exception) {
            log.w(e) { "Preflight failed, proceeding anyway" }
            PreflightResult.CanInstall
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun extractApkForPreflight(source: File, tempDir: File): String? = try {
        when {
            ApksBundleHelper.isBundleAny(source.path) -> {
                ApksBundleHelper.extractBundle(source, tempDir)
                    .let { paths -> paths.find { "base" in it.lowercase() } ?: paths.firstOrNull() }
                    .also { if (it == null) log.w { "No base APK in bundle: ${source.name}" } }
            }
            else -> {
                val tempApk = File(tempDir, "base.apk")
                source.copyTo(tempApk, overwrite = true)
                tempApk.absolutePath
            }
        }
    } catch (e: Exception) {
        log.w(e) { "Extract failed: ${source.name}, skipping preflight" }
        null
    }

    fun install(path: String, appName: String): Flow<InstallationState> =
        installationProvider.install(path, appName)

    fun uninstall(packageName: String): Flow<UninstallState> =
        installationProvider.uninstall(packageName)

    suspend fun isInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        pm.isPackageInstalled(packageName)
    }

    suspend fun waitForUninstall(
        packageName: String,
        maxWaitMs: Long = 5000L,
        intervalMs: Long = 250L
    ): Boolean = withContext(Dispatchers.IO) {
        var elapsed = 0L
        while (elapsed < maxWaitMs && pm.isPackageInstalled(packageName)) {
            log.d { "Waiting for uninstall... (${elapsed}ms)" }
            delay(intervalMs)
            elapsed += intervalMs
        }
        !pm.isPackageInstalled(packageName)
    }

    suspend fun cleanupSessions() = installationProvider.cleanupOrphanedSessions()

    fun requiresUninstall(error: InstallationError): Boolean = when (error) {
        is InstallationError.Conflict,
        is InstallationError.Incompatible,
        is InstallationError.SignatureMismatch -> true
        else -> false
    }

    fun getConflictingPackage(error: InstallationError, fallback: String?): String? = when (error) {
        is InstallationError.Conflict -> error.otherPackageName ?: fallback
        is InstallationError.SignatureMismatch -> error.packageName
        else -> fallback
    }
}
