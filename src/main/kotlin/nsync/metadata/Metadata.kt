package nsync.metadata

import java.net.URI

internal const val URI_SCHEMA = "nsync"

internal fun buildIdentifier(folderId: String, fileId: String? = null): URI {
    val uri = "$URI_SCHEMA:$folderId"
    val uri1 = if (fileId != null)
        "$uri:$fileId"
    else
        uri
    return URI(uri1)
}

internal fun isFileIdentifier(uri: URI): Boolean {
    return false
}

enum class Status {
    PENDING, TRANSFERRING, SYNCHRONIZED
}

interface Resource {
    val uri: URI
    fun isFile(): Boolean {
        return isFileIdentifier(this.uri)
    }
}

data class FSMeta(
        val id: String,
        val localUri: URI,
        val remoteUri: URI,
        override val uri: URI = buildIdentifier(id)
) : Resource {
    override fun toString(): String {
        return "(uri=$uri local=$localUri remote=$remoteUri)"

    }
}

data class FileMeta(
        val id: String,
        val fsId: String,
        val relativePath: String,
        val checksum: String, // 16 B
        val size: Long, // 8 B
        val modificationTs: Long, // 8 B
        val status: Status, // 4 B
        override val uri: URI = buildIdentifier(fsId, id)
) : Resource {
    override fun toString(): String {
        return "(uri=$uri relativePath=$relativePath size=$size modified=$modificationTs status=$status)"
    }
}

interface NameSystem {
    fun resolveToUri(fsId: String, relativePath: String): URI?
    fun resolveUri(uri: URI): Resource?
    fun forEach(operation: (FSMeta) -> Unit)
}
