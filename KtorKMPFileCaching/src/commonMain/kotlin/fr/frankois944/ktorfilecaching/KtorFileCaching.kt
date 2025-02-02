@file:OptIn(ExperimentalSerializationApi::class)

package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CacheStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal expect fun filesystem(): FileSystem

@Suppress("ktlint:standard:function-naming")
internal expect fun InternalFileCacheStorage(
    storedCacheDirectory: Path,
    directoryPath: Path,
    dispatcher: CoroutineDispatcher,
    fileSystem: FileSystem,
): CacheStorage

@Suppress("ktlint:standard:function-naming")
/**
 * Creates a multiplatform file-based cache storage.
 *
 * On Browser and Wasm Kotlin apps, the `LocalStorage` of the browser
 * is used for storing cached content instead of okio,
 * a prefix is added `fr.frankois944.ktorfilecaching_key` on the stored keys.
 *
 * @param storedCacheDirectory directory to the stored cache (unused of browser).
 * @param directoryPath base path to store cache data (unused of browser), by default `SYSTEM_TEMPORARY_DIRECTORY` of okio.
 * @param dispatcher dispatcher for file operations.
 * @param fileSystem an okio filesystem
 *
 */
public fun KtorFileCaching(
    storedCacheDirectory: Path = "KTorFileCaching".toPath(),
    directoryPath: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    fileSystem: FileSystem = filesystem(),
): CacheStorage =
    CachingCacheStorage(
        InternalFileCacheStorage(storedCacheDirectory, directoryPath, dispatcher, fileSystem),
    )
