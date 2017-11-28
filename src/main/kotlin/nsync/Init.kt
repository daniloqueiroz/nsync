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
import nsync.kernel.FolderCatalog
import nsync.kernel.analyzer.DirAnalyzer
import nsync.kernel.bus.AsyncBus
import nsync.kernel.bus.SignalBus
import nsync.ui.rest.WebServer
import nsync.kernel.storage.StorageManager
import nsync.kernel.synchronization.SyncArbiter
import nsync.ui.cli.BaseCommand
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
            var api: WebServer? = null
            var bus: SignalBus? = null

            val time = measureTimeMillis {
                logger.info { "Loading configuration from ${Configuration.directory}" }
                val conf = Configuration(BinaryFileConfigurationStorage(
                        Configuration.directory.resolve("config").toFile()))
                configureLog(Configuration.directory)

                logger.info { "Bootstrapping kernel" }

                logger.info { "Initializing SignalBus system" }
                bus = AsyncBus()
                bus!!.start()

                logger.info { "Initializing kernel systems" }
                val catalog = FolderCatalog(conf, bus!!)
                DirAnalyzer(bus!!)
                SyncArbiter(Configuration.directory.resolve("metadata"), catalog, bus!!)

                logger.info { "Loading storage drivers" }
                StorageManager(bus!!).loadDrivers()

                logger.info { "Initializing REST WebServer" }
                api = WebServer(port)
                api?.start()
            }

            logger.info { "Bootstrap complete (booting time: $time ms)" }
            val running = measureTimeMillis {
                bus?.let{
                    it.start()
                    it.join()
                }
            }

            logger.info { "Application has stopped (running time: $running ms)" }
            bus?.stop()
            api?.stop()
        } catch (err: Exception) {
            logger.error(err) { "! Panic: Unable to load Application"}
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


fun main(args: Array<String>) {
    val interpreter = BaseCommand(args)
    interpreter()
}