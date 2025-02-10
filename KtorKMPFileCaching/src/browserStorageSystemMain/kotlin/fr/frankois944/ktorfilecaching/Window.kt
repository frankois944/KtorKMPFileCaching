@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

internal expect object Window {
    fun getItem(key: String): String?

    fun setItem(
        key: String,
        value: String,
    )

    fun removeItem(key: String)
}
