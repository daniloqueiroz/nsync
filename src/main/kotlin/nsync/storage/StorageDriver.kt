package nsync.storage

import commons.Server
import commons.Signal
import commons.SignalBus
import mu.KotlinLogging
import nsync.*
import java.nio.file.Path

interface StorageDriver {
    val bus: SignalBus
    val scheme: String

    suspend fun syncFile(localFile: Path, folder: SyncFolder)
    suspend fun deleteFile(localFile: Path, folder: SyncFolder)
}

class StorageServer(private val bus: SignalBus) : Server(bus, listOf(TransferFile::class, DeleteFile::class)) {
    private val logger = KotlinLogging.logger {}
    private val drivers: MutableMap<String, StorageDriver> = mutableMapOf()

    fun loadDrivers() {
        // TODO change later to use this https://youtrack.jetbrains.com/issue/KT-14657
        listOf(::LocalFileStorage).forEach {
            val drive = it(bus) // TODO pass this (as StorageController interface to drivers)
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