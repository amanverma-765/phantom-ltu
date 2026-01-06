@file:Suppress("unused")

package de.robv.android.xposed

import android.util.Log
import java.lang.reflect.Member
import java.util.concurrent.ConcurrentHashMap

/**
 * Core Xposed bridge for hooking methods.
 * This is a simplified implementation for our built-in patches.
 */
object XposedBridge {

    private const val TAG = "XposedBridge"

    /**
     * Map of hooked methods to their callbacks.
     */
    private val hookedMethods = ConcurrentHashMap<Member, MutableList<XC_MethodHook>>()

    /**
     * Log a message with Xposed tag.
     */
    @JvmStatic
    fun log(text: String) {
        Log.i(TAG, text)
    }

    /**
     * Log a throwable with Xposed tag.
     */
    @JvmStatic
    fun log(t: Throwable) {
        Log.e(TAG, "Error", t)
    }

    /**
     * Hook a method with the given callback.
     *
     * Note: This simplified implementation stores callbacks but actual hooking
     * is handled by the LSPatch runtime (LSPLoader) which uses this bridge.
     */
    @JvmStatic
    fun hookMethod(hookMethod: Member, callback: XC_MethodHook): XC_MethodHook.Unhook {
        val callbacks = hookedMethods.getOrPut(hookMethod) { mutableListOf() }
        synchronized(callbacks) {
            callbacks.add(callback)
        }
        Log.d(TAG, "Hooked method: ${hookMethod.declaringClass.name}.${hookMethod.name}")
        return XC_MethodHook.Unhook(hookMethod, callback)
    }

    /**
     * Unhook a method.
     */
    @JvmStatic
    fun unhookMethod(hookMethod: Member, callback: XC_MethodHook) {
        val callbacks = hookedMethods[hookMethod] ?: return
        synchronized(callbacks) {
            callbacks.remove(callback)
            if (callbacks.isEmpty()) {
                hookedMethods.remove(hookMethod)
            }
        }
    }

    /**
     * Hook all methods with the given name in a class.
     */
    @JvmStatic
    fun hookAllMethods(hookClass: Class<*>, methodName: String, callback: XC_MethodHook): Set<XC_MethodHook.Unhook> {
        val unhooks = mutableSetOf<XC_MethodHook.Unhook>()
        for (method in hookClass.declaredMethods) {
            if (method.name == methodName) {
                unhooks.add(hookMethod(method, callback))
            }
        }
        return unhooks
    }

    /**
     * Hook all constructors of a class.
     */
    @JvmStatic
    fun hookAllConstructors(hookClass: Class<*>, callback: XC_MethodHook): Set<XC_MethodHook.Unhook> {
        val unhooks = mutableSetOf<XC_MethodHook.Unhook>()
        for (constructor in hookClass.declaredConstructors) {
            unhooks.add(hookMethod(constructor, callback))
        }
        return unhooks
    }

    /**
     * Invoke the original method (before hooks modified it).
     * This delegates to the LSPatch runtime.
     */
    @JvmStatic
    fun invokeOriginalMethod(method: Member, thisObject: Any?, args: Array<Any?>): Any? {
        // This is called by the LSPatch runtime, not by us directly
        throw UnsupportedOperationException("invokeOriginalMethod must be handled by LSPatch runtime")
    }
}
