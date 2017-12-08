package ui.cli

import nsync.Loader
import picocli.CommandLine
import ui.rest.FSBody
import ui.rest.WebServer
import utils.Failure
import utils.Success


@CommandLine.Command(description = arrayOf("Starts daemon"), name = "server")
internal class Server : CliCommand {
    @CommandLine.Option(
            names = arrayOf("-v"),
            help = true,
            description = arrayOf("If verbose is enabled, it logs to STD instead of file"))
    var verbose: Boolean = false

    override fun invoke(ctx: BaseCommand) {
        val loader = Loader(verbose, ctx.logLevel);
        val kernel = loader.boot()
        WebServer(ctx.port, kernel).start()
        kernel.start()
    }
}

@CommandLine.Command(description = arrayOf("Stops daemon"), name = "stop")
internal class Stop : CliCommand {
    override fun invoke(ctx: BaseCommand) {
        ctx.client(ctx.api.shutdown()).then {
            when (it) {
                is Success -> {
                    ctx.exit("App stopped. Uptime: ${it.value.uptimeMins} minutes")
                }
                is Failure -> {
                    ctx.exit("Unable to stop ui: ${it.message}", 1)
                }
            }
        }
    }
}

@CommandLine.Command(description = arrayOf("Checks if server is running"), name = "status")
internal class Status : CliCommand {
    override fun invoke(ctx: BaseCommand) {

        ctx.client(ctx.api.status()).then {
            when (it) {
                is Success -> {
                    ctx.exit("Server is running. Uptime: ${it.value.uptimeMins} minutes")
                }
                is Failure -> {
                    ctx.exit("Unable to retrieve ui result: ${it.message}", 1)
                }
            }
        }
    }
}


@CommandLine.Command(description = arrayOf("Add a new Folder to be synced"), name = "addfs")
internal class AddFS : CliCommand {
    @CommandLine.Parameters(index = "0", description = arrayOf("Local folder URI. eg.: 'file:///tmp'"))
    var localUri: String? = null

    @CommandLine.Parameters(index = "1", description = arrayOf("Local folder URI. eg.: 'file:///tmp'"))
    var remoteUri: String? = null

    override fun invoke(ctx: BaseCommand) {
        ctx.client(ctx.api.addFS(FSBody(localUri = localUri!!, remoteUri = remoteUri!!))).then {
            when (it) {
                is Success -> {
                    ctx.exit("FSAdd request accepted")
                }
                is Failure -> {
                    it.error.printStackTrace()
                    ctx.exit("Unable to retrieve send FSAdd request: ${it.message}", 1)
                }
            }
        }
    }
}