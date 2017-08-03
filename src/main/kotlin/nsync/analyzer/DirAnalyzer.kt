package nsync.analyzer

import nsync.SyncFolder
import nsync.synchronization.SyncArbiter


class DirAnalizer(arbiter: SyncArbiter) {
    private val scanner = DirScanner(arbiter)
    private val watcher = DirWatcher(arbiter)

    fun start() {
        scanner.start()
        watcher.start()
    }

    suspend fun analize(folder: SyncFolder) {
        scanner.scan(folder)
        watcher.watch(folder)
    }
}