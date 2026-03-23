package com.navi.phantom.loader

import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Hides Xposed framework classes from detection by the patched app.
 * Hooks class loading to throw ClassNotFoundException for Xposed-related classes
 * and scrubs stack traces to remove Xposed frames.
 */
object XposedHidingBypass {

    private val log = Logger.withTag("Phantom-XposedHiding")

    private val BLOCKED_PREFIXES = arrayOf(
        "de.robv.android.xposed.",
        "org.lsposed.",
        "com.navi.phantom.loader.",
        "com.navi.phantom.service.",
        "com.navi.phantom.metaloader.",
        "com.navi.phantom.shared.",
    )

    @JvmStatic
    fun apply(appClassLoader: ClassLoader) {
        hookClassLoading(appClassLoader)
        hookThrowableStackTrace()
        hookThreadStackTrace()
    }

    /**
     * Hook ClassLoader.loadClass() to block Xposed class loading from the app's classloader.
     * Only blocks when the calling classloader is the app's own (or a child of it),
     * not when the framework itself loads classes.
     */
    private fun hookClassLoading(appClassLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                ClassLoader::class.java, "loadClass",
                String::class.java, Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val thisLoader = param.thisObject as? ClassLoader ?: return
                        if (!isAppClassLoader(thisLoader, appClassLoader)) return

                        val className = param.args[0] as? String ?: return
                        if (isBlocked(className)) {
                            param.throwable = ClassNotFoundException(className)
                        }
                    }
                }
            )
            log.d { "Hooked ClassLoader.loadClass(String, boolean)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook ClassLoader.loadClass(String, boolean)" }
        }

        // Also hook the single-arg variant since some code calls it directly
        try {
            XposedHelpers.findAndHookMethod(
                ClassLoader::class.java, "loadClass",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val thisLoader = param.thisObject as? ClassLoader ?: return
                        if (!isAppClassLoader(thisLoader, appClassLoader)) return

                        val className = param.args[0] as? String ?: return
                        if (isBlocked(className)) {
                            param.throwable = ClassNotFoundException(className)
                        }
                    }
                }
            )
            log.d { "Hooked ClassLoader.loadClass(String)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook ClassLoader.loadClass(String)" }
        }
    }

    /**
     * Hook Throwable.getStackTrace() to scrub Xposed frames from exception stack traces.
     */
    private fun hookThrowableStackTrace() {
        try {
            XposedHelpers.findAndHookMethod(
                Throwable::class.java, "getStackTrace",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val trace = param.result as? Array<*> ?: return
                        @Suppress("UNCHECKED_CAST")
                        param.result = scrubStackTrace(trace as Array<StackTraceElement>)
                    }
                }
            )
            log.d { "Hooked Throwable.getStackTrace()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Throwable.getStackTrace()" }
        }
    }

    /**
     * Hook Thread.getStackTrace() and Thread.getAllStackTraces() to scrub Xposed frames.
     */
    private fun hookThreadStackTrace() {
        try {
            XposedHelpers.findAndHookMethod(
                Thread::class.java, "getStackTrace",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val trace = param.result as? Array<*> ?: return
                        @Suppress("UNCHECKED_CAST")
                        param.result = scrubStackTrace(trace as Array<StackTraceElement>)
                    }
                }
            )
            log.d { "Hooked Thread.getStackTrace()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Thread.getStackTrace()" }
        }

        try {
            XposedHelpers.findAndHookMethod(
                Thread::class.java, "getAllStackTraces",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        @Suppress("UNCHECKED_CAST")
                        val allTraces = param.result as? Map<Thread, Array<StackTraceElement>> ?: return
                        val scrubbed = allTraces.mapValues { (_, trace) -> scrubStackTrace(trace) }
                        param.result = scrubbed
                    }
                }
            )
            log.d { "Hooked Thread.getAllStackTraces()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Thread.getAllStackTraces()" }
        }
    }

    /**
     * Check if the given classloader is the app's classloader (same instance).
     * We only block class loading from the app's own classloader, not from
     * the Xposed framework's classloader which needs to load its own classes.
     */
    private fun isAppClassLoader(loader: ClassLoader, appClassLoader: ClassLoader): Boolean {
        return loader === appClassLoader
    }

    private fun isBlocked(className: String): Boolean {
        return BLOCKED_PREFIXES.any { className.startsWith(it) }
    }

    private fun scrubStackTrace(trace: Array<StackTraceElement>): Array<StackTraceElement> {
        return trace.filter { element ->
            !isBlocked(element.className)
        }.toTypedArray()
    }
}
