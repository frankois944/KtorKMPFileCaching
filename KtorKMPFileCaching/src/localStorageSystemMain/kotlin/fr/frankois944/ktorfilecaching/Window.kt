package fr.frankois944.ktorfilecaching

internal expect object Window {
    fun getItem(key: String): String?

    fun setItem(
        key: String,
        value: String,
    )

    fun removeItem(key: String)
}
