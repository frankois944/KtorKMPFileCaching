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

internal expect fun InternalFileCacheStorage(
    storedCacheDirectory: Path,
    directoryPath: Path,
    dispatcher: CoroutineDispatcher,
    fileSystem: FileSystem
) : CacheStorage

/**
 * Creates a multiplatform file-based cache storage.
 * @param directoryPath path to store cache data.
 * @param dispatcher dispatcher for file operations.
 */
public fun NewFileStorage(
    storedCacheDirectory: Path = "KTorFileCaching".toPath(),
    directoryPath: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    fileSystem: FileSystem = filesystem()
): CacheStorage = InternalFileCacheStorage(storedCacheDirectory, directoryPath, dispatcher, fileSystem)
