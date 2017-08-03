package nsync.cli

import nsync.cli.rest.Client
import picocli.CommandLine

internal interface CliCommand {
    fun execute(base: Main)
}

@CommandLine.Command(name = "nsync")
class Main {
    @CommandLine.Option(names = arrayOf("-h", "--help"), help = true,
            description = arrayOf("display this help message"))
    var helpRequested: Boolean = false

    @CommandLine.Option(names = arrayOf("-p", "--port"), help = true,
            description = arrayOf("NSync daemon port"))
    var port: Int = 1982

    @CommandLine.Option(names = arrayOf("-l", "--log"), help = true,
            description = arrayOf("Log level"))
    var logLevel: String = "info"

    val api by lazy { Client(port) }
}

fun main(args: Array<String>) {
    val commandLine = CommandLine(Main())
            .addSubcommand("server", Server())
            .addSubcommand("status", Status())
            .addSubcommand("stop", Stop())
            .addSubcommand("addFolder", AddFolder())

    try {
        val parsed = commandLine.parse(*args)
        processCmd(parsed)
    } catch (err: Exception) {
        CommandLine.usage(commandLine, System.err)
        err.printStackTrace()  // TODO remove it
    }
}

private fun processCmd(parsed: MutableList<CommandLine>) {
    val base: Main = parsed[0].command as Main
    if (base.helpRequested or (parsed.size < 2)) {
        throw IllegalArgumentException()
    }

    val cmd = parsed[1].command as CliCommand
    cmd.execute(base)
}