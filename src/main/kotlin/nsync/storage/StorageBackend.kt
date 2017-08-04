package nsync.storage

import nsync.SyncFolder
import java.nio.file.Path

interface StorageBackend {
    suspend fun syncFile(localFile: Path, folder: SyncFolder)
}