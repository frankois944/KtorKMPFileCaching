package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal expect fun filesystem(): FileSystem

/**
 * Ktor file caching
 *
 * @property fileSystem
 * @constructor
 *
 * @param storedCacheDirectory the directories where the cache is, by default it's KTorFileCaching
 * @param rootStoredCachePath the root directory, by default it's SYSTEM_TEMPORARY_DIRECTORY of okio
 * @param fileSystem the okio filesystem instance
 */
public class KtorFileCaching(
    storedCacheDirectory: Path = "KTorFileCaching".toPath(),
    rootStoredCachePath: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY,
    private val fileSystem: FileSystem = filesystem()
) : CacheStorage {

    //<editor-fold desc="Path">

    private val cacheDir = "$rootStoredCachePath${Path.DIRECTORY_SEPARATOR}$storedCacheDirectory".toPath()

    //</editor-fold>

    private val lock = Mutex() // Coroutine-friendly lock for thread safety
    private val metadataCache =
        mutableMapOf<String, MutableSet<String>>() // In-memory metadata cache for fast lookup

    @Suppress("ktlint:standard:property-naming")
    private val LOGGER = KtorSimpleLogger("fr.frankois944.ktorfilecaching")

    init {
        if (!fileSystem.exists(rootStoredCachePath)) {
            fileSystem.createDirectories(rootStoredCachePath)
        }
        if (!fileSystem.exists(cacheDir)) {
            fileSystem.createDirectory(cacheDir)
        }
    }

    /**
     * Purge cache
     * Delete in the filesystem all related files and directories
     *
     * @param forUrl purge the cache for this url or all cached data if null
     */
    public suspend fun purgeCache(forUrl: Url? = null): Unit =
        lock.withLock {
            if (forUrl == null) {
                // Purge all
                fileSystem.list(cacheDir).forEach { fileSystem.deleteRecursively(it) }
                metadataCache.clear()
            } else {
                val urlToPath = urlToPath(forUrl)
                val urlCacheDir = cacheDir.resolve(urlToPath)
                if (fileSystem.exists(urlCacheDir)) {
                    fileSystem.deleteRecursively(urlCacheDir)
                }
                metadataCache.remove(urlToPath)
            }
        }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? = lock.withLock {
        val urlCacheDir = cacheDir.resolve(urlToPath(url))
        if (!fileSystem.exists(urlCacheDir)) {
            LOGGER.trace("Can't [find] cache : $url")
            return null
        }

        val varyKeyHash = hashVaryKeys(varyKeys)
        val filePath = urlCacheDir.resolve(varyKeyHash)
        if (fileSystem.exists(filePath)) {
            fileSystem.read(filePath) {
                readUtf8()
            }.run {
                Json.decodeFromString<SerializableCachedResponseData>(this)
            }.cachedResponseData
        } else {
            null
        }
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> = lock.withLock {
        val urlToPath = urlToPath(url)
        val urlCacheDir = cacheDir.resolve(urlToPath)
        if (!fileSystem.exists(urlCacheDir)) return emptySet()

        metadataCache[urlToPath]?.addAll(fileSystem.list(urlCacheDir).map { it.name })

        metadataCache[urlToPath]?.map { fileName ->
            val filePath = urlCacheDir.resolve(fileName)
            fileSystem.read(filePath) {
                readUtf8()
            }.run {
                Json.decodeFromString<SerializableCachedResponseData>(this)
            }.cachedResponseData
        }?.toSet() ?: emptySet()
    }

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ): Unit = lock.withLock {
        val urlToPath = urlToPath(url)
        val urlCacheDir = cacheDir.resolve(urlToPath)
        if (!fileSystem.exists(urlCacheDir)) {
            fileSystem.createDirectory(urlCacheDir)
        }

        val varyKeyHash = hashVaryKeys(data.varyKeys)
        val filePath = urlCacheDir.resolve(varyKeyHash)

        fileSystem.write(filePath) {
            buffer.writeUtf8(Json.encodeToString(SerializableCachedResponseData(data)))
        }

        metadataCache[urlToPath]?.run {
            add(varyKeyHash)
        } ?: run {
            metadataCache[urlToPath] = mutableSetOf(varyKeyHash)
        }
    }

    private fun urlToPath(url: Url): String {
        return url.hashCode().toString()
    }

    private fun hashVaryKeys(varyKeys: Map<String, String>): String {
        return varyKeys.hashCode().toString()
    }
}
