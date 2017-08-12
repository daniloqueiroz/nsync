package nsync.kernel.storage

import nsync.kernel.SyncFolder
import java.nio.file.Path

interface StorageBackend {
    suspend fun syncFile(localFile: Path, folder: SyncFolder)
}