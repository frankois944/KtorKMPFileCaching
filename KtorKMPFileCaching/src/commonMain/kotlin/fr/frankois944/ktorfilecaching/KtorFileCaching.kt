package fr.frankois944.ktorfilecaching

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.String

/**
 * Ktor file caching
 *
 * @param storedCacheDirectory the directories where the cache is, by default : `KTorFileCaching`
 * @param rootStoredCachePath the root directory, by default : `SYSTEM_TEMPORARY_DIRECTORY` of okio
 * @param dispatcher to use for writing file operations
 * @param fileSystem the okio filesystem instance
 */
public class KtorFileCaching(
    storedCacheDirectory: Path = "KTorFileCaching".toPath(),
    rootStoredCachePath: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val fileSystem: FileSystem = filesystem(),
) : CacheStorage {
    // <editor-fold desc="Path">

    private val cacheDir = "$rootStoredCachePath${Path.DIRECTORY_SEPARATOR}$storedCacheDirectory".toPath()
    private val cacheSystem = CacheSystem(fileSystem, cacheDir)

    // </editor-fold>

    private val lock = Mutex() // Coroutine-friendly lock for thread safety
    private val metadataCache =
        mutableScatterMapOf<String, MutableScatterSet<String>>() // In-memory metadata cache for fast lookup

    @Suppress("ktlint:standard:property-naming")
    private val LOGGER = KtorSimpleLogger("fr.frankois944.ktorfilecaching")

    /**
     * Purge cache
     * Delete in the filesystem all related files and directories
     *
     * @param forUrl purge the cache for this url or all cached data if null
     */
    public suspend fun purgeCache(forUrl: Url? = null): Unit =
        lock.withLock {
            withContext(dispatcher) {
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
        }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? =
        lock.withLock {
            LOGGER.debug(
                """
                FIND: 
                url = $url
                varyKeys = $varyKeys
                """.trimIndent(),
            )
            val urlCacheDir = cacheDir.resolve(urlToPath(url))
            val varyKeyHash = hashVaryKeys(varyKeys)
            cacheSystem
                .read(urlCacheDir, varyKeyHash)
                ?.let {
                    Json.decodeFromString<SerializableCachedResponseData>(it)
                }?.cachedResponseData
        }

    override suspend fun findAll(url: Url): Set<CachedResponseData> =
        lock.withLock {
            val urlToPath = urlToPath(url)
            val urlCacheDir = cacheDir.resolve(urlToPath)
            if (!cacheSystem.exist(urlCacheDir, null)) return emptySet()

            LOGGER.debug(
                """
                FINDALL: 
                url = $url
                """.trimIndent(),
            )

            loadCacheOfPath(urlToPath, urlCacheDir)
            return@withLock mutableScatterSetOf<CachedResponseData>()
                .apply {
                    metadataCache[urlToPath]?.forEach { varyKeyHash ->
                        cacheSystem
                            .read(urlCacheDir, varyKeyHash)
                            ?.let {
                                Json.decodeFromString<SerializableCachedResponseData>(it)
                            }?.cachedResponseData
                            ?.let {
                                this.add(it)
                            }
                    }
                }.asSet()
        }

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ): Unit =
        lock.withLock {
            withContext(dispatcher) {
                val urlToPath = urlToPath(url)
                val urlCacheDir = cacheDir.resolve(urlToPath)
                val varyKeyHash = hashVaryKeys(data.varyKeys)
                LOGGER.debug(
                    """
                STORE: 
                url = $url
                varyKeys = ${data.varyKeys}
                """.trimIndent(),
                )
                loadCacheOfPath(urlToPath, urlCacheDir)
                cacheSystem.write(urlCacheDir, varyKeyHash, Json.encodeToString(SerializableCachedResponseData(data)))
                metadataCache[urlToPath]?.run {
                    add(varyKeyHash)
                } ?: run {
                    metadataCache[urlToPath] = mutableScatterSetOf(varyKeyHash)
                }
            }
        }

    private fun urlToPath(url: Url): String = url.hashCode().toString()

    private fun hashVaryKeys(varyKeys: Map<String, String>): String = varyKeys.hashCode().toString()

    private fun loadCacheOfPath(
        urlToPath: String,
        urlCacheDir: Path,
    ) {
        if (metadataCache[urlToPath] == null) {
            metadataCache[urlToPath] =
                mutableScatterSetOf<String>().apply {
                    cacheSystem.contentOf(urlCacheDir).forEach {
                        this.add(it.name)
                    }
                }
        }
    }
}
