package com.navi.phantom.domain.models

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkPath: String
)