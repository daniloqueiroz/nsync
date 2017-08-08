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
import nsync.extensions.forEach


sealed class Result<T>
data class Failure<T>(val error: Exception) : Result<T>() {
    val message: String? = error.message
}
data class Success<T>(val result: T) : Result<T>()


sealed class AppCommand<Result> {
    private val outbox: Channel<Result> = Channel()
    suspend fun <R> onResult(block: suspend (Result) -> R): R {
        val result = outbox.receive()
        return block(result)
    }

    internal suspend fun send(result: Result): Unit = outbox.send(result)
}

class StopCmd : AppCommand<Unit>()
data class AddSyncFolderCmd(val localUri: String, val remoteUri: String) : AppCommand<Result<SyncFolder>>()


class Application(private val catalog: FolderCatalog, private val inbox: Channel<AppCommand<*>>) {
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
        logger.info { "Starting $name version $version " }

        this.job = launch(CommonPool) {
            logger.info { "Loading existent folders" }
            catalog.forEach {
                registerFolder(it)
            }

            logger.info { "Initializing command listener" }
            consume(inbox)
        }
        this.job?.join()
    }

    private suspend fun consume(inbox: Channel<AppCommand<*>>) {
        inbox.forEach {
            logger.debug { "Command $it received" }
            try {
                process(it)
            } catch (err: Exception) {
                logger.error(err) { "Error executing command $it" }
            }
            yield()
        }
    }

    private suspend fun process(msg: AppCommand<*>) {
        when (msg) {
            is StopCmd -> this.shutdown()
            is AddSyncFolderCmd -> this.addSyncDir(msg)
        }
    }

    private fun shutdown() {
        logger.info { "Stopping $name" }
        this.job?.cancel()
    }

    private suspend fun addSyncDir(cmd: AddSyncFolderCmd) {
        try {
            val record = this.catalog.register(cmd.localUri, cmd.remoteUri)
            this.registerFolder(record)
            cmd.send(Success(record))
        } catch (err: Exception) {
            logger.error(err) { "Error adding folder" }
            cmd.send(Failure(err))
        }
    }

    private suspend fun registerFolder(folder: SyncFolder) {
        NBus.publish(folder)
    }
}
