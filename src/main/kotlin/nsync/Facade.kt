package nsync

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.metadata.MetadataServer
import java.net.URI
import kotlin.system.measureTimeMillis


const val name = "NSync"
const val version = "1.0.0-devel"


internal class Init(private val bus: SignalBus) {
    private val logger = KotlinLogging.logger {}
    operator fun invoke() = runBlocking {
        val running = measureTimeMillis {
            bus.join()
        }

        logger.info { "Application has stopped (running time: $running ms)" }
    }
}

class KernelFacade(
        private val bus: SignalBus,
        private val metadata: MetadataServer
) {
    fun start() =
            Init(bus)()

    suspend fun stop() =
            bus.publish(Stop())

    suspend fun addFS(localUri: String, remoteUri: String) =
            bus.publish(AddFS(FS(localFolder = URI(localUri), remoteFolder = URI(remoteUri))))
}

