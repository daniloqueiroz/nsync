package nsync.cli

import nsync.app.Loader
import picocli.CommandLine

@CommandLine.Command(description = arrayOf("Starts NSync daemon"))
internal class Server : CliCommand {
    override fun execute(base: Main) {
        Loader(base.logLevel, base.port).boot()
    }
}