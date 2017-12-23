package interactors

import nsync.*
import nsync.metadata.FS
import java.net.URI

class ListFsCommand(private val kernel: KernelFacade) {
    suspend operator fun invoke(): Sequence<FS> {
        return kernel.filesystems()
    }
}