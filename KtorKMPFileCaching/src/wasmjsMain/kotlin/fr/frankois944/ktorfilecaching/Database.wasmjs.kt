@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import kotlinx.browser.window
import org.w3c.dom.set

internal actual object Database {
    actual suspend fun getItem(key: String): String? = window.localStorage.getItem(key)

    actual suspend fun setItem(
        key: String,
        value: String,
    ) {
        window.localStorage[key] = value
    }

    actual suspend fun removeItem(key: String) {
        window.localStorage.removeItem(key)
    }
}
