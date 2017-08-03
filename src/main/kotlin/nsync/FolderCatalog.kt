package nsync

import mu.KLogging
import java.util.*

class FolderCatalog(private val conf: Configuration) {
    companion object : KLogging()

    init {
        if (this.conf.synchronization == null) {
            print("initialize map")
            this.conf.synchronization = mutableMapOf()
        }
    }

    fun register(localUri: String, remoteUri: String): SyncFolder {
        val folder = SyncFolder(UUID.randomUUID().toString(), localUri, remoteUri)
        logger.info { "Registering new $folder" }


        val dirs: MutableMap<String, ConfSyncFolder> = this.conf.synchronization!!
        dirs[folder.folderId] = ConfSyncFolder(folder.folderId, folder.localFolder, folder.remoteFolder)
        this.conf.synchronization = dirs

        return folder
    }

    fun find(uid: String): SyncFolder? {
        return this.conf.synchronization!![uid]?.let { SyncFolder(it.uid, it.localFolder, it.remoteFolder) }
    }

    operator fun iterator(): Iterator<SyncFolder> {
        return this.conf.synchronization!!.values?.map { SyncFolder(it.uid, it.localFolder, it.remoteFolder) }.iterator()
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