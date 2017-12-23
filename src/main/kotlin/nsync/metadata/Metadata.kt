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

data class FS(
        val localFolder: URI,
        val remoteFolder: URI,
        val identifier: URI? = null)

enum class Status {
    PENDING, TRANSFERRING, SYNCHRONIZED
}

interface Metadata {
    fun filesystems(): Sequence<FS>
}