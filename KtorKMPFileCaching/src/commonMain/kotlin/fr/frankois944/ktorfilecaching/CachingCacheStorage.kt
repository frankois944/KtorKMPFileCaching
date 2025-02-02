package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.util.collections.ConcurrentMap
import kotlin.collections.set

internal class CachingCacheStorage(
    private val delegate: CacheStorage,
) : CacheStorage {
    private val store = ConcurrentMap<Url, Set<CachedResponseData>>()

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ) {
        delegate.store(url, data)
        store[url] = delegate.findAll(url)
    }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? {
        if (!store.containsKey(url)) {
            store[url] = delegate.findAll(url)
        }
        val data = store.getValue(url)
        return data.find {
            varyKeys.all { (key, value) -> it.varyKeys[key] == value }
        }
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        if (!store.containsKey(url)) {
            store[url] = delegate.findAll(url)
        }
        return store.getValue(url)
    }
}
