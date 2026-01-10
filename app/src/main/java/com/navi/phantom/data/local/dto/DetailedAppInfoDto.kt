package com.navi.phantom.data.local.dto

import android.graphics.drawable.Drawable

data class DetailedAppInfoDto(
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
    val usesLocation: Boolean
)
