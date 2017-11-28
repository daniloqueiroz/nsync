package nsync.ui.cli

import nsync.ui.rest.Client
import nsync.kernel.KernelFacade
import picocli.CommandLine
import kotlin.system.exitProcess

internal interface CliCommand {
    operator fun invoke(ctx: BaseCommand)
}


@CommandLine.Command(
        name = KernelFacade.name,
        version = arrayOf("@|bold ${KernelFacade.name}|@ version @|bold ${KernelFacade.version}|@"),
        subcommands = arrayOf(
                Server::class,
                Status::class,
                Stop::class,
                AddFolder::class
        )
)
class BaseCommand(private val args: Array<String>) : CliCommand {
    @CommandLine.Option(
            names = arrayOf("--help"),
            help = true,
            description = arrayOf("show help message")
    )
    var helpRequested: Boolean = false

    @CommandLine.Option(
            names = arrayOf("--version"),
            versionHelp = true,
            description = arrayOf("show this version info"))
    var versionRequested = false

    @CommandLine.Option(
            names = arrayOf("-p", "--port"),
            help = true,
            description = arrayOf("daemon port")
    )
    var port: Int = 1982

    @CommandLine.Option(
            names = arrayOf("-l", "--log"),
            help = true,
            description = arrayOf("Log level")
    )
    var logLevel: String = "info"

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


