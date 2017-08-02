package nsync.synchronization

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import mu.KLogging
import nsync.FileChangedEvent
import nsync.FolderCatalog
import nsync.SyncFolder
import nsync.index.AsyncFileChannelIndex
import nsync.index.DataRecord
import nsync.index.Index
import nsync.index.SynchronizationStatus
import nsync.storage.StorageResolver
import java.nio.file.Path


/**
 * The SyncArbiter is responsible to process fileChanges events and decide
 * whether to propagate then to storage layer or not and to keep the indexes
 * updated.
 */
class SyncArbiter(
        private val metadataDirectory: Path,
        private val storage: StorageResolver,
        private val catalog: FolderCatalog) {
    private companion object : KLogging()

    val indexes: MutableMap<String, Index> = mutableMapOf()

    private fun relativePath(folder: SyncFolder, file: Path) = folder.fileRelativePath(file)
    /**
     * Notifies that a new directory was added with the given uid.
     *
     * It creates an empty index to handle future events for that dir.
     */
    fun dirAdded(folder: SyncFolder) {
        logger.info { "Metadata index for $folder" }
        val uid = folder.uid
        val index = AsyncFileChannelIndex(this.metadataDirectory, uid)
        this.indexes[uid] = index
    }

    /**
     * Notifies that a file has changed.
     *
     * Updates the index and decide whether to notify or not the storage layer.
     */
    suspend fun fileChanged(event: FileChangedEvent) {
        val syncFolder = this.catalog.find(event.uid) ?: return
        logger.info { "File event received $event - $syncFolder" }

        val index = this.indexes[event.uid]!!
        val record = this.findRecord(index, event, relativePath(syncFolder, event.localFilePath))

        /* TODO review states
         * for now sending pending ones are ok, maybe later we need some other intermediary state
         * such as "SCHEDULED" to tell that was already sent to storage, but isn't being transferred yet
         */
        if (record.status == SynchronizationStatus.PENDING) {
            val backend = this.storage.getStorageBackend(syncFolder)
            backend.syncFile(event, this::syncStatusChanged)
        }
    }

    /**
     * Notifies a [SynchronizationStatus] change for the given [FileChangedEvent].
     *
     * Updates the index information.
     */
    fun syncStatusChanged(event: FileChangedEvent, newState: SynchronizationStatus) {
        async(CommonPool) {
            print(catalog.find(event.uid))
            catalog.find(event.uid)?.let {
                val relativePath = relativePath(it, event.localFilePath)
                val index = indexes[event.uid]!!
                val record = index[relativePath].await()!!
                val updated = DataRecord(record.checksum, record.size, record.modificationTs, newState)
                index[relativePath] = updated
                logger.info { "File ${event.localFilePath} status changed from ${record.status} to ${newState}" }
            }
        }
    }

    private suspend fun findRecord(index: Index, event: FileChangedEvent, relativePath: String): DataRecord {
        val record: DataRecord = this.createRecord(event.localFilePath)
        val currentRecord = index[relativePath].await()
        return if (record.checksum == currentRecord?.checksum) {
            currentRecord
        } else {
            index[relativePath] = record
            record
        }
    }

    private suspend fun createRecord(filePath: Path): DataRecord {
        // TODO handle deleted files
        val f = filePath.toFile()
        val size = f.length()
        val ts = f.lastModified()
        val checksum = Checksum(f).compute()

        return DataRecord(checksum, size, ts, SynchronizationStatus.PENDING)
    }
}