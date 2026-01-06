@file:Suppress("unused")

package de.robv.android.xposed

import java.lang.reflect.Member

/**
 * Callback class for method hooks.
 */
abstract class XC_MethodHook {

    /**
     * Priority for this hook (higher = called earlier).
     */
    open val priority: Int = PRIORITY_DEFAULT

    /**
     * Called before the method is invoked.
     * Set param.result to return early without calling the original method.
     * Set param.throwable to throw an exception.
     */
    open fun beforeHookedMethod(param: MethodHookParam) {}

    /**
     * Called after the method is invoked.
     * Can modify param.result or param.throwable.
     */
    open fun afterHookedMethod(param: MethodHookParam) {}

    /**
     * Represents an unhook operation.
     */
    class Unhook(
        private val hookedMethod: Member,
        private val callback: XC_MethodHook
    ) {
        fun unhook() {
            XposedBridge.unhookMethod(hookedMethod, callback)
        }
    }

    /**
     * Parameters passed to hook callbacks.
     */
    class MethodHookParam {
        /**
         * The hooked method.
         */
        var method: Member? = null

        /**
         * The `this` object for instance methods, null for static methods.
         */
        var thisObject: Any? = null

        /**
         * Arguments passed to the method.
         */
        var args: Array<Any?> = emptyArray()

        /**
         * The return value of the method.
         * Set this in beforeHookedMethod to skip the original method.
         * Set this in afterHookedMethod to modify the return value.
         */
        @JvmField
        var result: Any? = null

        /**
         * Any exception thrown by the method or hooks.
         */
        @JvmField
        var throwable: Throwable? = null

        /**
         * Whether the result has been set (to skip original method).
         */
        internal var returnEarly = false

        /**
         * Set the result and mark to return early.
         */
        fun setResultValue(result: Any?) {
            this.result = result
            this.returnEarly = true
            this.throwable = null
        }

        /**
         * Set a throwable to throw instead of returning.
         */
        fun setThrowableValue(t: Throwable) {
            this.throwable = t
            this.returnEarly = true
            this.result = null
        }

        /**
         * Get an argument by index with type casting.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> getArg(index: Int): T = args[index] as T

        /**
         * Set an argument by index.
         */
        fun setArg(index: Int, value: Any?) {
            args[index] = value
        }
    }

    companion object {
        const val PRIORITY_DEFAULT = 50
        const val PRIORITY_LOWEST = -10000
        const val PRIORITY_HIGHEST = 10000
    }
}

/**
 * Convenience class for hooks that only modify the return value.
 */
abstract class XC_MethodReplacement : XC_MethodHook() {

    final override fun beforeHookedMethod(param: MethodHookParam) {
        try {
            val result = replaceHookedMethod(param)
            param.setResultValue(result)
        } catch (t: Throwable) {
            param.setThrowableValue(t)
        }
    }

    /**
     * Replace the hooked method with custom implementation.
     * Return value will be used as the method's return value.
     */
    abstract fun replaceHookedMethod(param: MethodHookParam): Any?

    companion object {
        /**
         * Create a replacement that returns null/void.
         */
        @JvmField
        val DO_NOTHING: XC_MethodReplacement = object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam): Any? = null
        }

        /**
         * Create a replacement that returns a constant value.
         */
        @JvmStatic
        fun returnConstant(value: Any?): XC_MethodReplacement {
            return object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? = value
            }
        }
    }
}
