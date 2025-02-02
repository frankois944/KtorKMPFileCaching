package fr.frankois944.ktorfilecaching

import kotlinx.browser.window
import org.w3c.dom.set

internal actual object Window {
    actual fun getItem(key: String): String? = window.localStorage.getItem(key)

    actual fun setItem(
        key: String,
        value: String,
    ) {
        window.localStorage[key] = value
    }

    actual fun removeItem(key: String) {
        window.localStorage.removeItem(key)
    }
}
