package nsync.app

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import mu.KLogging
import nsync.FolderCatalog
import nsync.NBus
import nsync.SyncFolder
import nsync.storage.LocalFileStorage
import nsync.storage.StorageBackend
import nsync.storage.StorageResolver

sealed class AppCommand {
    val outbox: Channel<SyncFolder> = Channel()
}

class StopCmd : AppCommand()
data class AddSyncFolderCmd(val localUri: String, val remoteUri: String) : AppCommand()


class Application(private val catalog: FolderCatalog, private val inbox: Channel<AppCommand>) {
    companion object : KLogging()

    private val name = "NSync"
    private val version = "1.0.0"
    private var job: Job? = null

    /**
     * Starts the application, in other words makes the application
     * starts processing commands.
     *
     * This command blocks until the application stops
     */
    suspend fun start() {
        logger.info { "Starting ${name} version ${version} " }

        this.job = launch(CommonPool) {
            logger.info { "Loading existent folders" }
            for (folder in catalog) {
                registerFolder(folder)
            }

            logger.info { "Initializing command listener" }
            consume(inbox)
        }
        this.job?.join()
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
        this.job?.cancel()
    }

    private suspend fun addSyncDir(cmd: AddSyncFolderCmd) {
        // TODO check if already exists, etc...
        val record = this.catalog.register(cmd.localUri, cmd.remoteUri)

        this.registerFolder(record)
        cmd.outbox.send(record)
    }

    private suspend fun registerFolder(folder: SyncFolder) {
        NBus.publish(folder)
    }
}

internal object StorageResolverImpl : StorageResolver {
    override fun getStorageBackend(folder: SyncFolder): StorageBackend {
        when (folder.schemeRemote) {
            "file" -> return LocalFileStorage(folder)
        }
        throw IllegalArgumentException("Not supported remote schema")
    }
}