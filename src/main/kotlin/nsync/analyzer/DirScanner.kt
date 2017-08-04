package nsync.analyzer

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.LocalFile
import nsync.NBus
import nsync.SyncFolder
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class DirScanner : Runnable {
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
            for (record in chn) {
                logger.debug { "Scanning $record " }
                val time = measureTimeMillis {
                    walk(record.folderId, Paths.get(record.pathLocal).toFile())
                }
                logger.info { "Finished scan for $record ($time ms)" }
            }
        }

    }

    private suspend fun walk(folderId: String, dir: File) {
        for (child in dir.listFiles()) {
            if (child.isFile) {
                val event = LocalFile(folderId, child.toPath())
                try {
                    logger.info { "Scanner find file ${event.localFilePath}" }
                    NBus.publish(event)
                } catch (err: Exception) {
                    logger.error(err) { "Error processing scanned file" }
                }
            } else {
                this.walk(folderId, child)
            }
        }
    }

    suspend fun scan(record: SyncFolder) {
        chn.send(record)
    }
}