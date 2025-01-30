@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import androidx.collection.ScatterSet
import androidx.collection.scatterSetOf
import okio.FileSystem
import okio.Path

internal actual fun filesystem(): FileSystem = FileSystem.SYSTEM

internal actual class CacheSystem actual constructor(
    private val fileSystem: FileSystem,
    private val cacheDir: Path,
) {
    init {
        if (!fileSystem.exists(cacheDir)) {
            fileSystem.createDirectories(cacheDir, true)
        }
    }

    internal actual fun exist(
        key: Path,
        varyKeyHash: String?,
    ): Boolean {
        varyKeyHash?.let {
            val filePath = key.resolve(varyKeyHash)
            return fileSystem.exists(filePath)
        } ?: run {
            return fileSystem.exists(key)
        }
    }

    internal actual fun write(
        key: Path,
        varyKeyHash: String,
        value: String,
    ) {
        if (!fileSystem.exists(key)) {
            fileSystem.createDirectories(key, true)
        }
        val filePath = key.resolve(varyKeyHash)
        fileSystem.write(filePath) {
            buffer.writeUtf8(value)
        }
    }

    internal actual fun contentOf(key: Path): ScatterSet<Path> =
        fileSystem.list(key).run {
            scatterSetOf(*this.toTypedArray())
        }

    internal actual fun read(
        key: Path,
        varyKeyHash: String,
    ): String? {
        val filePath = key.resolve(varyKeyHash)
        return if (fileSystem.exists(filePath)) {
            fileSystem.read(filePath) {
                readUtf8()
            }
        } else {
            null
        }
    }

    internal actual fun purge(key: Path?) {
        key?.let {
            if (fileSystem.exists(key)) {
                fileSystem.deleteRecursively(key)
            }
        } ?: run {
            fileSystem.list(cacheDir).forEach { fileSystem.deleteRecursively(it) }
        }
    }
}
