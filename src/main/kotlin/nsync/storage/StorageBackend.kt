package nsync.storage

import nsync.SyncFolder
import java.nio.file.Path

interface StorageBackend {
    suspend fun syncFile(syncId: String, localFile: Path, folder: SyncFolder)
}