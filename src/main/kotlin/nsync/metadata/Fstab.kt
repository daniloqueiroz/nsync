package nsync.metadata

import extensions.asString
import extensions.toByteBuffer
import mu.KLogging
import utils.AsyncFile
import java.net.URI
import java.nio.ByteBuffer
import java.util.*

internal data class Fsentry(
        val id: String,
        val localUri: URI,
        val remoteUri: URI,
        override val uri: URI = buildIdentifier(id)
): Resource {
    override fun toString(): String {
        return "(uri=$uri local=$localUri remote=$remoteUri)"
    }

    companion object: Marshaller<Fsentry> {
        override fun to(obj: Fsentry): ByteBuffer {
            val data = "${obj.id}|${obj.localUri}|${obj.remoteUri}"
            return data.toByteBuffer()
        }

        override fun from(buf: ByteBuffer): Fsentry {
            val token = asString(buf).split("|")
            return Fsentry(token[0], URI(token[1]), URI(token[2]))
        }
    }
}

/**
 * Maintains information about all FS on the system
 */
internal class Fstab(private val fstabFile: AsyncFile<Fsentry>) {
    private companion object : KLogging()
    private val filesystems: MutableMap<String, Fsentry> = mutableMapOf()

    init {
        fstabFile.readLines().forEach {
            val fs = it
            filesystems[fs.id] = fs
            logger.info { "Loading FS: $fs"}
        }
    }

    fun forEach(operation: (Fsentry) -> Unit) {
        this.filesystems.values.forEach {
            operation(it)
        }
    }

    fun findFS(fsIdentifier: String): Fsentry? {
        return this.filesystems[fsIdentifier]
    }

    suspend fun addFS(local: URI, remote: URI): Fsentry {
        checkDuplicate(local, remote)

        val fs = Fsentry(UUID.randomUUID().toString(), local, remote)
        logger.info { "Adding FS: $fs" }
        this.filesystems[fs.id] = fs
        this.fstabFile.writeLine(fs)
        return fs
    }

    private fun checkDuplicate(local: URI, remote: URI) {
        this.filesystems.values.forEach {
            if (it.localUri == local || it.remoteUri == remote)
                throw IllegalArgumentException("Folder is already registered")
        }
    }
}