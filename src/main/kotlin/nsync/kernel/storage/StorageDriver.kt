package nsync.kernel.storage

import mu.KotlinLogging
import nsync.kernel.RemoteFile
import nsync.kernel.SyncFolder
import nsync.kernel.bus.*
import java.nio.file.Path

interface StorageDriver {
    val bus: SignalBus
    val scheme: String

    suspend fun syncFile(localFile: Path, folder: SyncFolder)
    suspend fun deleteFile(localFile: Path, folder: SyncFolder)
}

// TODO StorageServer
class StorageManager(private val bus: SignalBus) : Consumer {
    private val logger = KotlinLogging.logger {}
    private val drivers: MutableMap<String, StorageDriver> = mutableMapOf()

    init {
        bus.register(this, TransferFile::class, DeleteFile::class)
    }

    fun loadDrivers() {
        // TODO change later to use this https://youtrack.jetbrains.com/issue/KT-14657
        listOf(::LocalFileStorage).forEach {
            val drive = it(bus)
            logger.info { "Adding driver ${drive::class.java.canonicalName} for scheme '${drive.scheme}://'" }
            drivers[drive.scheme] = drive
        }
    }

    suspend override fun handle(msg: Signal<*>) {
        val file = msg.payload as RemoteFile
        drivers[file.folder.schemeRemote]?.let {
            when (msg) {
                is TransferFile -> this.transfer(file, it)
                is DeleteFile -> this.delete(file, it)
                else -> logger.info { "Unexpected message: $msg" }
            }
        }
    }

    private suspend fun transfer(file: RemoteFile, driver: StorageDriver) {
        driver.syncFile(file.localFilePath, file.folder)
    }

    private suspend fun delete(file: RemoteFile, driver: StorageDriver) {
        driver.deleteFile(file.localFilePath, file.folder)
    }
}