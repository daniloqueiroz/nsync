package nsync.analyzer

import nsync.Consumer
import nsync.NBus
import nsync.NSyncEvent
import nsync.SyncFolder


class DirAnalyzer : Consumer<NSyncEvent> {
    private val watcher = DirWatcher()

    init {
        NBus.register(this, SyncFolder::class)
    }

    override suspend fun onEvent(event: NSyncEvent) {
        this.analyze(event as SyncFolder)
    }

    private suspend fun analyze(folder: SyncFolder) {
        watcher.watch(folder)
        DirScanner.scan(folder)
    }
}