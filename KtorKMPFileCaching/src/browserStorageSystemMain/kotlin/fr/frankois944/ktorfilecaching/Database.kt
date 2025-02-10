@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

internal expect object Database {
    suspend fun getItem(key: String): String?

    suspend fun setItem(
        key: String,
        value: String,
    )

    suspend fun removeItem(key: String)
}
