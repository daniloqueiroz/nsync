package nsync.storage

import nsync.FileChangedEvent
import nsync.SyncFolder
import nsync.index.SynchronizationStatus

interface StorageResolver {
    fun getStorageBackend(folder: SyncFolder): StorageBackend
}

interface StorageBackend {
    suspend fun syncFile(fileEvent: FileChangedEvent, callback: (FileChangedEvent, SynchronizationStatus) -> Unit)
}