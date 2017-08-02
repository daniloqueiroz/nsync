package nsync.cli

import picocli.CommandLine

@CommandLine.Command(description = arrayOf("Stops NSync daemon"))
internal class Stop : CliCommand {
    override fun execute(base: Main) {
        base.api.shutdown()
    }
}