package com.navi.phantom.patcher.util

import co.touchlab.kermit.Logger
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.cert.Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

object ApkSignatureHelper {

    private val log = Logger.withTag("ApkSignatureHelper")

    private val APK_V2_MAGIC = byteArrayOf(
        'A'.code.toByte(), 'P'.code.toByte(), 'K'.code.toByte(), ' '.code.toByte(),
        'S'.code.toByte(), 'i'.code.toByte(), 'g'.code.toByte(), ' '.code.toByte(),
        'B'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte(), 'c'.code.toByte(),
        'k'.code.toByte(), ' '.code.toByte(), '4'.code.toByte(), '2'.code.toByte()
    )

    private fun toChars(signature: ByteArray): CharArray {
        val n = signature.size
        val text = CharArray(n * 2)
        for (j in 0 until n) {
            val v = signature[j].toInt()
            var d = (v shr 4) and 0xf
            text[j * 2] = if (d >= 10) ('a'.code + d - 10).toChar() else ('0'.code + d).toChar()
            d = v and 0xf
            text[j * 2 + 1] = if (d >= 10) ('a'.code + d - 10).toChar() else ('0'.code + d).toChar()
        }
        return text
    }

    private fun loadCertificates(jarFile: JarFile, je: JarEntry, readBuffer: ByteArray): Array<Certificate>? {
        return try {
            jarFile.getInputStream(je).use { inputStream ->
                while (inputStream.read(readBuffer, 0, readBuffer.size) != -1) {
                    // Read entire entry to verify and populate certificates
                }
            }
            je.certificates
        } catch (e: Exception) {
            log.w(e) { "Failed to load certificates from jar entry: ${je.name}" }
            null
        }
    }

    @JvmStatic
    fun getApkSignInfo(apkFilePath: String): String? {
        return try {
            getApkSignV2(apkFilePath)
        } catch (e: Exception) {
            getApkSignV1(apkFilePath)
        }
    }

    @JvmStatic
    fun getApkSignV1(apkFilePath: String): String? {
        val readBuffer = ByteArray(8192)
        var certs: Array<Certificate>? = null

        return try {
            JarFile(apkFilePath).use { jarFile ->
                val entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    val je = entries.nextElement() as JarEntry
                    if (je.isDirectory || je.name.startsWith("META-INF/")) {
                        continue
                    }

                    val localCerts = loadCertificates(jarFile, je, readBuffer)
                    if (certs == null) {
                        certs = localCerts
                    } else {
                        for (i in certs.indices) {
                            var found = false
                            for (j in localCerts!!.indices) {
                                if (certs[i] == localCerts[j]) {
                                    found = true
                                    break
                                }
                            }
                            if (!found || certs.size != localCerts.size) {
                                return null
                            }
                        }
                    }
                }
                certs?.firstOrNull()?.let { String(toChars(it.encoded)) }
            }
        } catch (e: Throwable) {
            log.w(e) { "Failed to get V1 signature" }
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun getApkSignV2(apkFilePath: String): String {
        RandomAccessFile(apkFilePath, "r").use { apk ->
            val buffer = ByteBuffer.allocate(0x10)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            apk.seek(apk.length() - 0x6)
            apk.readFully(buffer.array(), 0x0, 0x6)
            val offset = buffer.int
            if (buffer.short.toInt() != 0) {
                throw IOException("no zip")
            }

            apk.seek(offset.toLong() - 0x10)
            apk.readFully(buffer.array(), 0x0, 0x10)

            if (!buffer.array().contentEquals(APK_V2_MAGIC)) {
                throw IOException("no apk v2")
            }

            apk.seek(offset.toLong() - 0x18)
            apk.readFully(buffer.array(), 0x0, 0x8)
            buffer.rewind()
            var size = buffer.long.toInt()

            val block = ByteBuffer.allocate(size + 0x8)
            block.order(ByteOrder.LITTLE_ENDIAN)
            apk.seek(offset.toLong() - block.capacity())
            apk.readFully(block.array(), 0x0, block.capacity())

            if (size.toLong() != block.long) {
                throw IOException("no apk v2")
            }

            while (block.remaining() > 24) {
                size = block.long.toInt()
                if (block.int == 0x7109871a) {
                    // signer-sequence length, signer length, signed data length
                    block.position(block.position() + 12)
                    size = block.int // digests-sequence length

                    // digests, certificates length
                    block.position(block.position() + size + 0x4)

                    size = block.int // certificate length
                    break
                } else {
                    block.position(block.position() + size - 0x4)
                }
            }

            val certificate = ByteArray(size)
            block.get(certificate)

            return String(toChars(certificate))
        }
    }
}
