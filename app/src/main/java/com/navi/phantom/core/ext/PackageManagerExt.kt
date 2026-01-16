package com.navi.phantom.core.ext

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getApplicationInfoCompat(
    packageName: String,
    flags: Long = 0L
): ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags))
} else {
    @Suppress("DEPRECATION")
    getApplicationInfo(packageName, flags.toInt())
}

fun PackageManager.getPackageInfoCompat(
    packageName: String,
    flags: Long = 0L
): PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
} else {
    @Suppress("DEPRECATION")
    getPackageInfo(packageName, flags.toInt())
}

fun PackageManager.getInstalledApplicationsCompat(
    flags: Long = 0L
): List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags))
} else {
    @Suppress("DEPRECATION")
    getInstalledApplications(flags.toInt())
}

fun PackageManager.isPackageInstalled(packageName: String): Boolean = try {
    getPackageInfoCompat(packageName)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
