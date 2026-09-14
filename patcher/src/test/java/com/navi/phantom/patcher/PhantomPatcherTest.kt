package com.navi.phantom.patcher

import com.navi.phantom.shared.Constants
import com.navi.phantom.shared.PatchConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhantomPatcherTest {

    @Test
    fun testNormalizePermission() {
        assertEquals("android.permission.INTERNET", PhantomPatcher.normalizePermission("INTERNET"))
        assertEquals("android.permission.INTERNET", PhantomPatcher.normalizePermission("internet"))
        assertEquals("android.permission.ACCESS_FINE_LOCATION", PhantomPatcher.normalizePermission("ACCESS_FINE_LOCATION"))
        assertEquals("android.permission.ACCESS_FINE_LOCATION", PhantomPatcher.normalizePermission("android.permission.ACCESS_FINE_LOCATION"))
        assertEquals("com.example.CUSTOM_PERMISSION", PhantomPatcher.normalizePermission("com.example.CUSTOM_PERMISSION"))
        assertEquals("", PhantomPatcher.normalizePermission(""))
        assertEquals("", PhantomPatcher.normalizePermission("   "))
        assertEquals("", PhantomPatcher.normalizePermission(null))
    }

    @Test
    fun testPatchConfigSerializationWithLevel3AndNewFields() {
        val config = PatchConfig(
            debuggable = true,
            overrideVersionCode = true,
            sigBypassLevel = Constants.SIGBYPASS_LV_PM_OPENAT_SVC,
            originalSignature = "MIIB...test",
            appComponentFactory = "com.test.Factory",
            managerApkPath = "/path/to/manager.apk",
            addedPermissions = listOf("android.permission.INTERNET", "android.permission.ACCESS_FINE_LOCATION"),
            versionCodeOverride = 100,
            managerPackageName = "com.custom.manager"
        )

        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<PatchConfig>(json)

        assertTrue(decoded.debuggable)
        assertTrue(decoded.overrideVersionCode)
        assertEquals(3, decoded.sigBypassLevel)
        assertEquals("MIIB...test", decoded.originalSignature)
        assertEquals("com.test.Factory", decoded.appComponentFactory)
        assertEquals("/path/to/manager.apk", decoded.managerApkPath)
        assertEquals(listOf("android.permission.INTERNET", "android.permission.ACCESS_FINE_LOCATION"), decoded.addedPermissions)
        assertEquals(100, decoded.versionCodeOverride)
        assertEquals("com.custom.manager", decoded.managerPackageName)
    }

    @Test
    fun testPatchConfigBackwardCompatibility() {
        // Test json missing the new fields (like an apk patched before this update)
        val json = """
            {
                "debuggable": false,
                "overrideVersionCode": false,
                "sigBypassLevel": 2,
                "originalSignature": null,
                "appComponentFactory": null,
                "managerApkPath": null
            }
        """.trimIndent()

        val decoded = Json.decodeFromString<PatchConfig>(json)
        assertFalse(decoded.debuggable)
        assertFalse(decoded.overrideVersionCode)
        assertEquals(2, decoded.sigBypassLevel)
        assertNull(decoded.originalSignature)
        assertNull(decoded.appComponentFactory)
        assertNull(decoded.managerApkPath)
        assertNull(decoded.addedPermissions)
        assertNull(decoded.versionCodeOverride)
        assertNull(decoded.managerPackageName)
    }
}
