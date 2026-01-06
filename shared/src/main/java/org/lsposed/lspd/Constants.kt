package org.lsposed.lspd

/**
 * Shared constants used for IPC between manager and patched apps.
 * App-specific constants are in org.lsposed.lspatch.AppConstants.
 */
object Constants {
    // Package and service names
    const val MANAGER_PACKAGE_NAME = "org.lsposed.lspatch"
    const val MODULE_SERVICE_NAME = "$MANAGER_PACKAGE_NAME.service.ModuleService"

    // LSPatch asset paths (used for detection)
    const val CONFIG_ASSET_PATH = "assets/lspatch/config.json"
    const val LOADER_DEX_ASSET_PATH = "assets/lspatch/loader.dex"
    const val METALOADER_DEX_ASSET_PATH = "assets/lspatch/metaloader.dex"
    const val LSPATCH_LOADER_LIB = "liblspatch.so"

    // Module package prefix for built-in patches
    const val BUILTIN_MODULE_PREFIX = "builtin."

    // DEX file constants
    const val PRIMARY_DEX_NAME = "classes.dex"
    const val SECONDARY_DEX_PREFIX = "classes"
    const val DEX_EXTENSION = ".dex"

    // LSPatch markers for detecting patched apps
    val LSPATCH_MARKERS = listOf(
        CONFIG_ASSET_PATH,
        LOADER_DEX_ASSET_PATH,
        METALOADER_DEX_ASSET_PATH
    )
}
