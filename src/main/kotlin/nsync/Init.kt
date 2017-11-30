package nsync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.kernel.analyzer.DirAnalyzer
import nsync.kernel.bus.AsyncBus
import nsync.kernel.bus.SignalBus
import nsync.kernel.storage.StorageManager
import nsync.kernel.synchronization.SyncArbiter
import nsync.ui.cli.BaseCommand
import nsync.ui.rest.WebServer
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.system.measureTimeMillis

/**
 * This class is responsible by bootstrapping the ui.
 */
class Init(
        private val verbose: Boolean,
        private val logLevel: String,
        private val port: Int
) {
    companion object : KLogging()

    fun boot() = runBlocking<Unit> {
        try {
            configureLog(Configuration.directory)
            logger.info { "Bootstrapping kernel" }
            val bus: SignalBus = loadKernel()
            logger.info { "Initializing REST WebServer" }
            val app = initApp()

            val running = measureTimeMillis {
                bus.let {
                    it.start()
                    it.join()
                }

                app.stop()
                bus.stop()
            }

            logger.info { "Application has stopped (running time: $running ms)" }
        } catch (err: Exception) {
            logger.error(err) { "! Panic: Unable to load Application" }
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

    private fun loadKernel(): SignalBus {
        logger.info { "Initializing SignalBus system" }
        val bus = AsyncBus()
        bus.start()

        logger.info { "Initializing kernel servers" }
        DirAnalyzer(bus)
        SyncArbiter(bus)
        val storage = StorageManager(bus)

        logger.info { "Loading storage drivers" }
        storage.loadDrivers()

        return bus
    }

    private fun initApp(): WebServer {
        val api = WebServer(port)
        api.start()
        return api
    }
}


fun main(args: Array<String>) {
    val interpreter = BaseCommand(args)
    interpreter()
}