package com.navi.phantom.loader

import android.app.ActivityThread
import android.app.LoadedApk
import android.content.res.XResources
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

object LSPLoader {

    @JvmStatic
    fun initModules(loadedApk: LoadedApk) {
        XposedInit.loadedPackagesInProcess.add(loadedApk.packageName)
        XResources.setPackageNameForResDir(loadedApk.packageName, loadedApk.resDir)

        val lpparam = XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks).apply {
            packageName = loadedApk.packageName
            processName = ActivityThread.currentProcessName()
            classLoader = loadedApk.classLoader
            appInfo = loadedApk.applicationInfo
            isFirstApplication = true
        }
        XC_LoadPackage.callAll(lpparam)
    }
}
