package nsync

import mu.KLogging
import java.util.*

class FolderCatalog(private val conf: Configuration) {
    companion object : KLogging()

    init {
        if (this.conf.synchronization == null) {
            print("initialize map")
            this.conf.synchronization = mutableMapOf()
        }
    }

    fun register(localUri: String, remoteUri: String): SyncFolder {
        val folder = SyncFolder(UUID.randomUUID().toString(), localUri, remoteUri)
        logger.info { "Registering new $folder" }


        val dirs: MutableMap<String, ConfSyncFolder> = this.conf.synchronization!!
        dirs[folder.folderId] = ConfSyncFolder(folder.folderId, folder.localFolder, folder.remoteFolder)
        this.conf.synchronization = dirs

        return folder
    }

    fun find(uid: String): SyncFolder? {
        return this.conf.synchronization!![uid]?.let { SyncFolder(it.uid, it.localFolder, it.remoteFolder) }
    }

    operator fun iterator(): Iterator<SyncFolder> {
        return this.conf.synchronization!!.values.map { SyncFolder(it.uid, it.localFolder, it.remoteFolder) }.iterator()
    }
}
