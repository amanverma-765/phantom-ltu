package de.robv.android.xposed.callbacks

import android.content.pm.ApplicationInfo

/**
 * Callbacks related to package loading.
 */
object XC_LoadPackage {

    /**
     * Parameters passed when a package is loaded.
     */
    class LoadPackageParam {
        /**
         * The name of the package being loaded.
         */
        var packageName: String = ""

        /**
         * The process name.
         */
        var processName: String = ""

        /**
         * The class loader for the package.
         */
        var classLoader: ClassLoader = ClassLoader.getSystemClassLoader()

        /**
         * Application info for the package.
         */
        var appInfo: ApplicationInfo? = null

        /**
         * Whether this is the first application in the process.
         */
        var isFirstApplication: Boolean = true
    }
}
