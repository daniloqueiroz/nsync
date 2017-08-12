package nsync.kernel.storage

import mu.KotlinLogging
import nsync.kernel.RemoteFile
import nsync.kernel.SyncFolder
import nsync.kernel.bus.Consumer
import nsync.kernel.bus.NBus
import nsync.kernel.bus.Signal
import nsync.kernel.bus.TransferFile
import java.nio.file.Path

interface StorageDriver {
    val scheme: String
    suspend fun syncFile(localFile: Path, folder: SyncFolder)
}


class StorageManager: Consumer {
    private val logger = KotlinLogging.logger {}
    private val drivers: MutableMap<String, StorageDriver> = mutableMapOf()

    init {
        NBus.register(this, TransferFile::class)
    }

    fun loadDrivers() {
        // TODO change later to use this https://youtrack.jetbrains.com/issue/KT-14657
        listOf(::LocalFileStorage).forEach {
            val drive = it()
            logger.info { "Adding driver $drive for scheme ${drive.scheme}" }
            drivers[drive.scheme] = drive
        }
    }

    suspend override fun handle(msg: Signal<*>) {
        when (msg) {
            is TransferFile -> this.transfer(msg.payload)
        }
    }

    private suspend fun  transfer(file: RemoteFile) {
        drivers[file.folder.schemeRemote]?.let {
            logger.info { "Redirecting file $file to $it" }
            it.syncFile(file.localFilePath, file.folder)
        }

    }



}