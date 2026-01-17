package com.navi.phantom.data.patcher

import android.content.Context
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.error.PatchingError
import com.navi.phantom.domain.model.PatchingOptions
import com.navi.phantom.domain.model.PatchingProgress
import com.navi.phantom.domain.model.PatchingStep
import com.navi.phantom.domain.repository.PatcherProvider
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
    return if (lastDot > 0) fileName.take(lastDot) else fileName
}

class PatcherProviderImpl(context: Context) : PatcherProvider {

    private val log = Logger.withTag("PatcherProviderImpl")
    private val outputDir: File = context.cacheDir.resolve(Constants.PATCHED_DIR).apply { mkdirs() }

    @Volatile
    private var isCancelled = false

    override fun patch(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        splitApkPaths: List<String>,
        options: PatchingOptions
    ): Flow<PatchingProgress> = callbackFlow {
        isCancelled = false

        withContext(Dispatchers.IO) {
            try {
                cleanupAllPatchedFiles()
                val isSplitApk = splitApkPaths.isNotEmpty()

                val outputPath = if (isSplitApk) {
                    patchBundle(packageName, versionCode, apkPath, splitApkPaths, options) { step, message ->
                        if (!isCancelled && isActive) trySend(PatchingProgress.Step(step, message))
                    }
                } else {
                    patchSingleApk(packageName, versionCode, apkPath, options) { step, message ->
                        if (!isCancelled && isActive) trySend(PatchingProgress.Step(step, message))
                    }
                }

                if (!isCancelled && isActive) trySend(PatchingProgress.Completed(outputPath))
            } catch (e: CancellationException) {
                log.i(e) { "Patching cancelled" }
                trySend(PatchingProgress.Cancelled)
            } catch (e: Throwable) {
                log.e(e) { "Patching failed" }
                trySend(PatchingProgress.Failed(PatchingError.from(e)))
            }
        }

        awaitClose { isCancelled = true }
    }

    override fun cancel() {
        isCancelled = true
    }

    private fun checkCancelled() {
        if (isCancelled) throw CancellationException("Patching cancelled")
    }

    private fun cleanupAllPatchedFiles() {
        outputDir.listFiles()
            ?.filter {
                it.name.endsWith(Constants.PATCH_FILE_SUFFIX) ||
                it.name.endsWith(Constants.PATCH_BUNDLE_SUFFIX)
            }
            ?.forEach { file ->
                log.d { "Deleting old patched file: ${file.name}" }
                file.delete()
            }
    }

    private fun patchSingleApk(
        packageName: String,
        versionCode: Long,
        apkPath: String,
        options: PatchingOptions,
        onStep: (PatchingStep, String) -> Unit
    ): String {
        val srcApkFile = File(apkPath)
        val outputFile = File(
            outputDir,
            String.format(Locale.getDefault(), "%s-%d%s", packageName, versionCode, Constants.PATCH_FILE_SUFFIX)
        )

        log.d { "Patching: $srcApkFile -> $outputFile" }

        createPatcher(options).patch(srcApkFile, outputFile) { step, details ->
            checkCancelled()
            mapPatcherStepToPatchingStep(step)?.let { onStep(it, details) }
        }

        return outputFile.absolutePath
    }

    private fun patchBundle(
        packageName: String,
        versionCode: Long,
        baseApkPath: String,
        splitApkPaths: List<String>,
        options: PatchingOptions,
        onStep: (PatchingStep, String) -> Unit
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
            val outputName = if (apkFileName.startsWith("split_")) baseName else packageName
            val outputFile = File(
                outputDir,
                String.format(Locale.getDefault(), "%s-%d-phantom.apk", outputName, versionCode)
            )

            log.d { "Patching APK ${index + 1}/${allApkPaths.size}: $srcApkFile" }

            patcher.patch(srcApkFile, outputFile) { step, details ->
                checkCancelled()
                mapPatcherStepToPatchingStep(step)?.let { patchingStep ->
                    val detailsWithProgress = if (allApkPaths.size > 1) "$details (${index + 1}/${allApkPaths.size})" else details
                    onStep(patchingStep, detailsWithProgress)
                }
            }

            patchedFiles.add(outputFile)
            originalNames[outputFile] = apkFileName
        }

        val bundleFile = File(
            outputDir,
            String.format(Locale.getDefault(), "%s-%d%s", packageName, versionCode, Constants.PATCH_BUNDLE_SUFFIX)
        )

        log.d { "Creating bundle: ${bundleFile.name}" }
        ApksBundleHelper.createBundle(patchedFiles, bundleFile, originalNames, true)

        onStep(PatchingStep.COMPLETE, bundleFile.name)
        return bundleFile.absolutePath
    }

    private fun createPatcher(options: PatchingOptions): PhantomPatcher {
        val args = mutableListOf<String>()
        if (options.debuggable) args.add("-d")
        if (options.sigbypassLevel > 0) {
            args.add("-l")
            args.add(options.sigbypassLevel.toString())
        }
        if (options.overrideVersionCode) args.add("-r")
        if (options.injectDex) args.add("--injectdex")
        args.add("-f")
        args.add("placeholder")
        return PhantomPatcher(args.toTypedArray())
    }

    private fun mapPatcherStepToPatchingStep(step: String): PatchingStep? =
        runCatching { PatchingStep.valueOf(step) }.getOrNull()
}