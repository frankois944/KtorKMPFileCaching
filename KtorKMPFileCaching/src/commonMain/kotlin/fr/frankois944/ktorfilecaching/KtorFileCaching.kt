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
    private val cacheSystem = CacheSystem(fileSystem, cacheDir)

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
                cacheSystem.purge()
                metadataCache.clear()
            } else {
                val urlToPath = urlToPath(forUrl)
                val urlCacheDir = cacheDir.resolve(urlToPath)
                cacheSystem.purge(urlCacheDir)
                metadataCache.remove(urlToPath)
            }
        }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? = lock.withLock {
        val urlCacheDir = cacheDir.resolve(urlToPath(url))
        val varyKeyHash = hashVaryKeys(varyKeys)
        cacheSystem.read(urlCacheDir, varyKeyHash)?.let {
            Json.decodeFromString<SerializableCachedResponseData>(it)
        }?.cachedResponseData
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> = lock.withLock {
        val urlToPath = urlToPath(url)
        val urlCacheDir = cacheDir.resolve(urlToPath)
        if (!cacheSystem.exist(urlCacheDir, null)) return emptySet()

        metadataCache[urlToPath]?.addAll(cacheSystem.contentOf(urlCacheDir).map { it.name })
        metadataCache[urlToPath]?.mapNotNull { varyKeyHash ->
            cacheSystem.read(urlCacheDir, varyKeyHash)?.let {
                Json.decodeFromString<SerializableCachedResponseData>(it)
            }?.cachedResponseData
        }?.toSet() ?: emptySet()
    }

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ): Unit = lock.withLock {
        val urlToPath = urlToPath(url)
        val urlCacheDir = cacheDir.resolve(urlToPath)
        val varyKeyHash = hashVaryKeys(data.varyKeys)
        cacheSystem.write(urlCacheDir, varyKeyHash, Json.encodeToString(SerializableCachedResponseData(data)))
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
