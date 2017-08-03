package nsync

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import mu.KotlinLogging
import nsync.index.SynchronizationStatus
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

object NBus {
    private val logger = KotlinLogging.logger {}
    private val subscribers: MutableMap<KClass<*>, MutableList<Consumer>> = mutableMapOf()
    private val chn = Channel<NSyncEvent>(Channel.UNLIMITED)

    init {
        launch(CommonPool) {
            for (event in chn) {
                val evtType = event::class
                for (consumer in subscribers.getOrDefault(evtType, mutableListOf())) {
                    dispatch(evtType, consumer, event)
                }
                yield()
            }
        }
    }

    private suspend fun dispatch(evtType: KClass<out NSyncEvent>, consumer: Consumer, event: NSyncEvent) {
        try {
            logger.debug { "Dispatching ${evtType.java.simpleName} event to ${consumer::class.java.simpleName}" }
            consumer.onEvent(event)
        } catch (err: Exception) {
            logger.error(err) { "Error dispatching event " }
        }
    }

    fun register(consumer: Consumer, vararg evtTypes: KClass<*>) {
        for (type in evtTypes) {
            logger.info { "Registering consumer ${consumer::class.java.simpleName} to ${type.java.simpleName}" }
            val existent = subscribers.getOrDefault(type, mutableListOf())
            existent.add(consumer)
            subscribers[type] = existent
        }
    }

    suspend fun publish(event: NSyncEvent) {
        logger.debug { "Event $event published" }
        chn.send(event)
    }
}

interface Consumer {
    suspend fun onEvent(event: NSyncEvent)
}

sealed class NSyncEvent

data class SyncFolder(
        val folderId: String,
        val localFolder: String,
        val remoteFolder: String) : NSyncEvent() {

    val uriRemote: URI by lazy { URI(remoteFolder) }
    val schemeRemote: String by lazy { uriRemote.scheme }
    val pathRemote: String by lazy { uriRemote.path }

    val uriLocal: URI by lazy { URI(localFolder) }
    val schemeLocal: String by lazy { uriLocal.scheme }
    val pathLocal: String by lazy { uriLocal.path }

    fun fileRelativePath(localFile: Path): String = Paths.get(pathLocal).relativize(localFile).toString()
}

data class LocalFile(
        val folderId: String,
        val localFilePath: Path) : NSyncEvent()

data class SyncRequest(
        val syncId: String,
        val localFilePath: Path,
        val folder: SyncFolder
) : NSyncEvent()

data class SyncStatus(
        val syncId: String,
        val status: SynchronizationStatus
) : NSyncEvent()