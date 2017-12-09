package nsync.analyzer

import commons.Server
import commons.Signal
import commons.SignalBus
import nsync.SyncFolder

class LocalFileWatchServer(bus: SignalBus) : Server(bus, listOf()) {
    private val watcher = DirWatcher(bus)
    private val scanner = DirScanner(bus)


    override suspend fun handle(msg: Signal<*>) {
        this.analyze(msg.payload as SyncFolder)
    }

    private suspend fun analyze(folder: SyncFolder) {
        watcher.watch(folder)
        scanner.scan(folder)
    }
}

