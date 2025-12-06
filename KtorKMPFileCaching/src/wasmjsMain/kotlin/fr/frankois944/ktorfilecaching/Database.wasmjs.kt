@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import fr.frankois944.ktorfilecaching.schema.KtorFileCachingDatabase
import fr.frankois944.ktorfilecaching.schema.KtorFileCachingDatabase.Companion.invoke
import kotlinx.browser.window
import org.w3c.dom.set

internal actual object Database {
    private var database: KtorFileCachingDatabase? = null

    suspend fun getDatabase(): KtorFileCachingDatabase {
        if (database == null) {
            val driver = createDefaultWebWorkerDriver()
            KtorFileCachingDatabase.Schema.awaitCreate(driver)
            database = KtorFileCachingDatabase(driver)
        }
        return database!!
    }

    actual suspend fun getItem(key: String): String? =
        getDatabase()
            .databaseCacheQueries
            .selectOne(key)
            .awaitAsOneOrNull()
            ?.value_

    actual suspend fun setItem(
        key: String,
        value: String,
    ) {
        getDatabase()
            .databaseCacheQueries
            .insert(key, value)
    }

    actual suspend fun removeItem(key: String) {
        getDatabase()
            .databaseCacheQueries
            .delete(key)
    }
}
