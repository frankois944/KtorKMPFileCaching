@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import kotlinx.browser.window
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

internal actual fun filesystem(): FileSystem = FakeFileSystem()

internal actual class CacheSystem actual constructor(
    fileSystem: FileSystem,
    cacheDir: Path,
) {
    private fun buildKeyPath(
        key: Path,
        varyKeyHash: String?,
    ): String =
        varyKeyHash?.let {
            "${prefix}_${key}_$varyKeyHash"
        } ?: run {
            "${prefix}_$key"
        }

    internal actual fun exist(
        key: Path,
        varyKeyHash: String?,
    ): Boolean =
        varyKeyHash?.let {
            getIndex().contains(buildKeyPath(key, varyKeyHash))
        } ?: run {
            getIndex().firstOrNull { item ->
                item.startsWith(buildKeyPath(key, null))
            } != null
        }

    internal actual fun write(
        key: Path,
        varyKeyHash: String,
        value: String,
    ) {
        with(buildKeyPath(key, varyKeyHash)) {
            window.localStorage.setItem(this, value)
            addToIndex(this)
        }
    }

    internal actual fun contentOf(key: Path): ScatterSet<Path> {
        val result = mutableScatterSetOf<Path>()
        getIndex().forEach {
            if (it.startsWith(buildKeyPath(key, null))) {
                result.add(it.split("_")[2].toPath())
            }
        }
        return result
    }

    internal actual fun read(
        key: Path,
        varyKeyHash: String,
    ): String? =
        with(buildKeyPath(key, varyKeyHash)) {
            window.localStorage.getItem(this)
        }

    internal actual fun purge(key: Path?) {
        key?.let {
            getIndex().forEach { index ->
                if (index.startsWith(buildKeyPath(key, null))) {
                    window.localStorage.removeItem(index)
                    removeFromIndex(index)
                }
            }
        } ?: run {
            getIndex().forEach {
                window.localStorage.removeItem(it)
            }
            purgeIndex()
        }
    }
}
