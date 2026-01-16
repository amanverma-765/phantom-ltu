package com.navi.phantom.data.patchedapp

import com.navi.phantom.data.database.entity.PatchedAppEntity
import com.navi.phantom.domain.model.PatchedApp

/**
 * Mapper functions for converting between PatchedApp domain model and PatchedAppEntity.
 */

fun PatchedAppEntity.toDomain(): PatchedApp = PatchedApp(
    id = id,
    packageName = packageName,
    appName = appName,
    versionName = versionName,
    versionCode = versionCode,
    patchedApkPath = patchedApkPath,
    originalApkSizeBytes = originalApkSizeBytes,
    patchedAtMillis = patchedAtMillis,
    isSplitApk = isSplitApk,
    iconBytes = iconBytes
)

fun PatchedApp.toEntity(): PatchedAppEntity = PatchedAppEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    versionName = versionName,
    versionCode = versionCode,
    patchedApkPath = patchedApkPath,
    originalApkSizeBytes = originalApkSizeBytes,
    patchedAtMillis = patchedAtMillis,
    isSplitApk = isSplitApk,
    iconBytes = iconBytes
)

fun List<PatchedAppEntity>.toDomain(): List<PatchedApp> = map { it.toDomain() }
