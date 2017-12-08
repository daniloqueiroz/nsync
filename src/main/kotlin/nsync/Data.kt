package nsync

import nsync.metadata.Status
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths


data class SyncFolder(
        val folderId: String,
        val localFolder: String,
        val remoteFolder: String) {

    val uriRemote: URI by lazy { URI(remoteFolder) }
    val schemeRemote: String by lazy { uriRemote.scheme }
    val pathRemote: String by lazy { uriRemote.path }

    val uriLocal: URI by lazy { URI(localFolder) }
    val schemeLocal: String by lazy { uriLocal.scheme }
    val pathLocal: String by lazy { uriLocal.path }

    fun fileRelativePath(localFile: Path): String = Paths.get(pathLocal).relativize(localFile).toString()
}

data class LocalFile(
        val folderId: String,
        val localFilePath: Path)

data class RemoteFile(
        val localFilePath: Path,
        val folder: SyncFolder
)

data class TransferStatus(
        val folderId: String,
        val localFilePath: Path,
        val status: Status
)