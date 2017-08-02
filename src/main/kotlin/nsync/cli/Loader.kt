package nsync.app

import kotlinx.coroutines.experimental.runBlocking
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import mu.KLogging
import nsync.BinaryFileConfigurationStorage
import nsync.Configuration
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlinx.coroutines.experimental.channels.Channel
import nsync.cli.rest.WebServer

/**
 * This class is responsible by bootstrapping the application.
 *
 * It loads configuration, setups log, creates inbox channel,
 * creates and starts rest rest and creates [Application]
 */
class Loader(
        private val logLevel: String,
        port: Int
) {
    companion object : KLogging()

    private val conf = Configuration(BinaryFileConfigurationStorage(
            Configuration.directory.resolve("config").toFile()))
    private val inbox = Channel<AppCommand>(10)
    private val api = WebServer(port, inbox)

    fun boot() = runBlocking<Unit> {
        logger.info { "Using Configuration from ${Configuration.directory}" }
        configureLog(Configuration.directory)

        logger.info { "Initializing REST WebServer" }
        api.start()

        logger.info { "Bootstraping application" }
        Application(conf, inbox).join()
        api.stop()
    }

    private fun configureLog(confDir: Path) {
        val level: Level? = Level.valueOf(logLevel.toUpperCase())
        val logCtx = LoggerFactory.getILoggerFactory()
        val log = logCtx.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        log.detachAndStopAllAppenders()
        log.setAdditive(false)
        log.setLevel(level ?: Level.ALL)

        val layout = PatternLayout()
        layout.setPattern("%date{yyyy-MM-dd'T'HH:mm:ss.SSS'Z', UTC} %level [%thread] %logger{15} %msg %ex{3} %n")
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