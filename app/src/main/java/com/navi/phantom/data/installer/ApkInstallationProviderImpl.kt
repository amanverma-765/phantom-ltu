package com.navi.phantom.data.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import co.touchlab.kermit.Logger
import com.navi.phantom.core.ext.isPackageInstalled
import com.navi.phantom.domain.error.InstallationError
import com.navi.phantom.domain.model.InstallationState
import com.navi.phantom.domain.model.UninstallState
import com.navi.phantom.domain.repository.ApkInstallationProvider
import com.navi.phantom.receiver.InstallResult
import com.navi.phantom.receiver.InstallStatusReceiver
import com.navi.phantom.shared.ApksBundleHelper
import com.navi.phantom.shared.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class ApkInstallationProviderImpl(private val context: Context) : ApkInstallationProvider {

    private val log = Logger.withTag("ApkInstallationProviderImpl")
    private val pm: PackageManager get() = context.packageManager
    private val packageInstaller: PackageInstaller get() = pm.packageInstaller

    override fun install(path: String, appName: String): Flow<InstallationState> = channelFlow {
        send(InstallationState.Preparing)

        val file = File(path)
        if (!file.exists()) {
            send(InstallationState.Failed(InstallationError.FileNotFound(path)))
            return@channelFlow
        }

        try {
            if (ApksBundleHelper.isBundleAny(path)) {
                installBundle(file, appName)
            } else {
                installSingleApk(file, appName)
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            log.e(e) { "Installation failed" }
            send(InstallationState.Failed(InstallationError.Exceptional(e)))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.channels.ProducerScope<InstallationState>.installSingleApk(
        file: File,
        appName: String
    ) {
        log.d { "Installing single APK: ${file.name}" }

        val installFile = if (file.name.endsWith(Constants.PATCH_EXTENSION, ignoreCase = true)) {
            val tempApk = context.cacheDir.resolve("install-${System.currentTimeMillis()}.apk")
            file.copyTo(tempApk, overwrite = true)
            log.d { "Copied .phn to temp .apk: ${tempApk.name}" }
            tempApk
        } else {
            file
        }

        installApkFiles(listOf(installFile), appName)
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<InstallationState>.installBundle(
        bundleFile: File,
        appName: String
    ) {
        log.d { "Installing bundle: ${bundleFile.name}" }

        val tempDir = context.cacheDir.resolve("install-temp-${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            val apkPaths = ApksBundleHelper.extractBundle(bundleFile, tempDir)
            log.d { "Extracted ${apkPaths.size} APKs from bundle" }

            if (apkPaths.isEmpty()) {
                send(InstallationState.Failed(InstallationError.InvalidPackage("No APKs found in bundle")))
                return
            }

            installApkFiles(apkPaths.map { File(it) }, appName)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            log.e(e) { "Failed to extract/install bundle" }
            send(InstallationState.Failed(InstallationError.InvalidPackage(e.message ?: "Bundle extraction failed", e)))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<InstallationState>.installApkFiles(
        apkFiles: List<File>,
        appName: String
    ) {
        var sessionId = -1
        var session: PackageInstaller.Session? = null

        try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            sessionId = packageInstaller.createSession(params)
            log.d { "Created session: $sessionId" }

            session = packageInstaller.openSession(sessionId)
            val totalSize = apkFiles.sumOf { it.length() }
            var writtenSize = 0L

            for ((index, apkFile) in apkFiles.withIndex()) {
                currentCoroutineContext().ensureActive()
                val apkName = if (index == 0) "base.apk" else "split_$index.apk"
                log.d { "Writing: ${apkFile.name} as $apkName (${apkFile.length()} bytes)" }

                session.openWrite(apkName, 0, apkFile.length()).use { outputStream ->
                    apkFile.inputStream().use { inputStream ->
                        val buffer = ByteArray(Constants.APK_COPY_BUFFER_SIZE)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            outputStream.write(buffer, 0, bytesRead)
                            writtenSize += bytesRead
                            val progress = ((writtenSize.toFloat() / totalSize) * 50).toInt()
                            send(InstallationState.Installing(progress, 100))
                        }
                    }
                    outputStream.flush()
                }
            }

            send(InstallationState.Installing(50, 100))

            when (val result = commitAndWaitForResult(session, sessionId)) {
                is InstallResult.Success -> {
                    log.i { "Installation succeeded" }
                    send(InstallationState.Succeeded)
                    cleanupTempFiles()
                }
                is InstallResult.Failure -> {
                    log.e { "Installation failed: ${result.status}, ${result.message}" }
                    send(InstallationState.Failed(mapStatusToError(result.status, result.message)))
                }
                is InstallResult.UserActionRequired -> {
                    log.i { "Launching user confirmation" }
                    send(InstallationState.AwaitingConfirmation)
                    result.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(result.intent)

                    when (val finalResult = waitForFinalResult()) {
                        is InstallResult.Success -> {
                            log.i { "Installation succeeded after confirmation" }
                            send(InstallationState.Succeeded)
                            cleanupTempFiles()
                        }
                        is InstallResult.Failure -> {
                            log.e { "Installation failed: ${finalResult.status}" }
                            send(InstallationState.Failed(mapStatusToError(finalResult.status, finalResult.message)))
                        }
                        is InstallResult.UserActionRequired -> {
                            log.e { "Unexpected second user action" }
                            send(InstallationState.Failed(InstallationError.Generic("Installation cancelled")))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            log.e(e) { "Installation error" }
            send(InstallationState.Failed(InstallationError.Exceptional(e)))
            if (sessionId != -1) {
                runCatching { packageInstaller.abandonSession(sessionId) }
                    .onFailure { log.w(it) { "Failed to abandon session $sessionId" } }
            }
        } finally {
            session?.close()
        }
    }

    private suspend fun commitAndWaitForResult(session: PackageInstaller.Session, sessionId: Int): InstallResult {
        while (InstallStatusReceiver.resultChannel.tryReceive().isSuccess) { }

        val intent = Intent(InstallStatusReceiver.ACTION_INSTALL_STATUS).apply {
            setPackage(context.packageName)
            putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, sessionId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        log.d { "Committing session $sessionId" }
        session.commit(pendingIntent.intentSender)

        return withTimeoutOrNull(Constants.INSTALL_TIMEOUT_MS) {
            InstallStatusReceiver.resultChannel.receive()
        } ?: InstallResult.Failure(PackageInstaller.STATUS_FAILURE_TIMEOUT, "Installation timed out")
    }

    private suspend fun waitForFinalResult(): InstallResult {
        log.d { "Waiting for user confirmation..." }
        return withTimeoutOrNull(Constants.INSTALL_TIMEOUT_MS) {
            InstallStatusReceiver.resultChannel.receive()
        } ?: InstallResult.Failure(PackageInstaller.STATUS_FAILURE_TIMEOUT, "Timed out waiting for confirmation")
    }

    override fun uninstall(packageName: String): Flow<UninstallState> = channelFlow {
        send(UninstallState.Preparing)
        log.d { "Starting uninstall: $packageName" }

        try {
            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(uninstallIntent)

            if (pollForUninstall(packageName)) {
                log.i { "Uninstall succeeded: $packageName" }
                send(UninstallState.Succeeded)
            } else {
                log.e { "Uninstall failed: $packageName" }
                send(UninstallState.Failed("Uninstall was cancelled or failed"))
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            log.e(e) { "Uninstall failed" }
            send(UninstallState.Failed(e.message ?: "Uninstall failed", e))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun pollForUninstall(packageName: String): Boolean {
        if (!pm.isPackageInstalled(packageName)) return true
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < Constants.UNINSTALL_POLL_TIMEOUT_MS) {
            delay(Constants.UNINSTALL_POLL_INTERVAL_MS)
            if (!pm.isPackageInstalled(packageName)) return true
        }
        return !pm.isPackageInstalled(packageName)
    }

    override suspend fun cleanupOrphanedSessions() = withContext(Dispatchers.IO) {
        try {
            val sessions = packageInstaller.mySessions
            if (sessions.isEmpty()) {
                log.d { "No sessions to cleanup" }
                return@withContext
            }
            log.d { "Abandoning ${sessions.size} session(s)" }
            sessions.forEach { session ->
                runCatching { packageInstaller.abandonSession(session.sessionId) }
                    .onFailure { log.w(it) { "Failed to abandon session ${session.sessionId}" } }
            }
        } catch (e: Exception) {
            log.w(e) { "Failed to cleanup sessions" }
        }
    }

    private fun cleanupTempFiles() {
        runCatching {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("install-") && (file.name.endsWith(".apk") || file.isDirectory)) {
                    file.deleteRecursively()
                }
            }
        }.onFailure { log.w(it) { "Failed to cleanup temp files" } }
    }

    private fun mapStatusToError(status: Int, message: String?): InstallationError = when (status) {
        PackageInstaller.STATUS_FAILURE -> InstallationError.Generic(message ?: "Installation failed")
        PackageInstaller.STATUS_FAILURE_ABORTED -> InstallationError.Aborted(message)
        PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallationError.Blocked(message)
        PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallationError.Conflict(message)
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallationError.Incompatible(message)
        PackageInstaller.STATUS_FAILURE_INVALID -> InstallationError.Invalid(message)
        PackageInstaller.STATUS_FAILURE_STORAGE -> InstallationError.Storage(message)
        PackageInstaller.STATUS_FAILURE_TIMEOUT -> InstallationError.Timeout()
        else -> InstallationError.Generic(message ?: "Unknown error: $status")
    }
}
