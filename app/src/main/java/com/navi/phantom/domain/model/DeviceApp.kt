package com.navi.phantom.domain.model

import android.graphics.drawable.Drawable
import java.util.Locale

data class DeviceApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val apkPath: String,
    val apkSizeBytes: Long,
    val installTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val usesLocation: Boolean,
    val isPatched: Boolean
) {
    val apkSizeMb: Double
        get() = apkSizeBytes / (1024.0 * 1024.0)

    val formattedSize: String
        get() = when {
            apkSizeBytes < 1024 -> "$apkSizeBytes B"
            apkSizeBytes < 1024 * 1024 -> "${apkSizeBytes / 1024} KB"
            else -> String.format(locale = Locale.ENGLISH, "%.1f MB", apkSizeMb)
        }
}