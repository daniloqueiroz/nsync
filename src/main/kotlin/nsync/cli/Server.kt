package nsync.cli

import nsync.app.Loader
import picocli.CommandLine

@CommandLine.Command(description = arrayOf("Starts NSync daemon"))
internal class Server : CliCommand {
    @CommandLine.Option(names = arrayOf("-v"), help = true,
            description = arrayOf("If verbose is enabled, it logs to STD instead of file"))
    var verbose: Boolean = false

    override fun execute(base: Main) {
        Loader(verbose, base.logLevel, base.port).boot()
    }
}