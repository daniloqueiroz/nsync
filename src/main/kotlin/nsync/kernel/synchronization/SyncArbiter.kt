package nsync.kernel.synchronization

import mu.KLogging
import nsync.kernel.LocalFile
import nsync.kernel.SyncFolder
import nsync.kernel.bus.*
import java.nio.file.Path


/**
 * The SyncArbiter is responsible to process fileChanges events and decide
 * whether to propagate then to storage layer or not and to keep the indexes
 * updated.
 */
// TODO SyncServer
class SyncArbiter(
        private val bus: SignalBus) : Consumer {
    private companion object : KLogging()

    private fun relativePath(folder: SyncFolder, file: Path) = folder.fileRelativePath(file)

    init {
        bus.register(this, FileDeleted::class, FileModified::class)
    }

    override suspend fun handle(msg: Signal<*>) {
        when (msg) {
            is FileModified -> this.fileChanged(msg.payload)
            is FileDeleted -> this.fileDeleted(msg.payload)
        }
    }

    /**
     * Notifies that a file has changed.
     *
     * Updates the index and decide whether to notify or not the storage layer.
     */
    private suspend fun fileChanged(file: LocalFile) {
    }

    private suspend fun fileDeleted(file: LocalFile) {
    }
}
