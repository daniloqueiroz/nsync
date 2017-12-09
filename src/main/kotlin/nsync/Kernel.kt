package nsync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import commons.*
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import mu.KotlinLogging
import nsync.analyzer.LocalFileWatchServer
import nsync.metadata.MetadataServer
import nsync.storage.StorageServer
import nsync.synchronization.SyncServer
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Path
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

/**
 * This class is responsible by bootstrapping the app.
 */
class Loader(
        private val verbose: Boolean,
        private val logLevel: String
) {
    companion object : KLogging()

    fun boot(): KernelFacade {
        try {
            configureLog(Configuration.directory)
            logger.info { "Bootstrapping kernel" }
            logger.info { "Initializing SignalBus system" }
            val bus = AsyncBus()

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

    private fun configureLog(confDir: Path) {
        val level: Level? = Level.valueOf(logLevel.toUpperCase())
        val logCtx = LoggerFactory.getILoggerFactory()
        val log = logCtx.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        log.detachAndStopAllAppenders()
        log.isAdditive = false
        log.level = level ?: Level.ALL

        val layout = PatternLayout()
        layout.pattern = "%date{yyyy-MM-dd'T'HH:mm:ss.SSS'Z', UTC} | %level | %thread | %logger{15} | %msg %ex{3} %n"
        layout.context = logCtx as Context
        layout.start()

        if (this.verbose) {
            val logConsoleAppender = ConsoleAppender<ILoggingEvent>()
            logConsoleAppender.setLayout(layout)
            logConsoleAppender.context = logCtx
            logConsoleAppender.name = "console"
            logConsoleAppender.start()
            log.addAppender(logConsoleAppender)
        } else {
            val fileConsoleAppender = FileAppender<ILoggingEvent>()
            fileConsoleAppender.setLayout(layout)
            fileConsoleAppender.isAppend = false
            fileConsoleAppender.context = logCtx
            fileConsoleAppender.name = "file"
            fileConsoleAppender.file = confDir.resolve("log").toString()
            fileConsoleAppender.start()
            log.addAppender(fileConsoleAppender)
        }
    }
}