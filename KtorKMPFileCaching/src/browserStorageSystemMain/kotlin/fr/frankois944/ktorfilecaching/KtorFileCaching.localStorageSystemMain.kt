@file:OptIn(ExperimentalSerializationApi::class)

package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.use

internal val prefix = "fr.frankois944.ktorfilecaching_key"

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
    private val mutexes = ConcurrentMap<String, Mutex>()

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ): Unit =
        withContext(dispatcher) {
            val urlHex = key(url)
            val caches = readCache(urlHex).filterNot { it.varyKeys == data.varyKeys } + data
            writeCache(urlHex, caches)
        }

    override suspend fun findAll(url: Url): Set<CachedResponseData> = readCache(key(url))

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
    ) = coroutineScope {
        val mutex = mutexes.computeIfAbsent(urlHex) { Mutex() }
        mutex.withLock {
            val serializedData = Cbor.encodeToHexString(caches.map { SerializableCachedResponseData(it) })
            Database.setItem("${prefix}_$urlHex", serializedData)
        }
    }

    private suspend fun readCache(urlHex: String): Set<CachedResponseData> {
        val mutex = mutexes.computeIfAbsent(urlHex) { Mutex() }
        return mutex.withLock {
            val item = Database.getItem("${prefix}_$urlHex")
            if (item == null) return@withLock emptySet()
            try {
                Cbor.decodeFromHexString<Set<SerializableCachedResponseData>>(item).map { it.cachedResponseData }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }
    }
}
