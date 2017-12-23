package ui.cli

import commons.Failure
import commons.Success
import picocli.CommandLine
import ui.rest.FSBody


@CommandLine.Command(description = arrayOf("Add a new FS to sync"), name = "add")
internal class AddFS : CliCommand {
    @CommandLine.Parameters(index = "0", description = arrayOf("Local folder URI. eg.: 'file:///tmp'"))
    var localUri: String? = null

    @CommandLine.Parameters(index = "1", description = arrayOf("Remote folder URI. eg.: 'file:///tmp'"))
    var remoteUri: String? = null

    override fun invoke(ctx: BaseCommand) {
        ctx.client(ctx.api.addFS(FSBody(localUri = localUri!!, remoteUri = remoteUri!!))).then {
            when (it) {
                is Success -> {
                    ctx.exit("FS added ${it.value}")
                }
                is Failure -> {
                    ctx.exit("Unable to add fs: ${it.message}", 1)
                }
            }
        }
    }
}

@CommandLine.Command(description = arrayOf("List existing FS"), name = "list")
internal class ListFS : CliCommand {
    override fun invoke(ctx: BaseCommand) {
        ctx.client(ctx.api.listFS()).then {
            when (it) {
                is Success -> {
                    val filesystems = it.value
                    if (filesystems.size == 0) {
                        println("No Filesystems added.")
                    } else {
                        println("Existing Filesystems:")
                        filesystems.forEach {
                            println("\t$it")
                        }
                    }
                    ctx.exit()
                }
                is Failure -> {
                    ctx.exit("Unable to list fs: ${it.message}", 1)
                }
            }
        }
    }
}