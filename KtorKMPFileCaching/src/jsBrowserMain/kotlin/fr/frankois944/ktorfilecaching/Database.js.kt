@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import com.juul.indexeddb.Database
import com.juul.indexeddb.Key
import com.juul.indexeddb.KeyPath
import com.juul.indexeddb.openDatabase
import fr.frankois944.ktorfilecaching.database.CacheItem

internal actual object Database {
    private var database: Database? = null

    private const val DATABASE_NAME = "KTorKmpFileCaching"
    private const val STORE_NAME = "cacheitems"

    suspend fun getDatabase(): Database {
        if (database == null) {
            database =
                openDatabase(DATABASE_NAME, 1) { database, oldVersion, _ ->
                    if (oldVersion < 1) {
                        val store = database.createObjectStore(STORE_NAME, KeyPath("cacheKey"))
                        store.createIndex("cacheValue", KeyPath("cacheValue"), unique = false)
                    }
                }
        }
        return database!!
    }

    actual suspend fun getItem(key: String): String? =
        getDatabase()
            .transaction(STORE_NAME) {
                objectStore(STORE_NAME).get(Key(key)) as? CacheItem
            }?.cacheValue

    actual suspend fun setItem(
        key: String,
        value: String,
    ) {
        getDatabase().writeTransaction(STORE_NAME) {
            objectStore(STORE_NAME).put(
                jso<CacheItem> {
                    cacheKey = key
                    cacheValue = value
                },
            )
        }
    }

    actual suspend fun removeItem(key: String) {
        getDatabase().writeTransaction(STORE_NAME) {
            objectStore(STORE_NAME).delete(Key(key))
        }
    }
}
