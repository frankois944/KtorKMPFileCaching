@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package fr.frankois944.ktorfilecaching

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun filesystem(): FileSystem = NodeJsFileSystem
