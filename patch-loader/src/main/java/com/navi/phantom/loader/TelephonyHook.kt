package com.navi.phantom.loader

import android.telephony.TelephonyManager
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Hooks TelephonyManager to prevent cell tower-based location triangulation.
 * Returns null/empty for cell location queries so apps can't determine
 * approximate position from cell tower IDs.
 */
object TelephonyHook {

    private val log = Logger.withTag("Phantom-Telephony")

    @JvmStatic
    fun apply() {
        hookGetCellLocation()
        hookGetAllCellInfo()
        hookGetNeighboringCellInfo()
        hookPhoneStateListener()
    }

    @Suppress("DEPRECATION")
    private fun hookGetCellLocation() {
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java, "getCellLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        param.result = null
                    }
                }
            )
            log.d { "Hooked getCellLocation" }
        } catch (t: Throwable) {
            log.d { "getCellLocation not available" }
        }
    }

    private fun hookGetAllCellInfo() {
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java, "getAllCellInfo",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        param.result = emptyList<Any>()
                    }
                }
            )
            log.d { "Hooked getAllCellInfo" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getAllCellInfo" }
        }
    }

    @Suppress("DEPRECATION")
    private fun hookGetNeighboringCellInfo() {
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java, "getNeighboringCellInfo",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        param.result = emptyList<Any>()
                    }
                }
            )
            log.d { "Hooked getNeighboringCellInfo" }
        } catch (t: Throwable) {
            log.d { "getNeighboringCellInfo not available" }
        }
    }

    /**
     * Hook TelephonyManager.listen() to intercept LISTEN_CELL_LOCATION events.
     * Prevents apps from receiving real cell tower change notifications.
     */
    @Suppress("DEPRECATION")
    private fun hookPhoneStateListener() {
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java, "listen",
                android.telephony.PhoneStateListener::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        val events = param.args[1] as Int
                        // Strip LISTEN_CELL_LOCATION and LISTEN_CELL_INFO flags
                        @Suppress("DEPRECATION")
                        val filtered = events and
                            android.telephony.PhoneStateListener.LISTEN_CELL_LOCATION.inv() and
                            android.telephony.PhoneStateListener.LISTEN_CELL_INFO.inv()
                        param.args[1] = filtered
                    }
                }
            )
            log.d { "Hooked TelephonyManager.listen" }
        } catch (t: Throwable) {
            log.d { "TelephonyManager.listen hook not available" }
        }
    }
}
