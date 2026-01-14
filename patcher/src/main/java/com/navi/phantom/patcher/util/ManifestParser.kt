package com.navi.phantom.patcher.util

import co.touchlab.kermit.Logger
import com.wind.meditor.utils.Utils
import pxb.android.axml.AxmlParser
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

data class ManifestInfo(
    val packageName: String?,
    val appComponentFactory: String?,
    val minSdkVersion: Int
)

object ManifestParser {

    private val log = Logger.withTag("ManifestParser")

    @JvmStatic
    @Throws(IOException::class)
    fun parseManifestFile(inputStream: InputStream): ManifestInfo? {
        val parser = AxmlParser(Utils.getBytesFromInputStream(inputStream))
        var packageName: String? = null
        var appComponentFactory: String? = null
        var minSdkVersion = 0

        try {
            while (true) {
                when (val type = parser.next()) {
                    AxmlParser.END_FILE -> break
                    AxmlParser.START_TAG -> {
                        val attrCount = parser.attributeCount
                        for (i in 0 until attrCount) {
                            val attrName = parser.getAttrName(i)
                            val attrNameRes = parser.getAttrResId(i)
                            val name = parser.name

                            if (name == "manifest" && attrName == "package") {
                                packageName = parser.getAttrValue(i).toString()
                            }

                            if (name == "uses-sdk" && attrName == "minSdkVersion") {
                                minSdkVersion = parser.getAttrValue(i).toString().toInt()
                            }

                            if (attrName == "appComponentFactory" || attrNameRes == 0x0101057a) {
                                appComponentFactory = parser.getAttrValue(i).toString()
                            }

                            if (!packageName.isNullOrEmpty() &&
                                !appComponentFactory.isNullOrEmpty() &&
                                minSdkVersion > 0
                            ) {
                                return ManifestInfo(packageName, appComponentFactory, minSdkVersion)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to parse manifest" }
            return null
        }

        return ManifestInfo(packageName, appComponentFactory, minSdkVersion)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun parseManifestFile(filePath: String): ManifestInfo? {
        return FileInputStream(File(filePath)).use { parseManifestFile(it) }
    }
}
