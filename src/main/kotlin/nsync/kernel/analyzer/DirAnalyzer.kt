package nsync.kernel.analyzer

import nsync.kernel.SyncFolder
import nsync.kernel.bus.*


class DirAnalyzer(bus: NBus) : Consumer {
    private val watcher = DirWatcher(bus)
    private val scanner = DirScanner(bus)

    init {
        bus.register(this, FolderAdded::class)
    }

    override suspend fun handle(msg: Signal<*>) {
        this.analyze(msg.payload as SyncFolder)
    }

    private suspend fun analyze(folder: SyncFolder) {
        watcher.watch(folder)
        scanner.scan(folder)
    }
}