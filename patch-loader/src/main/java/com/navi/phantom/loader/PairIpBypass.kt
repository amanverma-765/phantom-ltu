package com.navi.phantom.loader

import android.app.Activity
import android.content.Context
import android.os.Bundle
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import java.security.PublicKey

/**
 * Bypasses Google PairIP (pairipcore) protection.
 * Auto-detects PairIP presence and applies all necessary hooks.
 * Ported from pairipfix project.
 */
object PairIpBypass {

    private val log = Logger.withTag("Phantom-PairIP")

    // PairIP class names
    private const val SIGNATURE_CHECK = "com.pairip.SignatureCheck"
    private const val LICENSE_CLIENT = "com.pairip.licensecheck.LicenseClient"
    private const val LICENSE_CONTENT_PROVIDER = "com.pairip.licensecheck.LicenseContentProvider"
    private const val LICENSE_ACTIVITY = "com.pairip.licensecheck.LicenseActivity"
    private const val LICENSE_RESPONSE_HELPER = "com.pairip.licensecheck.LicenseResponseHelper"
    private const val LICENSE_CHECK_STATE = "com.pairip.licensecheck.LicenseClient\$LicenseCheckState"
    private const val LICENSE_CLIENT_V3 = "com.pairip.licensecheck3.LicenseClientV3"
    private const val RESPONSE_VALIDATOR = "com.pairip.licensecheck.ResponseValidator"
    private const val RESPONSE_VALIDATOR_V3 = "com.pairip.licensecheck3.ResponseValidator"
    private const val STARTUP_LAUNCHER = "com.pairip.StartupLauncher"

    private val ALL_CLASSES = listOf(
        SIGNATURE_CHECK,
        LICENSE_CLIENT,
        LICENSE_CONTENT_PROVIDER,
        STARTUP_LAUNCHER,
        LICENSE_CLIENT_V3,
        RESPONSE_VALIDATOR,
        RESPONSE_VALIDATOR_V3
    )

    @JvmStatic
    fun apply(classLoader: ClassLoader) {
        if (!hasPairIpProtection(classLoader)) {
            log.d { "No PairIP protection detected, skipping" }
            return
        }
        log.i { "PairIP protection detected, applying bypasses" }

        applySignatureBypass(classLoader)
        applyLicenseContentProviderBypass(classLoader)
        applyLicenseClientBypass(classLoader)
        applyLicenseActivityBypass(classLoader)
        applyLicenseResponseBypass(classLoader)
        applyLicenseClientV3Bypass(classLoader)
        applyVmRunnerBypass(classLoader)

        log.i { "PairIP bypasses applied" }
    }

    private fun hasPairIpProtection(classLoader: ClassLoader): Boolean {
        return ALL_CLASSES.any { classExists(classLoader, it) }
    }

    private fun classExists(classLoader: ClassLoader, name: String): Boolean {
        return XposedHelpers.findClassIfExists(name, classLoader) != null
    }

    // --- Signature Bypass ---

    private fun applySignatureBypass(cl: ClassLoader) {
        try {
            hookDoNothing(cl, SIGNATURE_CHECK, "verifyIntegrity", Context::class.java)
            log.d { "Hooked SignatureCheck.verifyIntegrity" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook SignatureCheck.verifyIntegrity" }
        }

        try {
            hookReturnConstant(cl, SIGNATURE_CHECK, "verifySignatureMatches", true, String::class.java)
            log.d { "Hooked SignatureCheck.verifySignatureMatches" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook SignatureCheck.verifySignatureMatches" }
        }
    }

    // --- License Content Provider Bypass ---

    private fun applyLicenseContentProviderBypass(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                LICENSE_CONTENT_PROVIDER, cl, "onCreate",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam<*>): Any {
                        setLicenseStateFullCheckOK(cl)
                        return true
                    }
                }
            )
            log.d { "Hooked LicenseContentProvider.onCreate" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseContentProvider.onCreate" }
        }
    }

    private fun setLicenseStateFullCheckOK(cl: ClassLoader) {
        try {
            val stateClass = XposedHelpers.findClassIfExists(LICENSE_CHECK_STATE, cl) ?: return
            val fullCheckOk = XposedHelpers.getStaticObjectField(stateClass, "FULL_CHECK_OK")
            val clientClass = XposedHelpers.findClassIfExists(LICENSE_CLIENT, cl) ?: return
            XposedHelpers.setStaticObjectField(clientClass, "licenseCheckState", fullCheckOk)
            log.d { "Set LicenseClient.licenseCheckState = FULL_CHECK_OK" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to set license state" }
        }
    }

    // --- License Client Bypass ---

    private fun applyLicenseClientBypass(cl: ClassLoader) {
        try {
            hookDoNothing(cl, LICENSE_CLIENT, "initializeLicenseCheck")
            log.d { "Hooked LicenseClient.initializeLicenseCheck" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseClient.initializeLicenseCheck" }
        }

        try {
            hookReturnConstant(cl, LICENSE_CLIENT, "performLocalInstallerCheck", true)
            log.d { "Hooked LicenseClient.performLocalInstallerCheck" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseClient.performLocalInstallerCheck" }
        }
    }

    // --- License Activity Bypass ---

    private fun applyLicenseActivityBypass(cl: ClassLoader) {
        try {
            hookDoNothing(cl, LICENSE_ACTIVITY, "closeApp")
            log.d { "Hooked LicenseActivity.closeApp" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseActivity.closeApp" }
        }

        try {
            XposedHelpers.findAndHookMethod(
                LICENSE_ACTIVITY, cl, "onStart",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        (param.thisObject as? Activity)?.finish()
                    }
                }
            )
            log.d { "Hooked LicenseActivity.onStart" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseActivity.onStart" }
        }

        try {
            hookDoNothing(cl, LICENSE_ACTIVITY, "showErrorDialog")
            log.d { "Hooked LicenseActivity.showErrorDialog" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseActivity.showErrorDialog" }
        }

        try {
            hookDoNothing(cl, LICENSE_ACTIVITY, "showPaywallAndCloseApp")
            log.d { "Hooked LicenseActivity.showPaywallAndCloseApp" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseActivity.showPaywallAndCloseApp" }
        }

        try {
            hookDoNothing(cl, LICENSE_ACTIVITY, "logAndShowErrorDialog", String::class.java)
            log.d { "Hooked LicenseActivity.logAndShowErrorDialog(String)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseActivity.logAndShowErrorDialog(String)" }
        }

        try {
            hookDoNothing(cl, LICENSE_ACTIVITY, "logAndShowErrorDialog", String::class.java, Exception::class.java)
            log.d { "Hooked LicenseActivity.logAndShowErrorDialog(String, Exception)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseActivity.logAndShowErrorDialog(String, Exception)" }
        }
    }

    // --- License Response Bypass ---

    private fun applyLicenseResponseBypass(cl: ClassLoader) {
        // LicenseResponseHelper
        try {
            hookDoNothing(cl, LICENSE_RESPONSE_HELPER, "validateResponse", Bundle::class.java, String::class.java)
            log.d { "Hooked LicenseResponseHelper.validateResponse" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseResponseHelper.validateResponse" }
        }

        try {
            hookDoNothing(
                cl, LICENSE_RESPONSE_HELPER, "verifySignature",
                String::class.java, String::class.java, String::class.java, PublicKey::class.java
            )
            log.d { "Hooked LicenseResponseHelper.verifySignature" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseResponseHelper.verifySignature" }
        }

        try {
            hookReturnConstant(cl, LICENSE_RESPONSE_HELPER, "getPublicKey", null)
            log.d { "Hooked LicenseResponseHelper.getPublicKey" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseResponseHelper.getPublicKey" }
        }

        try {
            hookReturnConstant(cl, LICENSE_RESPONSE_HELPER, "getRepeatedCheckMetadata", null, Bundle::class.java)
            log.d { "Hooked LicenseResponseHelper.getRepeatedCheckMetadata" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseResponseHelper.getRepeatedCheckMetadata" }
        }

        // ResponseValidator (newer version, if present)
        if (classExists(cl, RESPONSE_VALIDATOR)) {
            try {
                hookDoNothing(cl, RESPONSE_VALIDATOR, "validateResponse", Bundle::class.java, String::class.java)
                log.d { "Hooked ResponseValidator.validateResponse" }
            } catch (t: Throwable) {
                log.w(t) { "Failed to hook ResponseValidator.validateResponse" }
            }

            try {
                hookDoNothing(
                    cl, RESPONSE_VALIDATOR, "verifySignature",
                    String::class.java, String::class.java, String::class.java, PublicKey::class.java
                )
                log.d { "Hooked ResponseValidator.verifySignature" }
            } catch (t: Throwable) {
                log.w(t) { "Failed to hook ResponseValidator.verifySignature" }
            }
        }

        // ResponseValidator V3 (if present)
        if (classExists(cl, RESPONSE_VALIDATOR_V3)) {
            try {
                hookDoNothing(cl, RESPONSE_VALIDATOR_V3, "validateResponse", Bundle::class.java, String::class.java)
                log.d { "Hooked ResponseValidatorV3.validateResponse" }
            } catch (t: Throwable) {
                log.w(t) { "Failed to hook ResponseValidatorV3.validateResponse" }
            }
        }
    }

    // --- License Client V3 Bypass ---

    private fun applyLicenseClientV3Bypass(cl: ClassLoader) {
        if (!classExists(cl, LICENSE_CLIENT_V3)) return

        try {
            hookDoNothing(cl, LICENSE_CLIENT_V3, "processResponse", Int::class.javaPrimitiveType!!, Bundle::class.java)
            log.d { "Hooked LicenseClientV3.processResponse" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LicenseClientV3.processResponse" }
        }
    }

    // --- VM Runner Bypass ---

    private fun applyVmRunnerBypass(cl: ClassLoader) {
        try {
            hookDoNothing(cl, STARTUP_LAUNCHER, "launch")
            log.d { "Hooked StartupLauncher.launch" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook StartupLauncher.launch" }
        }

        try {
            val launcherClass = XposedHelpers.findClassIfExists(STARTUP_LAUNCHER, cl)
            if (launcherClass != null) {
                XposedHelpers.setStaticBooleanField(launcherClass, "launchCalled", true)
                log.d { "Set StartupLauncher.launchCalled = true" }
            }
        } catch (t: Throwable) {
            log.w(t) { "Failed to set StartupLauncher.launchCalled" }
        }
    }

    // --- Hook Helpers ---

    private fun hookDoNothing(cl: ClassLoader, className: String, methodName: String, vararg paramTypes: Any) {
        val args = arrayOf(*paramTypes, XC_MethodReplacement.DO_NOTHING)
        XposedHelpers.findAndHookMethod(className, cl, methodName, *args)
    }

    private fun hookReturnConstant(cl: ClassLoader, className: String, methodName: String, value: Any?, vararg paramTypes: Any) {
        val args = arrayOf(*paramTypes, XC_MethodReplacement.returnConstant(value))
        XposedHelpers.findAndHookMethod(className, cl, methodName, *args)
    }
}
