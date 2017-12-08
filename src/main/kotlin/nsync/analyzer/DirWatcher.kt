package nsync.analyzer

import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.signals.LocalFile
import nsync.signals.SyncFolder
import nsync.signals.FileDeleted
import nsync.signals.FileModified
import nsync.signals.SignalBus
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey


class DirWatcher(private val bus: SignalBus) {
    private data class Record(val uid: String, val dir: Path)
    private companion object : KLogging()

    private val service = FileSystems.getDefault().newWatchService()
    private val uids: MutableMap<WatchKey, Record> = mutableMapOf()
    private val receiver: Thread

    init {
        receiver = Thread({
            runBlocking {
                logger.info { "Waiting for FS events" }
                do {
                    val notified = service.take()
                    try {
                        process(notified)
                    } catch (err: Exception) {
                        logger.error(err) { "Error processing FS File Event" }
                    }
                    notified.reset()
                } while (true)
            }
        })
        receiver.name = "Analyzer.watcher-worker"
        receiver.isDaemon = true
        receiver.start()
    }

    internal fun watch(record: SyncFolder) {
        this.watch(record.folderId, Paths.get(record.pathLocal))
    }

    private fun watch(uid: String, dir: Path) {
        logger.info { "Watching $uid:$dir" }
        val key = dir.register(this.service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        uids[key] = Record(uid, dir)
    }

    private suspend fun process(notified: WatchKey) {
        val info = uids[notified]!!
        notified.pollEvents().forEach {
            val entry = info.dir.resolve(it.context() as Path)
            logger.debug { "File Event ${it.kind()} received for $entry" }

            when (it.kind()) {
                ENTRY_MODIFY -> if (entry.toFile().isFile) {
                    this.publish(LocalFile(info.uid, entry), false)
                }
                ENTRY_CREATE -> if (entry.toFile().isDirectory) {
                    this.watch(info.uid, entry)
                } else {
                    this.publish(LocalFile(info.uid, entry), false)
                }
                ENTRY_DELETE -> if (entry.toFile().isDirectory) {
                    uids.remove(notified)
                } else {
                    this.publish(LocalFile(info.uid, entry), true)
                }
            }
        }
    }

    private suspend fun publish(file: LocalFile, deleted: Boolean) {
        bus.publish(if (deleted) ::FileDeleted else ::FileModified, file)
    }
}
