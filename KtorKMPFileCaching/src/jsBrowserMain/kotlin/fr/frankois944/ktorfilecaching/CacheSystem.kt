package fr.frankois944.ktorfilecaching

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private var cacheIndex: MutableSet<String>? = null
internal val prefix = "fr.frankois944.ktorfilecaching_key"

private fun loadIndex() : MutableSet<String> {
    if (cacheIndex == null) {
        window.localStorage.getItem("${prefix}_index")?.let {
            cacheIndex = Json.decodeFromString(it)
        }
    }
    return cacheIndex ?: mutableSetOf()
}

private fun saveIndex(value: Set<String>) {
    window.localStorage.setItem("${prefix}_index", Json.encodeToString(value))
}

internal fun getIndex() : Set<String> {
    return loadIndex()
}

internal fun addToIndex(value: String) {
    with(loadIndex()) {
        if (add(value)) {
            saveIndex(this)
        }
    }
}

internal fun removeFromIndex(value: String) {
    with(loadIndex()) {
        if (remove(value)) {
            saveIndex(this)
        }
    }
}

internal fun purgeIndex() {
    cacheIndex = null
    window.localStorage.removeItem("${prefix}_index")
}

