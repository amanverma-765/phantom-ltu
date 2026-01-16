package com.navi.phantom.data.apps.dto

import android.graphics.drawable.Drawable

data class DeviceAppDto(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkPath: String,
    val usesLocation: Boolean
)
