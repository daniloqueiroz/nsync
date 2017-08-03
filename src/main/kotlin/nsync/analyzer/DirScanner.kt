package nsync.analyzer

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.FileChangedEvent
import nsync.SyncFolder
import nsync.synchronization.SyncArbiter
import java.nio.file.Files
import java.nio.file.Paths

class DirScanner(private val arbiter: SyncArbiter) {
    private companion object : KLogging()

    private val chn = Channel<FileChangedEvent>()

    fun start() {
        async(CommonPool) {
            do {
                try {
                    val event = chn.receive()
                    logger.debug {"Scanner find file ${event.localFilePath} - sending to arbiter"}
                    arbiter.fileChanged(event)
                } catch (err: Exception) {
                    logger.error(err) { "Error processing scanned file" }
                }
            } while (true)
        }
    }

    suspend fun scan(record: SyncFolder) {
        val scanner = Thread({
            logger.info { "Scanning $record " }
            Files.walk(Paths.get(record.pathLocal)).forEach({
                if (it.toFile().isFile) {
                    runBlocking {
                        chn.send(FileChangedEvent(record.uid, it))
                    }
                }
            })
        })
        scanner.name = "DirScanner:${record.uid}"
        scanner.isDaemon = true
        scanner.start()
    }
}