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

    @Test
    fun testManifestInfoSplitFlag() {
        val nonSplit = com.navi.phantom.patcher.util.ManifestInfo("com.example", null, 28, false)
        assertFalse(nonSplit.isSplit)

        val split = com.navi.phantom.patcher.util.ManifestInfo("com.example", null, 28, true)
        assertTrue(split.isSplit)
    }

    @Test
    fun testExtractOriginIfPatchedPhantom() {
        val tempZip = java.io.File.createTempFile("test-patched-", ".apk")
        try {
            java.util.zip.ZipOutputStream(tempZip.outputStream()).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry(Constants.ORIGINAL_APK_ASSET_PATH))
                zos.write("genuine_inner_apk_bytes".toByteArray())
                zos.closeEntry()

                zos.putNextEntry(java.util.zip.ZipEntry(Constants.CONFIG_ASSET_PATH))
                val config = PatchConfig(
                    debuggable = false,
                    overrideVersionCode = false,
                    sigBypassLevel = 1,
                    originalSignature = "test_genuine_sig",
                    appComponentFactory = "com.test.GenuineFactory"
                )
                zos.write(Json.encodeToString(config).toByteArray())
                zos.closeEntry()
            }

            val patcher = PhantomPatcher(arrayOf("-f", "placeholder"))
            val originInfo = patcher.extractOriginIfPatched(tempZip)
            assertNotNull(originInfo)
            assertEquals("genuine_inner_apk_bytes", originInfo!!.tempFile.readText())
            assertEquals("test_genuine_sig", originInfo.previousConfig?.originalSignature)
            assertEquals("com.test.GenuineFactory", originInfo.previousConfig?.appComponentFactory)
            originInfo.tempFile.delete()
        } finally {
            tempZip.delete()
        }
    }

    @Test
    fun testExtractOriginIfPatchedLegacyLspatch() {
        val tempZip = java.io.File.createTempFile("test-lsp-patched-", ".apk")
        try {
            java.util.zip.ZipOutputStream(tempZip.outputStream()).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry(Constants.LSP_ORIGINAL_APK_ASSET_PATH))
                zos.write("legacy_inner_apk_bytes".toByteArray())
                zos.closeEntry()
            }

            val patcher = PhantomPatcher(arrayOf("-f", "placeholder"))
            val originInfo = patcher.extractOriginIfPatched(tempZip)
            assertNotNull(originInfo)
            assertEquals("legacy_inner_apk_bytes", originInfo!!.tempFile.readText())
            assertNull(originInfo.previousConfig)
            originInfo.tempFile.delete()
        } finally {
            tempZip.delete()
        }
    }

    @Test
    fun testExtractOriginIfUnpatched() {
        val tempZip = java.io.File.createTempFile("test-unpatched-", ".apk")
        try {
            java.util.zip.ZipOutputStream(tempZip.outputStream()).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry("AndroidManifest.xml"))
                zos.write("manifest_content".toByteArray())
                zos.closeEntry()
            }

            val patcher = PhantomPatcher(arrayOf("-f", "placeholder"))
            val originInfo = patcher.extractOriginIfPatched(tempZip)
            assertNull(originInfo)
        } finally {
            tempZip.delete()
        }
    }
}
