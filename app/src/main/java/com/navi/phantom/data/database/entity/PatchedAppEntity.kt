package com.navi.phantom.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a successfully patched application.
 *
 * This stores the essential information about patched apps that can be displayed
 * on the home screen and used for subsequent operations like reinstallation.
 */
@Entity(
    tableName = "patched_apps",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class PatchedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Package name of the original app (e.g., "com.example.app") */
    val packageName: String,

    /** User-visible app name (e.g., "Example App") */
    val appName: String,

    /** Version name of the patched app (e.g., "1.2.3") */
    val versionName: String,

    /** Version code of the patched app */
    val versionCode: Long,

    /** Path to the patched APK or APKSB bundle file */
    val patchedApkPath: String,

    /** Original APK size in bytes (for display purposes) */
    val originalApkSizeBytes: Long,

    /** Timestamp when the app was patched */
    val patchedAtMillis: Long,

    /** Whether this is a split APK bundle (.apksb) or single APK */
    val isSplitApk: Boolean,

    /** App icon stored as PNG bytes */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val iconBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatchedAppEntity

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
