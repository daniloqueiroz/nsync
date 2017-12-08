package nsync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import mu.KLogging
import nsync.analyzer.LocalFileWatchServer
import nsync.metadata.MetadataServer
import utils.AsyncBus
import nsync.storage.StorageServer
import nsync.synchronization.SyncServer
import org.slf4j.LoggerFactory
import utils.Configuration
import java.nio.file.Path

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
            bus.start()

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