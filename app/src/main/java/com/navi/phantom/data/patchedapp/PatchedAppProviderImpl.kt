package com.navi.phantom.data.patchedapp

import com.navi.phantom.data.database.dao.PatchedAppDao
import com.navi.phantom.data.database.entity.PatchedAppEntity
import com.navi.phantom.domain.model.PatchedApp
import com.navi.phantom.domain.repository.PatchedAppProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of PatchedAppRepository using Room database.
 */
class PatchedAppProviderImpl(
    private val patchedAppDao: PatchedAppDao
) : PatchedAppProvider {

    override fun observeAll(): Flow<List<PatchedApp>> =
        patchedAppDao.observeAll().map { entities -> entities.toDomain() }

    override suspend fun getAll(): List<PatchedApp> =
        patchedAppDao.getAll().toDomain()

    override suspend fun getByPackageName(packageName: String): PatchedApp? =
        patchedAppDao.getByPackageName(packageName)?.toDomain()

    override fun observeByPackageName(packageName: String): Flow<PatchedApp?> =
        patchedAppDao.observeByPackageName(packageName).map { it?.toDomain() }

    override suspend fun save(patchedApp: PatchedApp): Long =
        patchedAppDao.insert(patchedApp.toEntity())

    override suspend fun saveFromPatch(
        packageName: String,
        appName: String,
        versionName: String,
        versionCode: Long,
        patchedApkPath: String,
        originalApkSizeBytes: Long,
        isSplitApk: Boolean,
        iconBytes: ByteArray?
    ): Long {
        val entity = PatchedAppEntity(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            versionCode = versionCode,
            patchedApkPath = patchedApkPath,
            originalApkSizeBytes = originalApkSizeBytes,
            patchedAtMillis = System.currentTimeMillis(),
            isSplitApk = isSplitApk,
            iconBytes = iconBytes
        )
        return patchedAppDao.insert(entity)
    }

    override suspend fun deleteByPackageName(packageName: String) =
        patchedAppDao.deleteByPackageName(packageName)

    override suspend fun deleteById(id: Long) =
        patchedAppDao.deleteById(id)

    override suspend fun exists(packageName: String): Boolean =
        patchedAppDao.exists(packageName)

    override fun observeCount(): Flow<Int> =
        patchedAppDao.observeCount()
}