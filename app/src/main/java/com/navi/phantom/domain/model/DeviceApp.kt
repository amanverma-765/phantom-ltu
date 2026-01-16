package com.navi.phantom.domain.model

import android.graphics.drawable.Drawable

data class DeviceApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkPath: String,
    val usesLocation: Boolean
)
