package com.navi.phantom.loader

import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * Prevents System.exit() and Runtime.exit() from terminating the process.
 * Must be applied before any other bypass that might trigger PairIP class loading,
 * since PairIP can call System.exit() during its detection checks.
 */
object ExitBypass {

    private val log = Logger.withTag("Phantom-Exit")

    @JvmStatic
    fun apply() {
        hookExit(System::class.java, "System")
        hookExit(Runtime::class.java, "Runtime")
    }

    private fun hookExit(clazz: Class<*>, name: String) {
        try {
            XposedHelpers.findAndHookMethod(
                clazz, "exit", Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        log.d { "Blocked $name.exit(${param.args[0]})" }
                        param.result = null
                    }
                }
            )
            log.d { "Hooked $name.exit()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook $name.exit()" }
        }
    }
}
