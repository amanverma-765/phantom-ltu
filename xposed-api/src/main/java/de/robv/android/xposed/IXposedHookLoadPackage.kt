package de.robv.android.xposed

import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Interface for Xposed modules that hook into package loading.
 */
interface IXposedHookLoadPackage {
    /**
     * Called when a package is loaded.
     * @param lpparam Information about the loaded package.
     */
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
}
