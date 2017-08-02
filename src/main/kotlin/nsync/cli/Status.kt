package nsync.cli

import picocli.CommandLine

@CommandLine.Command(description = arrayOf("Checks if NSync is running"))
internal class Status : CliCommand {
    override fun execute(base: Main) {
        base.api.status()
    }
}
