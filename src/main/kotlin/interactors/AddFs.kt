package interactors

import nsync.*
import java.net.URI

class AddFsCommand(private val kernel: KernelFacade) {
    suspend operator fun invoke(localUri: String, remoteUri: String): URI {
        return kernel.connect(FSAdded::class).use {
            it.send(AddFS(FS(URI(localUri), URI(remoteUri))))
            it.receive().payload.identifier!!
        }
    }
}