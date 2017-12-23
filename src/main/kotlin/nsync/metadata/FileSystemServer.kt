package nsync.metadata

import commons.AsyncFile
import commons.Server
import commons.Signal
import commons.SignalBus
import mu.KLogging
import nsync.AddFS
import nsync.FSAdded
import java.nio.file.Path


class FileSystemServer(
        bus: SignalBus,
        metadataDirectory: Path
) : Server(bus, listOf(AddFS::class)), Metadata {

    private companion object : KLogging()

    private val tab: Fstab

    init {
        val file = metadataDirectory.resolve(FSTAB_FILENAME)
        val fstabFile = AsyncFile(file, FSEntry)
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

    override fun filesystems(): Sequence<FS> {
        return this.tab.map{ FS(identifier = it.uri, localFolder = it.localUri, remoteFolder = it.remoteUri) }
    }
}

