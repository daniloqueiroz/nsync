package nsync

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

object NBus {
    private val logger = KotlinLogging.logger {}
    private val subscribers: MutableMap<KClass<*>, MutableList<Consumer<NSyncEvent>>> = mutableMapOf()
    private val chn = Channel<NSyncEvent>()

    init {
        launch(CommonPool) {
            do {
                val event = chn.receive()
                val evtType = event::class
                for (consumer in subscribers.getOrDefault(evtType, mutableListOf())) {
                    logger.debug { "Dispatching ${evtType.java.simpleName} event to ${consumer::class.java.simpleName}" }
                    consumer.onEvent(event)

                }
                yield()
            } while (true)
        }
    }

    fun register(consumer: Consumer<NSyncEvent>, vararg evtTypes: KClass<*>) {
        for (type in evtTypes) {
            logger.debug { "Registering consumer ${consumer::class.java.simpleName} to ${type.java.simpleName}" }
            val existent = subscribers.getOrDefault(type, mutableListOf())
            existent.add(consumer)
            subscribers[type] = existent
        }
    }

    suspend fun publish(event: NSyncEvent) {
        chn.send(event)
    }
}

interface Consumer<in T> {
    suspend fun onEvent(event: T)
}

sealed class NSyncEvent

data class SyncFolder(
        val uid: String,
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

data class FileChangedEvent(
        val uid: String,
        val localFilePath: Path) : NSyncEvent()
