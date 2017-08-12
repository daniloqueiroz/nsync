package nsync.kernel.analyzer

import nsync.kernel.SyncFolder
import nsync.kernel.bus.*


class DirAnalyzer : Consumer {
    private val watcher = DirWatcher()
    private val scanner = DirScanner()

    init {
        NBus.register(this, FolderAdded::class)
    }

    override suspend fun onEvent(msg: Signal<*>) {
        this.analyze(msg.payload as SyncFolder)
    }

    private suspend fun analyze(folder: SyncFolder) {
        watcher.watch(folder)
        scanner.scan(folder)
    }
}