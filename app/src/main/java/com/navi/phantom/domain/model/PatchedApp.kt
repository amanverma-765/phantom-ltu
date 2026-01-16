package com.navi.phantom.domain.model

import java.io.File
import java.util.Locale

/**
 * Domain model representing a patched application.
 *
 * This is the clean architecture domain representation that is used
 * throughout the app, separate from the database entity.
 */
data class PatchedApp(
    val id: Long,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val patchedApkPath: String,
    val originalApkSizeBytes: Long,
    val patchedAtMillis: Long,
    val isSplitApk: Boolean,
    val iconBytes: ByteArray?
) {
    val apkSizeMb: Double
        get() = originalApkSizeBytes / (1024.0 * 1024.0)

    val formattedSize: String
        get() = when {
            originalApkSizeBytes < 1024 -> "$originalApkSizeBytes B"
            originalApkSizeBytes < 1024 * 1024 -> "${originalApkSizeBytes / 1024} KB"
            else -> String.format(locale = Locale.ENGLISH, "%.1f MB", apkSizeMb)
        }

    val patchedApkExists: Boolean
        get() = File(patchedApkPath).exists()

    val fileExtension: String
        get() = if (isSplitApk) "apks" else "apk"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatchedApp

        if (id != other.id) return false
        if (packageName != other.packageName) return false
        if (appName != other.appName) return false
        if (versionName != other.versionName) return false
        if (versionCode != other.versionCode) return false
        if (patchedApkPath != other.patchedApkPath) return false
        if (originalApkSizeBytes != other.originalApkSizeBytes) return false
        if (patchedAtMillis != other.patchedAtMillis) return false
        if (isSplitApk != other.isSplitApk) return false
        if (iconBytes != null) {
            if (other.iconBytes == null) return false
            if (!iconBytes.contentEquals(other.iconBytes)) return false
        } else if (other.iconBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + versionCode.hashCode()
        result = 31 * result + patchedApkPath.hashCode()
        result = 31 * result + originalApkSizeBytes.hashCode()
        result = 31 * result + patchedAtMillis.hashCode()
        result = 31 * result + isSplitApk.hashCode()
        result = 31 * result + (iconBytes?.contentHashCode() ?: 0)
        return result
    }
}
