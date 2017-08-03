package nsync.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.BinaryFileConfigurationStorage
import nsync.Configuration
import nsync.FolderCatalog
import nsync.analyzer.DirAnalyzer
import nsync.cli.rest.WebServer
import nsync.synchronization.SyncArbiter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.system.measureTimeMillis

/**
 * This class is responsible by bootstrapping the application.
 *
 * It loads configuration, setups log, creates inbox channel,
 * creates and starts rest rest and creates [Application]
 */
class Loader(
        private val logLevel: String,
        private val port: Int
) {
    companion object : KLogging()

    fun boot() = runBlocking<Unit> {
        try {
            var app: Application? = null
            var api: WebServer? = null
            val bootTime = measureTimeMillis {
                logger.info { "Using Configuration from ${Configuration.directory}" }
                val conf = Configuration(BinaryFileConfigurationStorage(
                        Configuration.directory.resolve("config").toFile()))
                configureLog(Configuration.directory)

                logger.info { "Bootstraping application" }
                val inbox = Channel<AppCommand>(10)

                val catalog = FolderCatalog(conf)
                DirAnalyzer()
                SyncArbiter(Configuration.directory.resolve("metadata"), StorageResolverImpl, catalog)
                app = Application(catalog, inbox)

                logger.info { "Initializing REST WebServer" }
                api = WebServer(port, inbox)
                api?.start()
            }

            logger.info { "Loading completed after $bootTime ms" }
            app?.start()
            api?.stop()
        } catch (err: Exception) {
            logger.error(err) { "Panic: Unable to load Application"}
        }
    }

    private fun configureLog(confDir: Path) {
        val level: Level? = Level.valueOf(logLevel.toUpperCase())
        val logCtx = LoggerFactory.getILoggerFactory()
        val log = logCtx.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        log.detachAndStopAllAppenders()
        log.setAdditive(false)
        log.setLevel(level ?: Level.ALL)

        val layout = PatternLayout()
        layout.setPattern("%date{yyyy-MM-dd'T'HH:mm:ss.SSS'Z', UTC} | %level | %thread | %logger{15} | %msg %ex{3} %n")
        layout.setContext(logCtx as Context)
        layout.start()

        // TODO choose appender based on a cli flag
        val logConsoleAppender = ConsoleAppender<ILoggingEvent>()
        logConsoleAppender.setLayout(layout)
        logConsoleAppender.context = logCtx
        logConsoleAppender.name = "console"
        logConsoleAppender.start()
        log.addAppender(logConsoleAppender)

        val fileConsoleAppender = FileAppender<ILoggingEvent>()
        fileConsoleAppender.setLayout(layout)
        fileConsoleAppender.setAppend(false)
        fileConsoleAppender.context = logCtx
        fileConsoleAppender.name = "file"
        fileConsoleAppender.file = confDir.resolve("log").toString()
        fileConsoleAppender.start()
        log.addAppender(fileConsoleAppender)
    }
}