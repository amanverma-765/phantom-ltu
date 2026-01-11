package com.navi.phantom.shared

object Constants {
    // Asset Paths
    const val CONFIG_ASSET_PATH = "assets/lspatch/config.json"
    const val LOADER_DEX_ASSET_PATH = "assets/lspatch/loader.dex"
    const val META_LOADER_DEX_ASSET_PATH = "assets/lspatch/metaloader.dex"
    const val ORIGINAL_APK_ASSET_PATH = "assets/lspatch/origin.apk"
    const val EMBEDDED_MODULES_ASSET_PATH = "assets/lspatch/modules/"

    // File Naming
    const val PATCH_FILE_SUFFIX = "-lspatched.apk"
    const val PATCH_BUNDLE_SUFFIX = "-lspatched.apks"
    const val PROXY_APP_COMPONENT_FACTORY = "org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub"
    const val MANAGER_PACKAGE_NAME = "com.navi.phantom"
    const val MIN_ROLLING_VERSION_CODE = 348

    // Signature Bypass Levels
    const val SIGBYPASS_LV_DISABLE = 0
    const val SIGBYPASS_LV_PM = 1
    const val SIGBYPASS_LV_PM_OPENAT = 2
    const val SIGBYPASS_LV_MAX = 3
}