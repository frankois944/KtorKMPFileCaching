package fr.frankois944.ktorfilecaching

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import kotlinx.browser.window
import kotlinx.serialization.json.Json

private var cacheIndex: MutableScatterSet<String>? = null
internal val prefix = "fr.frankois944.ktorfilecaching_key"

private fun loadIndex(): MutableScatterSet<String> {
    if (cacheIndex == null) {
        window.localStorage.getItem("${prefix}_index")?.let {
            cacheIndex = Json.decodeFromString(it)
        }
    }
    return cacheIndex ?: mutableScatterSetOf()
}

private fun saveIndex(value: ScatterSet<String>) {
    window.localStorage.setItem("${prefix}_index", Json.encodeToString(value))
}

internal fun getIndex(): ScatterSet<String> = loadIndex()

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
