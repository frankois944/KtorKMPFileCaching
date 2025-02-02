package fr.frankois944.ktorfilecaching

import okio.FileSystem

internal actual fun filesystem(): FileSystem = FileSystem.SYSTEM