package nsync.analyzer

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.LocalFile
import nsync.NBus
import nsync.SyncFolder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

class DirScanner(private val record: SyncFolder): Runnable {
    private val logger = KotlinLogging.logger {}

    override fun run() {
        logger.debug { "Scanning $record " }
        val time = measureTimeMillis {
            Files.walk(Paths.get(record.pathLocal)).forEach({
                if (it.toFile().isFile) {
                    runBlocking {
                        val event = LocalFile(record.folderId, it)
                        try {
                            logger.info { "Scanner find file ${event.localFilePath}" }
                            NBus.publish(event)
                        } catch (err: Exception) {
                            logger.error(err) { "Error processing scanned file" }
                        }
                    }
                }
            })
        }
        logger.info { "Finished scan for $record ($time ms)"}
    }

    companion object {
        fun scan(record: SyncFolder) {
            val scanner = Thread(DirScanner(record))
            scanner.name = "Analyzer.scanner-worker:${record.folderId}"
            scanner.isDaemon = true
            scanner.start()
        }
    }
}