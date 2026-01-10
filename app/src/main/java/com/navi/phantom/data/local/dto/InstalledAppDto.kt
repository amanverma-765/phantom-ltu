package com.navi.phantom.data.local.dto

import android.graphics.drawable.Drawable


data class InstalledAppDto(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkPath: String
)