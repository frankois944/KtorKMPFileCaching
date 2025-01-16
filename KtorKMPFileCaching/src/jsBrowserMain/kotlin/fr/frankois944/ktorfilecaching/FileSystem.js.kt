@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

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

    internal actual fun contentOf(key: Path): Set<Path> =
        getIndex()
            .map { index ->
                index.split("_")[2].toPath()
            }.toSet()

    internal actual fun read(
        key: Path,
        varyKeyHash: String,
    ): String? =
        with(buildKeyPath(key, varyKeyHash)) {
            window.localStorage.getItem(this)
        }

    internal actual fun purge(key: Path?) {
        key?.let {
            getIndex()
                .filter { item ->
                    item.startsWith(buildKeyPath(key, null))
                }.forEach { index ->
                    window.localStorage.removeItem(index)
                    removeFromIndex(index)
                }
        } ?: run {
            getIndex().forEach {
                window.localStorage.removeItem(it)
            }
            purgeIndex()
        }
    }
}
