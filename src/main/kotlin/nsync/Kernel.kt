package nsync

import commons.*
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import mu.KotlinLogging
import nsync.analyzer.LocalFileWatchServer
import nsync.metadata.MetadataServer
import nsync.storage.StorageServer
import nsync.synchronization.SyncServer
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
        logger.info { "$name kernel up and running" }
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
}

/**
 * This class is responsible by bootstrapping the app.
 */
class Loader {
    companion object : KLogging()

    fun boot(): KernelFacade {
        try {

            logger.info { "Bootstrapping kernel" }
            logger.info { "Initializing SignalBus system" }
            val bus: SignalBus = AsyncBus()

            logger.info { "Initializing kernel servers" }
            LocalFileWatchServer(bus)
            SyncServer(bus)
            val metadata = MetadataServer(bus, Configuration.directory)
            val storage = StorageServer(bus)

            logger.info { "Loading storage drivers" }
            storage.loadDrivers()

            return KernelFacade(bus, metadata)
        } catch (err: Exception) {
            logger.error(err) { "! Panic: Unable to load Application" }
            throw err
        }
    }
}