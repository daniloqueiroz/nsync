package nsync.analyzer

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.LocalFile
import nsync.NBus
import nsync.SyncFolder
import java.nio.file.Files
import java.nio.file.Paths

class DirScanner(private val record: SyncFolder): Runnable {
    private val logger = KotlinLogging.logger {}

    override fun run() {
        logger.info { "Scanning $record " }
        Files.walk(Paths.get(record.pathLocal)).forEach({
            if (it.toFile().isFile) {
                runBlocking {
                    val event = LocalFile(record.folderId, it)
                    try {
                        logger.info {"Scanner find file ${event.localFilePath}"}
                        NBus.publish(event)
                    } catch (err: Exception) {
                        logger.error(err) { "Error processing scanned file" }
                    }
                }
            }
        })
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