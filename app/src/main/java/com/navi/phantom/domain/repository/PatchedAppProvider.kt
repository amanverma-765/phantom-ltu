package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.PatchedApp
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing patched apps.
 *
 * Following clean architecture principles, the domain layer defines
 * the interface while the data layer provides the implementation.
 */
interface PatchedAppProvider {

    /**
     * Observe all patched apps. Emits a new list whenever the data changes.
     */
    fun observeAll(): Flow<List<PatchedApp>>

    /**
     * Get all patched apps (one-shot).
     */
    suspend fun getAll(): List<PatchedApp>

    /**
     * Get a patched app by package name.
     */
    suspend fun getByPackageName(packageName: String): PatchedApp?

    /**
     * Observe a specific patched app by package name.
     */
    fun observeByPackageName(packageName: String): Flow<PatchedApp?>

    /**
     * Save a patched app. If an app with the same package name exists, it will be updated.
     */
    suspend fun save(patchedApp: PatchedApp): Long

    /**
     * Save a newly patched app with the given details.
     * This is a convenience method for saving after a successful patch operation.
     */
    suspend fun saveFromPatch(
        packageName: String,
        appName: String,
        versionName: String,
        versionCode: Long,
        patchedApkPath: String,
        originalApkSizeBytes: Long,
        isSplitApk: Boolean,
        iconBytes: ByteArray? = null
    ): Long

    /**
     * Delete a patched app by package name.
     */
    suspend fun deleteByPackageName(packageName: String)

    /**
     * Delete a patched app by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Check if a patched app exists for the given package name.
     */
    suspend fun exists(packageName: String): Boolean

    /**
     * Observe the count of patched apps.
     */
    fun observeCount(): Flow<Int>
}