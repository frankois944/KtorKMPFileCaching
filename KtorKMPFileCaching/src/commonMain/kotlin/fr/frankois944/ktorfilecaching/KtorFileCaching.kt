package fr.frankois944.ktorfilecaching

import co.touchlab.stately.collections.ConcurrentMutableMap
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

public class KtorFileCaching(
    private val fileSystem: FileSystem = filesystem(),
) : CacheStorage {

    //<editor-fold desc="Path">

    private val baseDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    private val separator = Path.DIRECTORY_SEPARATOR
    private val cacheDir = "${baseDir}${separator}KTorFileCaching$separator".toPath()

    //</editor-fold>

    private val lock = Mutex() // Coroutine-friendly lock for thread safety
    private val metadataCache = ConcurrentMutableMap<String, Set<String>>() // In-memory metadata cache for fast lookup

    @Suppress("ktlint:standard:property-naming")
    private val LOGGER = KtorSimpleLogger("fr.frankois944.ktorfilecaching")

    init {
        if (!fileSystem.exists(baseDir)) {
            fileSystem.createDirectories(baseDir, true)
        }
        if (!fileSystem.exists(cacheDir)) {
            fileSystem.createDirectories(cacheDir, true)
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
            try {
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
            } catch (ex: Exception) {
                if (forUrl != null) {
                    LOGGER.error("Can't [purge] cache for url $forUrl", ex)
                } else {
                    LOGGER.error("Can't [purge] all cache", ex)
                }
            }
        }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? = lock.withLock {
        try {
            val urlCacheDir = cacheDir.resolve(urlToPath(url))
            if (!fileSystem.exists(urlCacheDir)) {
                LOGGER.trace("Can't [find] cache : $url")
                return null
            }

            val varyKeyHash = hashVaryKeys(varyKeys)
            val filePath = urlCacheDir.resolve(varyKeyHash)
            if (fileSystem.exists(filePath)) {
                fileSystem.read(filePath) {
                    Json.decodeFromString<SerializableCachedResponseData>(buffer.readUtf8())
                }.cachedResponseData
            } else {
                null
            }
        } catch (ex: Exception) {
            LOGGER.error("Can't [find] cache: $url with varyKeys $varyKeys", ex)
            null
        }
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> = lock.withLock {
        try {
            val urlToPath = urlToPath(url)
            val urlCacheDir = cacheDir.resolve(urlToPath)
            if (!fileSystem.exists(urlCacheDir)) return emptySet()

            metadataCache.block { cache ->
                if (!cache.containsKey(urlToPath)) {
                    metadataCache[urlToPath] = fileSystem.list(urlCacheDir).map { it.name }.toSet()
                }
            }

            metadataCache[urlToPath]?.map { fileName ->
                val filePath = urlCacheDir.resolve(fileName)
                fileSystem.read(filePath) {
                    Json.decodeFromString<SerializableCachedResponseData>(buffer.readUtf8())
                }.cachedResponseData
            }?.toSet() ?: emptySet()
        } catch (ex: Exception) {
            LOGGER.error("Can't [findAll] cache: $url", ex)
            return emptySet()
        }
    }

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ): Unit = lock.withLock {
        try {
            val urlCacheDir = cacheDir.resolve(urlToPath(url))
            if (!fileSystem.exists(urlCacheDir)) {
                fileSystem.createDirectory(urlCacheDir)
            }

            val varyKeyHash = hashVaryKeys(data.varyKeys)
            val filePath = urlCacheDir.resolve(varyKeyHash)

            fileSystem.write(filePath) {
                buffer.writeUtf8(Json.encodeToString(data))
            }

            metadataCache.block {
                if (!it.containsKey(urlToPath(url))) {
                    it[urlToPath(url)] = mutableSetOf(varyKeyHash)
                }
            }
        } catch (ex: Exception) {
            LOGGER.trace("Can't [store] cache: $url", ex)
        }
    }

    private fun urlToPath(url: Url): String {
        return url.toString().hashCode().toString()
    }

    private fun hashVaryKeys(varyKeys: Map<String, String>): String {
        return varyKeys.toMap().entries.joinToString { "${it.key}=${it.value}" }.hashCode().toString()
    }
}
