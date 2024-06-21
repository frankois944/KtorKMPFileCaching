package fr.frankois944.ktorfilecaching

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun filesystem(): FileSystem = NodeJsFileSystem
