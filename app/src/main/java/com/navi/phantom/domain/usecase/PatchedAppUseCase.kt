package com.navi.phantom.domain.usecase

import com.navi.phantom.domain.model.PatchedApp
import com.navi.phantom.domain.repository.PatchedAppProvider
import kotlinx.coroutines.flow.Flow

/**
 * Use case for managing patched apps.
 *
 * Provides business logic for patched app operations while keeping
 * ViewModels lean and focused on UI state management.
 */
class PatchedAppUseCase(
    private val patchedAppProvider: PatchedAppProvider
) {

    /**
     * Observe all patched apps.
     */
    fun observeAll(): Flow<List<PatchedApp>> =
        patchedAppProvider.observeAll()

    /**
     * Get all patched apps (one-shot).
     */
    suspend fun getAll(): List<PatchedApp> =
        patchedAppProvider.getAll()

    /**
     * Get a patched app by package name.
     */
    suspend fun getByPackageName(packageName: String): PatchedApp? =
        patchedAppProvider.getByPackageName(packageName)

    /**
     * Save a newly patched app after successful patching.
     *
     * @return The ID of the saved patched app
     */
    suspend fun savePatchedApp(
        packageName: String,
        appName: String,
        versionName: String,
        versionCode: Long,
        patchedApkPath: String,
        originalApkSizeBytes: Long,
        isSplitApk: Boolean,
        iconBytes: ByteArray? = null
    ): Long = patchedAppProvider.saveFromPatch(
        packageName = packageName,
        appName = appName,
        versionName = versionName,
        versionCode = versionCode,
        patchedApkPath = patchedApkPath,
        originalApkSizeBytes = originalApkSizeBytes,
        isSplitApk = isSplitApk,
        iconBytes = iconBytes
    )

    /**
     * Delete a patched app by package name.
     */
    suspend fun deleteByPackageName(packageName: String) =
        patchedAppProvider.deleteByPackageName(packageName)

    /**
     * Delete a patched app by ID.
     */
    suspend fun deleteById(id: Long) =
        patchedAppProvider.deleteById(id)

    /**
     * Check if a patched app exists.
     */
    suspend fun exists(packageName: String): Boolean =
        patchedAppProvider.exists(packageName)

    /**
     * Observe the count of patched apps.
     */
    fun observeCount(): Flow<Int> =
        patchedAppProvider.observeCount()
}