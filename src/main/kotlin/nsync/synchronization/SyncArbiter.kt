package nsync.synchronization

import mu.KLogging
import nsync.*
import nsync.index.AsyncFileChannelIndex
import nsync.index.DataRecord
import nsync.index.Index
import nsync.index.SynchronizationStatus
import java.nio.file.Path
import java.util.*


/**
 * The SyncArbiter is responsible to process fileChanges events and decide
 * whether to propagate then to storage layer or not and to keep the indexes
 * updated.
 */
class SyncArbiter(
        private val metadataDirectory: Path,
        private val catalog: FolderCatalog) : Consumer {
    private companion object : KLogging()

    private val indexes: MutableMap<String, Index> = mutableMapOf()

    private fun relativePath(folder: SyncFolder, file: Path) = folder.fileRelativePath(file)

    init {
        NBus.register(this, SyncFolder::class, LocalFile::class, SyncStatus::class)
    }

    override suspend fun onEvent(event: NSyncEvent) {
        when (event) {
            is SyncFolder -> this.dirAdded(event)
            is LocalFile -> this.fileChanged(event)
            is SyncStatus -> this.syncStatusChanged(event)
        }
    }

    /**
     * Notifies that a new directory was added with the given folderId.
     *
     * It creates an empty index to handle future events for that dir.
     */
    private fun dirAdded(folder: SyncFolder) {
        logger.info { "Metadata index for $folder" }
        val uid = folder.folderId
        val index = AsyncFileChannelIndex(this.metadataDirectory, uid)
        this.indexes[uid] = index
    }

    /**
     * Notifies that a file has changed.
     *
     * Updates the index and decide whether to notify or not the storage layer.
     */
    private suspend fun fileChanged(file: LocalFile) {
        val syncFolder = this.catalog.find(file.folderId) ?: return
        logger.info { "File received $file" }

        val index = this.indexes[file.folderId]!!
        val record = this.findRecord(index, file, relativePath(syncFolder, file.localFilePath))

        /* TODO review states
         * for now sending pending ones are ok, maybe later we need some other intermediary state
         * such as "SCHEDULED" to tell that was already sent to storage, but isn't being transferred yet
         */
        if (record.status == SynchronizationStatus.PENDING) {
            logger.info { "Requesting synchronization for file ${file.localFilePath}" }
            NBus.publish(SyncRequest(file.localFilePath, syncFolder))
        }
    }

    /**
     * Notifies a [SynchronizationStatus] change for the given [LocalFile].
     *
     * Updates the index information.
     */
    private suspend fun syncStatusChanged(event: SyncStatus) {
        catalog.find(event.folderId)?.let {
            val relativePath = relativePath(it, event.localFilePath)
            val index = indexes[event.folderId]!!
            val record = index[relativePath].await()!!
            val updated = DataRecord(record.checksum, record.size, record.modificationTs, event.status)
            index[relativePath] = updated
            logger.info { "File ${event.localFilePath} event changed from ${record.status} to ${event.status}" }
        }
    }

    private suspend fun findRecord(index: Index, event: LocalFile, relativePath: String): DataRecord {
        val record: DataRecord = this.createRecord(event.localFilePath)
        val currentRecord = index[relativePath].await()
        return if (Arrays.equals(record.checksum, currentRecord?.checksum)) {
            logger.debug {
                "Checksum for ${event.localFilePath} matches stored checksum." +
                        " Status: ${currentRecord?.status}"
            }
            currentRecord!!
        } else {
            logger.debug {
                "Checksum for ${event.localFilePath} differ. " +
                        "Stored ${currentRecord?.checksum?.toHexString()}; Current: ${record.checksum.toHexString()}"
            }
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
