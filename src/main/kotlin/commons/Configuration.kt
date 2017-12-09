package commons

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Configuration {

    val directory: Path = Paths.get(System.getProperty("user.home"), ".config", "nsync")

    init {
        if (!Files.exists(directory)) {
            Files.createDirectory(directory)
        }
    }
}


fun configureLog(confDir: Path, verbose: Boolean, logLevel: String) {
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

    if (verbose) {
        println("Log configured as verbose and level $logLevel")
        val logConsoleAppender = ConsoleAppender<ILoggingEvent>()
        logConsoleAppender.setLayout(layout)
        logConsoleAppender.context = logCtx
        logConsoleAppender.name = "console"
        logConsoleAppender.start()
        log.addAppender(logConsoleAppender)
    } else {
        val logFile = confDir.resolve("log").toString()
        println("Log configured as to file $logFile and level $logLevel")
        val fileConsoleAppender = FileAppender<ILoggingEvent>()
        fileConsoleAppender.setLayout(layout)
        fileConsoleAppender.isAppend = false
        fileConsoleAppender.context = logCtx
        fileConsoleAppender.name = "file"
        fileConsoleAppender.file = logFile
        fileConsoleAppender.start()
        log.addAppender(fileConsoleAppender)
    }
}