package org.lsposed.lspd.nativebridge;

/**
 * JNI bridge for native-level signature bypass hooks.
 */
public class SigBypass {

    /**
     * Enables native openat() hook for APK file redirection.
     *
     * @param origApkPath  Path to the currently installed (patched) APK
     * @param cacheApkPath Path to the cached original APK to redirect to
     */
    public static native void enableOpenatHook(String origApkPath, String cacheApkPath);
}
