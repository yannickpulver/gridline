package com.yannickpulver.gridline.data.grid

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** Images added offline wait here until they're uploaded. */
class LocalImageFiles(private val dir: Path, private val fs: FileSystem = FileSystem.SYSTEM) {

    fun save(name: String, bytes: ByteArray): String {
        fs.createDirectories(dir)
        val path = dir / name
        fs.write(path) { write(bytes) }
        return path.toString()
    }

    /** Null when the file is gone, e.g. the system cleaned it up. */
    fun read(path: String): ByteArray? =
        path.toPath().takeIf { fs.exists(it) }?.let { fs.read(it) { readByteArray() } }

    fun delete(path: String) = fs.delete(path.toPath(), mustExist = false)

    fun deleteAll() = fs.deleteRecursively(dir, mustExist = false)
}
