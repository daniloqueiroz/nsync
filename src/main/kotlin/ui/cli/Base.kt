package ui.cli

import commons.Configuration
import commons.configureLog
import nsync.name
import nsync.version
import picocli.CommandLine
import ui.rest.Client
import kotlin.system.exitProcess

internal interface CliCommand {
    operator fun invoke(ctx: BaseCommand)
}


@CommandLine.Command(
        name = name,
        version = arrayOf("@|bold ${name}|@ version @|bold ${version}|@"),
        subcommands = arrayOf(
                Server::class,
                Status::class,
                Stop::class,
                AddFS::class
        )
)
class BaseCommand(private val args: Array<String>) : CliCommand {
    @CommandLine.Option(
            names = arrayOf("--help"),
            help = true,
            description = arrayOf("show help message")
    )
    private var helpRequested: Boolean = false

    @CommandLine.Option(
            names = arrayOf("--version"),
            versionHelp = true,
            description = arrayOf("show this version info")
    )
    private var versionRequested = false

    @CommandLine.Option(
            names = arrayOf("-v", "--verbose"),
            help = true,
            description = arrayOf("if verbose is enabled,  it logs to STD instead of file")
    )
    var verbose: Boolean = false

    @CommandLine.Option(
            names = arrayOf("-l", "--level"),
            help = true,
            description = arrayOf("Log level")
    )
    var logLevel: String = "info"

    @CommandLine.Option(
            names = arrayOf("-p", "--port"),
            help = true,
            description = arrayOf("daemon port")
    )
    var port: Int = 1982

    val client by lazy { Client(port) }
    val api by lazy { client.api }

    fun exit(message: String, code: Int = 0) {
        print(message)
        exitProcess(code)
    }

    operator fun invoke() {
        this.invoke(this)
    }

    override fun invoke(ctx: BaseCommand) {
        val interpreter = CommandLine(BaseCommand(args))
        val parsed = interpreter.parse(*args)
        val base = parsed[0].getCommand<BaseCommand>()

        if (base.versionRequested) {
            interpreter.printVersionHelp(System.err)
        } else if (base.helpRequested or (parsed.size < 2)) {
            CommandLine.usage(base, System.err)
        } else {
            val cmd = parsed[1].getCommand<CliCommand>()
            cmd(base)
        }
    }
}

fun main(args: Array<String>) {
    val interpreter = BaseCommand(args)
    interpreter()
}