package nsync

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import nsync.metadata.MetadataServer
import nsync.signals.AddFS
import nsync.signals.FS
import nsync.signals.SignalBus
import java.net.URI
import kotlin.system.measureTimeMillis


const val name = "NSync"
const val version = "1.0.0-devel"


internal class Init(private val bus: SignalBus) {
    private val logger = KotlinLogging.logger {}
    operator fun invoke() = runBlocking<Unit> {
        val running = measureTimeMillis {
            bus.let {
                it.start()
                it.join()
            }

            bus.stop()
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
            bus.stop()

    suspend fun addFS(localUri: String, remoteUri: String) =
            bus.publish(::AddFS, FS(localFolder = URI(localUri), remoteFolder = URI(remoteUri)))
}

