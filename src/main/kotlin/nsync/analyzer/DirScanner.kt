package nsync.analyzer

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.FileModified
import nsync.LocalFile
import nsync.SignalBus
import nsync.SyncFolder
import java.io.File
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

class DirScanner(private val bus: SignalBus) : Runnable {
    private val logger = KotlinLogging.logger {}
    private val chn = Channel<SyncFolder>()

    init {
        val scanner = Thread(this)
        scanner.name = "Analyzer.scanner-worker"
        scanner.isDaemon = true
        scanner.start()
    }

    override fun run() {
        runBlocking {
            chn.consumeEach {
                logger.debug { "Scanning $it " }
                val time = measureTimeMillis {
                    walk(it.folderId, Paths.get(it.pathLocal).toFile())
                }
                logger.info { "Finished scan for $it ($time ms)" }
            }
        }

    }

    private suspend fun walk(folderId: String, dir: File) {
        for (child in dir.listFiles()) {
            if (child.isFile) {
                val event = LocalFile(folderId, child.toPath())
                try {
                    logger.info { "Scanner find file ${event.localFilePath}" }
                    bus.publish(FileModified(event))
                } catch (err: Exception) {
                    logger.error(err) { "Error processing scanned file" }
                }
            } else {
                this.walk(folderId, child)
            }
        }
    }

    internal suspend fun scan(record: SyncFolder) {
        chn.send(record)
    }
}