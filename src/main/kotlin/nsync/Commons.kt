package nsync

import mu.KLogging
import java.io.Serializable
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

data class SyncFolder(
        val uid: String,
        val localFolder: String,
        val remoteFolder: String) : Serializable {

    val uriRemote: URI by lazy { URI(remoteFolder) }
    val schemeRemote: String by lazy { uriRemote.scheme }
    val pathRemote: String by lazy { uriRemote.path }

    val uriLocal: URI by lazy { URI(localFolder) }
    val schemeLocal: String by lazy { uriLocal.scheme }
    val pathLocal: String by lazy { uriLocal.path }

    fun fileRelativePath(localFile: Path): String = Paths.get(pathLocal).relativize(localFile).toString()
}


data class FileChangedEvent(
        val uid: String,
        val localFilePath: Path)


class FolderCatalog(private val conf: Configuration) {
    companion object : KLogging()

    init {
        if (this.conf.synchronization == null) {
            print("initialize map")
            this.conf.synchronization = mutableMapOf()
        }
    }

    fun register(localUri: String, remoteUri: String): SyncFolder {
        val sync = SyncFolder(UUID.randomUUID().toString(), localUri, remoteUri)
        logger.info { "Registering new directory ${sync}" }


        val dirs: MutableMap<String, SyncFolder> = this.conf.synchronization!!
        dirs[sync.uid] = sync
        this.conf.synchronization = dirs

        return sync
    }

    fun find(uid: String): SyncFolder? {
        return this.conf.synchronization!![uid]
    }

    operator fun iterator(): Iterator<SyncFolder> {
        return this.conf.synchronization!!.values.iterator()
    }
}

// extensions to print Byte/ByteArray as Hex

private val CHARS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

internal fun Byte.toHexString(): String {
    val i = this.toInt()
    val char2 = CHARS[i and 0x0f]
    val char1 = CHARS[i shr 4 and 0x0f]
    return "$char1$char2"
}

internal fun ByteArray.toHexString(): String {
    val builder = StringBuilder()
    for (b in this) {
        builder.append(b.toHexString())
    }
    return builder.toString()
}