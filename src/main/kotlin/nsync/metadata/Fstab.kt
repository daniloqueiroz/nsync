package nsync.metadata

import extensions.asString
import extensions.toByteBuffer
import mu.KLogging
import commons.AsyncFile
import java.net.URI
import java.nio.ByteBuffer
import java.util.*

internal data class FSEntry(
        val id: String,
        val localUri: URI,
        val remoteUri: URI,
        val uri: URI = buildIdentifier(id)
) {
    override fun toString(): String {
        return "(uri=$uri local=$localUri remote=$remoteUri)"
    }

    companion object: Marshaller<FSEntry> {
        override fun to(obj: FSEntry): ByteBuffer {
            val data = "${obj.id} ${obj.localUri} ${obj.remoteUri}"
            return data.toByteBuffer()
        }

        override fun from(buf: ByteBuffer): FSEntry {
            val token = asString(buf).split(" ")
            return FSEntry(token[0], URI(token[1]), URI(token[2]))
        }
    }
}

/**
 * Maintains information about all FS on the system
 */
internal class Fstab(private val fstabFile: AsyncFile<FSEntry>): Sequence<FSEntry> {
    private companion object : KLogging()
    private val filesystems: MutableMap<String, FSEntry> = mutableMapOf()

    init {
        fstabFile.readLines().forEach {
            val fs = it
            filesystems[fs.id] = fs
            logger.info { "Loading FS: $fs"}
        }
    }

    fun findFS(fsIdentifier: String): FSEntry? {
        return this.filesystems[fsIdentifier]
    }

    suspend fun addFS(local: URI, remote: URI): FSEntry {
        checkDuplicate(local, remote)

        val fs = FSEntry(UUID.randomUUID().toString(), local, remote)
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

    override fun iterator(): Iterator<FSEntry> {
        return this.filesystems.values.iterator()
    }
}