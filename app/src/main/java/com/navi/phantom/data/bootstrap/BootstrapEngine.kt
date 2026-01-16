package com.navi.phantom.data.bootstrap

import android.content.Context
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.errors.BootstrapError
import com.navi.phantom.features.bootstrap.logic.BootstrapStep
import com.navi.phantom.patcher.PhantomPatcher
import com.navi.phantom.shared.ApksBundleHelper
import com.navi.phantom.shared.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

private fun getBaseName(fileName: String): String {
    val lastDot = fileName.lastIndexOf('.')
    return if (lastDot > 0) fileName.substring(0, lastDot) else fileName
}

class BootstrapEngine(private val context: Context) {

    private val log = Logger.withTag("BootstrapEngine")
    private val outputDir: File = context.filesDir.resolve("bootstrapped").apply { mkdirs() }

    @Volatile
    private var isCancelled = false

    fun bootstrap(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        splitApkPaths: List<String>,
        options: BootstrapOptions
    ): Flow<BootstrapProgress> = callbackFlow {
        isCancelled = false

        withContext(Dispatchers.IO) {
            try {
                val isSplitApk = splitApkPaths.isNotEmpty()

                val outputPath = if (isSplitApk) {
                    bootstrapBundle(packageName, versionCode, apkPath, splitApkPaths, options) { step, message ->
                        if (!isCancelled && isActive) {
                            trySend(BootstrapProgress.Step(step, message))
                        }
                    }
                } else {
                    bootstrapSingleApk(packageName, versionCode, apkPath, options) { step, message ->
                        if (!isCancelled && isActive) {
                            trySend(BootstrapProgress.Step(step, message))
                        }
                    }
                }

                // Send completion event
                if (!isCancelled && isActive) {
                    trySend(BootstrapProgress.Completed(outputPath))
                }
            } catch (e: CancellationException) {
                log.i { "Bootstrap cancelled" }
                trySend(BootstrapProgress.Cancelled)
            } catch (e: Throwable) {
                // Catch Throwable to handle both Exception and Error types
                // (e.g., PatchError, OutOfMemoryError, etc.)
                log.e(e) { "Bootstrap failed" }
                val bootstrapError = BootstrapError.fromThrowable(e)
                trySend(BootstrapProgress.Failed(bootstrapError))
            }
        }

        awaitClose { isCancelled = true }
    }

    fun cancel() {
        isCancelled = true
    }

    private fun checkCancelled() {
        if (isCancelled) throw CancellationException("Bootstrap cancelled")
    }

    private fun cleanupExistingPatchedFiles(packageName: String, exceptFile: File) {
        outputDir.listFiles()
            ?.filter { it.name.startsWith(packageName) && it.absolutePath != exceptFile.absolutePath }
            ?.forEach { file ->
                log.d { "Deleting old patched file: ${file.name}" }
                file.delete()
            }
    }

    private fun bootstrapSingleApk(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        options: BootstrapOptions,
        onStep: (BootstrapStep, String) -> Unit
    ): String {
        val srcApkFile = File(apkPath)
        val outputFile = File(
            outputDir,
            String.format(
                Locale.getDefault(),
                "%s-%d%s",
                packageName,
                versionCode,
                Constants.PATCH_FILE_SUFFIX
            )
        )

        log.d { "Bootstrapping: $srcApkFile -> $outputFile" }

        val patcher = createPatcher(options)
        patcher.patch(srcApkFile, outputFile) { step, details ->
            checkCancelled()
            val bootstrapStep = mapPatcherStepToBootstrapStep(step)
            if (bootstrapStep != null) {
                onStep(bootstrapStep, details)
            }
        }

        // Cleanup old versions only after successful patching
        cleanupExistingPatchedFiles(packageName, outputFile)

        return outputFile.absolutePath
    }

    private fun bootstrapBundle(
        packageName: String,
        versionCode: Long,
        baseApkPath: String,
        splitApkPaths: List<String>,
        options: BootstrapOptions,
        onStep: (BootstrapStep, String) -> Unit
    ): String {
        val allApkPaths = listOf(baseApkPath) + splitApkPaths
        val patchedFiles = mutableListOf<File>()
        val originalNames = mutableMapOf<File, String>()

        val patcher = createPatcher(options)

        for ((index, apkPath) in allApkPaths.withIndex()) {
            checkCancelled()

            val srcApkFile = File(apkPath)
            val apkFileName = srcApkFile.name
            val baseName = getBaseName(apkFileName)

            // Use package name for base APK, original name for splits
            // Keep .apk extension for files inside bundle (needed for extraction/installation)
            val outputName = if (apkFileName.startsWith("split_")) baseName else packageName
            val outputFile = File(
                outputDir,
                String.format(
                    Locale.getDefault(),
                    "%s-%d-phantom.apk",
                    outputName,
                    versionCode
                )
            )

            log.d { "Bootstrapping APK ${index + 1}/${allApkPaths.size}: $srcApkFile" }

            patcher.patch(srcApkFile, outputFile) { step, details ->
                checkCancelled()
                val bootstrapStep = mapPatcherStepToBootstrapStep(step)
                if (bootstrapStep != null) {
                    val detailsWithProgress = if (allApkPaths.size > 1) {
                        "$details (${index + 1}/${allApkPaths.size})"
                    } else {
                        details
                    }
                    onStep(bootstrapStep, detailsWithProgress)
                }
            }

            patchedFiles.add(outputFile)
            originalNames[outputFile] = apkFileName
        }

        val bundleFile = File(
            outputDir,
            String.format(
                Locale.getDefault(),
                "%s-%d%s",
                packageName,
                versionCode,
                Constants.PATCH_BUNDLE_SUFFIX
            )
        )

        log.d { "Creating bundle: ${bundleFile.name}" }
        ApksBundleHelper.createBundle(patchedFiles, bundleFile, originalNames, true)

        // Cleanup old versions only after successful bundle creation
        cleanupExistingPatchedFiles(packageName, bundleFile)

        onStep(BootstrapStep.COMPLETE, bundleFile.name)
        return bundleFile.absolutePath
    }

    private fun createPatcher(options: BootstrapOptions): PhantomPatcher {
        val args = mutableListOf<String>()

        if (options.debuggable) {
            args.add("-d")
        }
        if (options.sigbypassLevel > 0) {
            args.add("-l")
            args.add(options.sigbypassLevel.toString())
        }
        if (options.overrideVersionCode) {
            args.add("-r")
        }
        args.add("-f") // Force overwrite
        args.add("placeholder") // PhantomPatcher expects at least one APK path in constructor

        return PhantomPatcher(args.toTypedArray())
    }

    private fun mapPatcherStepToBootstrapStep(step: String): BootstrapStep? =
        runCatching { BootstrapStep.valueOf(step) }.getOrNull()
}
