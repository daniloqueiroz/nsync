package nsync.kernel.synchronization

import mu.KLogging
import nsync.extensions.toHexString
import nsync.index.AsyncFileChannelIndex
import nsync.index.DataRecord
import nsync.index.Index
import nsync.index.SynchronizationStatus
import nsync.kernel.*
import nsync.kernel.bus.*
import java.nio.file.Path
import java.util.*


/**
 * The SyncArbiter is responsible to process fileChanges events and decide
 * whether to propagate then to storage layer or not and to keep the indexes
 * updated.
 */
class SyncArbiter(
        private val metadataDirectory: Path,
        private val catalog: FolderCatalog,
        private val bus: SignalBus) : Consumer {
    private companion object : KLogging()

    private val indexes: MutableMap<String, Index> = mutableMapOf()

    private fun relativePath(folder: SyncFolder, file: Path) = folder.fileRelativePath(file)

    init {
        bus.register(this, FolderAdded::class, FileModified::class, ChangeStatus::class)
    }

    override suspend fun handle(msg: Signal<*>) {
        when (msg) {
            is FolderAdded -> this.dirAdded(msg.payload)
            is FileModified -> this.fileChanged(msg.payload)
            is ChangeStatus -> this.syncStatusChanged(msg.payload)
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
        logger.info { "File event received for $file" }

        val index = this.indexes[file.folderId]!!
        val record = this.findRecord(index, file, relativePath(syncFolder, file.localFilePath))

        /* TODO review states
         * for now sending pending ones are ok, maybe later we need some other intermediary state
         * such as "SCHEDULED" to tell that was already sent to storage, but isn't being transferred yet
         */
        if (record.status == SynchronizationStatus.PENDING) {
            logger.info { "Requesting synchronization for file ${file.localFilePath}" }
            bus.publish(::TransferFile, RemoteFile(file.localFilePath, syncFolder))
        }
    }

    /**
     * Notifies a [SynchronizationStatus] change for the given [LocalFile].
     *
     * Updates the index information.
     */
    private suspend fun syncStatusChanged(event: TransferStatus) {
        catalog.find(event.folderId)?.let {
            val relativePath = relativePath(it, event.localFilePath)
            val index = indexes[event.folderId]!!
            index.get(relativePath)?.let {
                val updated = DataRecord(it.checksum, it.size, it.modificationTs, event.status)
                index.set(relativePath, updated)
                logger.info { "File ${event.localFilePath} event changed from ${it.status} to ${event.status}" }
            }
        }
    }

    private suspend fun findRecord(index: Index, event: LocalFile, relativePath: String): DataRecord {
        // TODO handle deleted files
        val record: DataRecord = this.createRecord(event.localFilePath)
        val currentRecord = index.get(relativePath)
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
            index.set(relativePath, record)
            record
        }
    }

    private suspend fun createRecord(filePath: Path): DataRecord {
        val f = filePath.toFile()
        val size = f.length()
        val ts = f.lastModified()
        val checksum = Checksum(f).compute()

        return DataRecord(checksum, size, ts, SynchronizationStatus.PENDING)
    }
}
