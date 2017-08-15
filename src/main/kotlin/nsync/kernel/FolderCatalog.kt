package nsync.kernel

import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.ConfSyncFolder
import nsync.Configuration
import nsync.kernel.bus.FolderAdded
import nsync.kernel.bus.NBus
import java.util.*

class FolderCatalog(private val conf: Configuration, val bus: NBus) {
    companion object : KLogging()

    init {
        if (this.conf.synchronization == null) {
            this.conf.synchronization = mutableMapOf()

            runBlocking {
                logger.info { "Loading existent folders" }
                forEach {
                    bus.publish(::FolderAdded, it)
                }
            }
        }
    }

    suspend fun register(localUri: String, remoteUri: String): SyncFolder {
        this.checkDuplicate(remoteUri)

        val folder = SyncFolder(UUID.randomUUID().toString(), localUri, remoteUri)
        logger.info { "Registering new $folder" }


        val dirs: MutableMap<String, ConfSyncFolder> = this.conf.synchronization!!
        dirs[folder.folderId] = ConfSyncFolder(folder.folderId, folder.localFolder, folder.remoteFolder)
        this.conf.synchronization = dirs

        bus.publish(::FolderAdded, folder)
        return folder
    }

    private fun checkDuplicate(remoteUri: String) {
        this.forEach {
            if (it.remoteFolder == remoteUri)
                throw IllegalArgumentException("Folder is already registered")
        }
    }

    fun find(uid: String): SyncFolder? {
        return this.conf.synchronization!![uid]?.let { SyncFolder(it.uid, it.localFolder, it.remoteFolder) }
    }

    operator fun iterator(): Iterator<SyncFolder> {
        return this.conf.synchronization!!.values.map { SyncFolder(it.uid, it.localFolder, it.remoteFolder) }.iterator()
    }

    inline fun forEach(operation: (SyncFolder) -> Unit) : Unit {
        for (folder in this) operation(folder)
    }
}