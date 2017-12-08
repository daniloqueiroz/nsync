package nsync.metadata

import mu.KLogging
import nsync.*
import utils.AsyncFile
import java.nio.file.Path


class MetadataServer(bus: SignalBus, metadataDirectory: Path) : Server(bus, listOf(AddFS::class)) {
    private companion object : KLogging()

    private val tab: Fstab

    init {
        val file = metadataDirectory.resolve(FSTAB_FILENAME)
        val fstabFile = AsyncFile(file, Fsentry)
        tab = Fstab(fstabFile)
    }

    suspend override fun handle(msg: Signal<*>) {
        when (msg) {
            is AddFS -> this.addFS(msg.payload)
        }
    }

    suspend private fun addFS(fsInfo: FS) {
        try {
            val entry = tab.addFS(fsInfo.localFolder, fsInfo.remoteFolder)
            logger.info { "New fs added: ${entry.uri.toString()}" }
            val nuFs = FS(
                    localFolder = entry.localUri,
                    remoteFolder = entry.remoteUri,
                    identifier = entry.uri
            )
            this.publish(FSAdded(nuFs))
        } catch (err: IllegalArgumentException) {
            logger.error(err) { "Unable to add fs." }
        }
    }

}

