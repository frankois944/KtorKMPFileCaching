@file:Suppress("NOTHING_TO_INLINE")

package fr.frankois944.ktorfilecaching

internal inline fun <T : Any> jso(): T = js("({})")

internal inline fun <T : Any> jso(block: T.() -> Unit): T = jso<T>().apply(block)
