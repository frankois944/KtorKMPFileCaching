@file:OptIn(InternalCoroutinesApi::class)

package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal expect fun filesystem(): FileSystem

public class KtorFileCaching(
    private val fileSystem: FileSystem = filesystem(),
) : CacheStorage {
    private val baseDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    private val separator = Path.DIRECTORY_SEPARATOR
    private val cacheDir = "${baseDir}${separator}KTorFileCaching$separator"

    @Suppress("ktlint:standard:property-naming")
    private val LOGGER = KtorSimpleLogger("fr.frankois944.ktorfilecaching")

    @OptIn(InternalCoroutinesApi::class)
    private val lockObject = SynchronizedObject()

    /**
     * Purge cache
     * Delete in the filesystem all related files and directories
     *
     * @param forUrl purge the cache for this url or all cached data if null
     */
    public fun purgeCache(forUrl: Url? = null) {
        try {
            synchronized(lockObject) {
                forUrl?.let {
                    val urlPath = "${cacheDir}$forUrl".toPath()
                    if (fileSystem.exists(urlPath)) {
                        fileSystem.deleteRecursively(urlPath)
                    }
                } ?: run {
                    if (fileSystem.exists(cacheDir.toPath())) {
                        fileSystem.deleteRecursively(cacheDir.toPath())
                    }
                }
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
    ): CachedResponseData? {
        try {
            val fileCacheDir = "${cacheDir}${url.hashCode()}".toPath()
            return if (fileSystem.exists(fileCacheDir)) {
                fileSystem
                    .list(fileCacheDir)
                    .firstNotNullOfOrNull {
                        if (it.name.endsWith("${varyKeys.hashCode()}")) {
                            fileSystem.read(it) {
                                Json.decodeFromString<SerializableCachedResponseData>(this.readUtf8()).cachedResponseData
                            }
                        } else {
                            null
                        }
                    }
            } else {
                null
            }
        } catch (ex: Exception) {
            LOGGER.error("Can't [find] cache: $url with varyKeys $varyKeys", ex)
            return null
        }
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        try {
            val fileCacheDir = "${cacheDir}${url.hashCode()}".toPath()
            return if (fileSystem.exists(fileCacheDir)) {
                fileSystem
                    .list(fileCacheDir)
                    .map {
                        fileSystem.read(it) {
                            val content = readUtf8()
                            Json.decodeFromString<SerializableCachedResponseData>(content).cachedResponseData
                        }
                    }.toSet()
            } else {
                emptySet()
            }
        } catch (ex: Exception) {
            LOGGER.error("Can't [findAll] cache: $url", ex)
            return emptySet()
        }
    }

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ) {
        try {
            synchronized(lockObject) {
                val cachedResponseData = SerializableCachedResponseData(data)
                val fileCacheDir = "${cacheDir}${url.hashCode()}".toPath()
                if (!fileSystem.exists(fileCacheDir)) {
                    fileSystem.createDirectories(fileCacheDir)
                }
                val cacheFilePath = "$fileCacheDir$separator${data.varyKeys.hashCode()}".toPath()
                if (fileSystem.exists(cacheFilePath)) {
                    fileSystem.delete(cacheFilePath)
                }
                fileSystem.write(cacheFilePath) {
                    this.writeUtf8(Json.encodeToString(cachedResponseData))
                }
            }
        } catch (ex: Exception) {
            LOGGER.trace("Can't [store] cache: $url", ex)
        }
    }
}
