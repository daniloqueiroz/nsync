package nsync.synchronization

import mu.KLogging
import nsync.*
import java.nio.file.Path


/**
 * The SyncServer is responsible to process fileChanges events and decide
 * whether to propagate then to storage layer or not and to keep the indexes
 * updated.
 */
class SyncServer(bus: SignalBus) : Server(bus, listOf(FileDeleted::class, FileModified::class)) {
    private companion object : KLogging()

    private fun relativePath(folder: SyncFolder, file: Path) = folder.fileRelativePath(file)

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
