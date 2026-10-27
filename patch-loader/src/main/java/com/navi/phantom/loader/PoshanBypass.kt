package com.navi.phantom.loader

import android.content.Context
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

/**
 * Bypasses Poshan Tracker's in-house native integrity and root checking.
 * Targets com.poshantracker.rootcheck.NativeRootCheck and RNRootbeerModule.
 */
object PoshanBypass {

    private val log = Logger.withTag("Phantom-PoshanBypass")

    private const val NATIVE_ROOT_CHECK = "com.poshantracker.rootcheck.NativeRootCheck"
    private const val RN_ROOTBEER_MODULE = "com.poshantracker.RNRootbeerModule"

    @JvmStatic
    fun apply(classLoader: ClassLoader) {
        val rootCheckClass = XposedHelpers.findClassIfExists(NATIVE_ROOT_CHECK, classLoader)
        val rootbeerClass = XposedHelpers.findClassIfExists(RN_ROOTBEER_MODULE, classLoader)

        if (rootCheckClass == null && rootbeerClass == null) {
            log.d { "No Poshan protection detected, skipping" }
            return
        }

        if (rootCheckClass != null) {
            log.i { "Poshan NativeRootCheck detected, applying integrity bypass" }
            hookNativeRootCheck(classLoader)
        }

        if (rootbeerClass != null) {
            log.i { "Poshan RNRootbeerModule detected, applying rootbeer bypass" }
            hookRNRootbeerModule(classLoader)
        }
    }

    private fun hookNativeRootCheck(cl: ClassLoader) {
        // verifiedIntegrityVerdict(Context) -> 0 (clean verdict, skips SecurityBlockActivity)
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "verifiedIntegrityVerdict", 0, Context::class.java)
        // integrityVerdict(Context) -> 0
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "integrityVerdict", 0, Context::class.java)
        // verifySignature(byte[]) -> 0 (SIG_VERIFIED)
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "verifySignature", 0, ByteArray::class.java)
        // Booleans
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "isRooted", false)
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "isEmulator", false)
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "isInstrumented", false)
        // Indicators
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "rootIndicators", 0)
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "emulatorIndicators", 0)
        hookReturnConstant(cl, NATIVE_ROOT_CHECK, "instrumentationIndicators", 0)
        // selfEnforce(Context) -> do nothing
        hookDoNothing(cl, NATIVE_ROOT_CHECK, "selfEnforce", Context::class.java)
    }

    private fun hookRNRootbeerModule(cl: ClassLoader) {
        hookReturnConstant(cl, RN_ROOTBEER_MODULE, "strictRootBeerCheck", false)
        hookReturnConstant(cl, RN_ROOTBEER_MODULE, "isFridaDefaultPortOpen", false)
        hookReturnConstant(cl, RN_ROOTBEER_MODULE, "javaEmulatorHeuristics", false)
        hookReturnConstant(cl, RN_ROOTBEER_MODULE, "javaInstrumentationHeuristics", false)
    }

    private fun hookReturnConstant(
        cl: ClassLoader,
        className: String,
        methodName: String,
        value: Any?,
        vararg parameterTypes: Class<*>
    ) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, cl) ?: return
            if (parameterTypes.isEmpty()) {
                XposedHelpers.findAndHookMethod(clazz, methodName, XC_MethodReplacement.returnConstant(value))
            } else {
                XposedHelpers.findAndHookMethod(
                    clazz,
                    methodName,
                    *parameterTypes,
                    XC_MethodReplacement.returnConstant(value)
                )
            }
            log.d { "Hooked $className.$methodName -> $value" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook $className.$methodName" }
        }
    }

    private fun hookDoNothing(
        cl: ClassLoader,
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>
    ) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, cl) ?: return
            if (parameterTypes.isEmpty()) {
                XposedHelpers.findAndHookMethod(clazz, methodName, XC_MethodReplacement.DO_NOTHING)
            } else {
                XposedHelpers.findAndHookMethod(
                    clazz,
                    methodName,
                    *parameterTypes,
                    XC_MethodReplacement.DO_NOTHING
                )
            }
            log.d { "Hooked $className.$methodName -> DO_NOTHING" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook $className.$methodName" }
        }
    }
}
