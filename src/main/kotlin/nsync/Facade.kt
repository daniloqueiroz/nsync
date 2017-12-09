package nsync

import commons.Connection
import commons.Signal
import commons.SignalBus
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.metadata.MetadataServer
import java.net.URI
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis


const val name = "NSync"
const val version = "1.0.0-devel"

class KernelFacade(
        private val bus: SignalBus,
        private val metadata: MetadataServer
) {
    private val logger = KotlinLogging.logger {}

    fun start() = runBlocking {
        logger.info { "Starting $name kernel" }
        val running = measureTimeMillis {
            bus.join()
        }

        logger.info { "$name kernel has stopped (running time: $running ms)" }
    }

    suspend fun stop() =
            bus.publish(Stop())

    fun <E, T : Signal<E>> connect(eventKlass: KClass<T>): Connection<E, T> {
        return this.bus.connect(eventKlass)
    }

    // TODO move it to iteractors
    suspend fun addFS(localUri: String, remoteUri: String) =
            bus.publish(AddFS(FS(localFolder = URI(localUri), remoteFolder = URI(remoteUri))))

}

