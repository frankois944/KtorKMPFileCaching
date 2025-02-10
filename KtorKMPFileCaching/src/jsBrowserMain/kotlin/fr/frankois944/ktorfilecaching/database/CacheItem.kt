package fr.frankois944.ktorfilecaching.database

internal external interface CacheItem {
    var cacheKey: String
    var cacheValue: String?
}
