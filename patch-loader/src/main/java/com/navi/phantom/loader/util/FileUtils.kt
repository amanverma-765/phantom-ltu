package com.navi.phantom.loader.util

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object FileUtils {

    @JvmStatic
    @Throws(IOException::class)
    fun deleteFolderIfExists(target: Path) {
        if (Files.notExists(target)) return
        Files.walkFileTree(target, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult {
                if (e == null) {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                } else {
                    throw e
                }
            }
        })
    }
}
