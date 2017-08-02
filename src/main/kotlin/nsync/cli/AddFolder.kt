package nsync.cli

import picocli.CommandLine

@CommandLine.Command(description = arrayOf("Add a new Folder to be sync"))
internal class AddFolder : CliCommand {
    @CommandLine.Parameters(index = "0", description = arrayOf("Local folder URI. eg.: 'file:///tmp'"))
    var localUri: String? = null

    @CommandLine.Parameters(index = "1", description = arrayOf("Local folder URI. eg.: 'file:///tmp'"))
    var remoteUri: String? = null

    override fun execute(base: Main) {
        base.api.addFolder(this.localUri!!, this.remoteUri!!)
    }
}
