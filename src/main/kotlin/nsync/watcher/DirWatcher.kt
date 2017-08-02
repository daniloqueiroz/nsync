package nsync.watcher

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.FileChangedEvent
import nsync.SyncFolder
import nsync.synchronization.SyncArbiter
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey

private data class Record(val uid: String, val dir: Path)

class DirWatcher(private val arbiter: SyncArbiter) {
    companion object : KLogging()

    private val service = FileSystems.getDefault().newWatchService()
    private val uids: MutableMap<WatchKey, Record> = mutableMapOf()
    private val chn = Channel<WatchKey>()
    private var poll: Thread? = null

    fun watch(record: SyncFolder) {
        logger.info { "Watching directory ${record.uid} ${record.localFolder}" }
        this.watch(record.uid, Paths.get(record.pathLocal))
    }

    private fun watch(uid: String, dir: Path) {
        val key = dir.register(this.service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        uids[key] = Record(uid, dir)
    }

    fun start() {
        poll = Thread({
            runBlocking {
                logger.info { "Waiting for FS events" }
                do {
                    val notified = service.take()
                    chn.send(notified)
                    notified.reset()
                } while (true)
            }
        })
        poll?.name = "FSEventPoll"
        poll?.isDaemon = true
        poll?.start()

        async(CommonPool) {
            do {
                try {
                    process(chn.receive())
                } catch (err: Exception) {
                    logger.error(err) { "Error processing FS File Event" }
                }
            } while (true)
        }
    }

    private suspend fun process(notified: WatchKey) {
        val info = uids[notified]!!
        notified.pollEvents().forEach {
            val entry = info.dir.resolve(it.context() as Path)
            logger.debug { "File Event ${it.kind()} received for ${entry}" }

            when (it.kind()) {
                ENTRY_CREATE -> {
                    if (entry.toFile().isDirectory) {
                        this.watch(info.uid, entry)
                    } else {
                        this.arbiter.fileChanged(FileChangedEvent(info.uid, entry))
                    }
                }
                ENTRY_DELETE -> println("deleted: ${it.context()}")
                ENTRY_MODIFY -> println("modified: ${it.context()}")
            }
        }


    }

}