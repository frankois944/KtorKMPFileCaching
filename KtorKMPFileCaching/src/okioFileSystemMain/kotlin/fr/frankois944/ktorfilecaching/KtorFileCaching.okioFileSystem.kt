@file:OptIn(ExperimentalSerializationApi::class)

package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import okio.use

@Suppress("ktlint:standard:function-naming")
internal actual fun InternalFileCacheStorage(
    storedCacheDirectory: Path,
    directoryPath: Path,
    dispatcher: CoroutineDispatcher,
    fileSystem: FileSystem,
): CacheStorage = FileCacheStorage(storedCacheDirectory, directoryPath, dispatcher, fileSystem)

internal class FileCacheStorage(
    storedCacheDirectory: Path,
    directoryPath: Path,
    val dispatcher: CoroutineDispatcher,
    val fileSystem: FileSystem = filesystem(),
) : CacheStorage {
    private val lock = Mutex() // Coroutine-friendly lock for thread safety
    private val baseDir = "$directoryPath${Path.DIRECTORY_SEPARATOR}$storedCacheDirectory"

    init {
        fileSystem.createDirectories(baseDir.toPath())
    }

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ): Unit =
        withContext(dispatcher) {
            val urlHex = key(url)
            val caches = readCache(urlHex).filterNot { it.varyKeys == data.varyKeys } + data
            writeCache(urlHex, caches)
        }

    override suspend fun findAll(url: Url): Set<CachedResponseData> = readCache(key(url)).toSet()

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? {
        val data = readCache(key(url))
        return data.find { varyKeys.all { (key, value) -> it.varyKeys[key] == value } }
    }

    private fun key(url: Url): String {
        val hashingSink = HashingSink.sha256(blackholeSink())
        hashingSink.buffer().use {
            it.write(url.toString().encodeUtf8())
        }
        return hashingSink.hash.hex()
    }

    private suspend fun writeCache(
        urlHex: String,
        caches: List<CachedResponseData>,
    ) = withContext(dispatcher) {
        lock.withLock {
            val filePath = "$baseDir${Path.DIRECTORY_SEPARATOR}$urlHex".toPath()
            val serializedData = Cbor.encodeToByteArray(caches.map { SerializableCachedResponseData(it) })
            fileSystem.write(filePath) { write(serializedData) }
        }
    }

    private suspend fun readCache(urlHex: String): List<CachedResponseData> =
        withContext(dispatcher) {
            lock.withLock {
                val filePath = "$baseDir${Path.DIRECTORY_SEPARATOR}$urlHex".toPath()
                if (!fileSystem.exists(filePath)) return@withContext emptyList()
                return@withContext try {
                    val bytes = fileSystem.read(filePath) { readByteArray() }
                    Cbor.decodeFromByteArray<List<SerializableCachedResponseData>>(bytes).map { it.cachedResponseData }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
}
