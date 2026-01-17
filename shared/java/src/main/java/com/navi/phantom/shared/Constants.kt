package com.navi.phantom.shared

object Constants {
    // Asset Paths
    const val CONFIG_ASSET_PATH = "assets/phantom/config.json"
    const val LOADER_DEX_ASSET_PATH = "assets/phantom/loader.dex"
    const val META_LOADER_DEX_ASSET_PATH = "assets/phantom/metaloader.dex"
    const val ORIGINAL_APK_ASSET_PATH = "assets/phantom/origin.apk"

    // Directory Names
    const val PATCHED_DIR = "patched"

    // File Naming - Patched output suffixes
    const val PATCH_FILE_SUFFIX = "-phantom.apk"
    const val PATCH_BUNDLE_SUFFIX = "-phantom.apks"

    // Pure extensions (for detection/checking)
    const val PATCH_EXTENSION = ".apk"
    const val BUNDLE_EXTENSION = ".apks"

    // Input formats (users provide these)
    const val APK_EXTENSION = ".apk"
    const val APKS_EXTENSION = ".apks"
    const val PROXY_APP_COMPONENT_FACTORY = "com.navi.phantom.metaloader.LSPAppComponentFactoryStub"
    const val MANAGER_PACKAGE_NAME = "com.navi.phantom"

    // Signature Bypass Levels
    const val SIGBYPASS_LV_DISABLE = 0
    const val SIGBYPASS_LV_PM = 1
    const val SIGBYPASS_LV_PM_OPENAT = 2
    const val SIGBYPASS_LV_MAX = 3

    // Installation Timeouts (milliseconds)
    const val INSTALL_TIMEOUT_MS = 120_000L
    const val UNINSTALL_POLL_TIMEOUT_MS = 60_000L
    const val UNINSTALL_POLL_INTERVAL_MS = 500L

    // Buffer sizes
    const val APK_COPY_BUFFER_SIZE = 65536
}