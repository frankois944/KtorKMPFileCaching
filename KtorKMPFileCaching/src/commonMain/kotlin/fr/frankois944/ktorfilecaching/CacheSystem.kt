@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import okio.FileSystem
import okio.Path

internal expect class CacheSystem(fileSystem: FileSystem, cacheDir: Path) {
    internal fun exist(key: Path, varyKeyHash: String?): Boolean

    internal fun write(key: Path, varyKeyHash: String, value: String)

    internal fun contentOf(key: Path): Set<Path>

    internal fun read(key: Path, varyKeyHash: String): String?

    internal fun purge(key: Path? = null)
}