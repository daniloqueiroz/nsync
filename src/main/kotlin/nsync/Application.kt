package nsync.app

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import mu.KLogging
import nsync.Configuration
import nsync.FolderCatalog
import nsync.SyncFolder
import nsync.analyzer.DirAnalizer
import nsync.storage.LocalFileStorage
import nsync.storage.StorageBackend
import nsync.storage.StorageResolver
import nsync.synchronization.SyncArbiter

sealed class AppCommand {
    val outbox: Channel<SyncFolder> = Channel()
}
class StopCmd : AppCommand()
data class AddSyncFolderCmd(val localUri: String, val remoteUri: String) : AppCommand()


class Application(conf: Configuration, inbox: Channel<AppCommand>) {
    companion object : KLogging()

    private val name = "NSync"
    private val version = "1.0.0"
    private val catalog: FolderCatalog
    private val analyzer: DirAnalizer
    private val job: Job
    private val arbiter: SyncArbiter

    init {
        logger.info { "Starting ${name} version ${version} " }
        catalog = FolderCatalog(conf)
        arbiter = SyncArbiter(Configuration.directory.resolve("metadata"), StorageResolverImpl, catalog)
        analyzer = DirAnalizer(arbiter)

        job = launch(CommonPool) {
            analyzer.start()
            logger.info { "Loading existent folders" }
            for(folder in catalog) {
                registerFolder(folder)
            }

            logger.info { "Initializing command listener" }
            consume(inbox)
        }
    }

    private suspend fun consume(inbox: Channel<AppCommand>) {
        while (true) {
            logger.debug { "Application is waiting for commands" }
            val msg = inbox.receive()
            logger.debug { "Command ${msg} received" }
            try {
                process(msg)
            } catch (err: Exception) {
                logger.error(err) { "Error executing command ${msg}" }
            }
            yield()
        }
    }

    private suspend fun process(msg: AppCommand) {
        when (msg) {
            is StopCmd -> this.shutdown()
            is AddSyncFolderCmd -> this.addSyncDir(msg)
        }
    }

    private fun shutdown() {
        logger.info { "Stopping ${name}" }
        this.job.cancel()
    }

    private suspend fun addSyncDir(cmd: AddSyncFolderCmd) {
        // TODO check if already exists, etc...
        val record = this.catalog.register(cmd.localUri, cmd.remoteUri)

        this.registerFolder(record)
        cmd.outbox.send(record)
    }

    private suspend fun registerFolder(folder: SyncFolder) {
        this.arbiter?.dirAdded(folder)
        analyzer.analize(folder)
    }

    /**
     * Joins application coroutine and waits for it to finishes.
     * This method is blocking.
     */
    suspend fun join() {
        this.job.join()
    }
}

internal object StorageResolverImpl: StorageResolver {
    override fun getStorageBackend(folder: SyncFolder): StorageBackend {
        when (folder.schemeRemote) {
            "file" -> return LocalFileStorage(folder)
        }
        throw IllegalArgumentException("Not supported remote schema")
    }
}