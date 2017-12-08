package nsync.metadata

import extensions.asString
import extensions.toByteBuffer
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import mu.KotlinLogging
import commons.AsyncFile
import commons.Configuration
import java.nio.ByteBuffer
import java.util.*

internal data class Index(
        val id: String,
        val relativePath: String,
        val position: Long
) {
    override fun toString(): String {
        return "(path=$relativePath position=$position)"
    }

    companion object : Marshaller<Index> {
        override fun to(obj: Index): ByteBuffer {
            val data = "${obj.id}|${obj.relativePath}|${obj.position}"
            return data.toByteBuffer()
        }

        override fun from(buf: ByteBuffer): Index {
            val token = asString(buf).split("|")
            return Index(
                    id = token[0],
                    relativePath = token[0],
                    position = token[2].toLong()
            )
        }
    }
}

internal data class Inode(
        val checksum: ByteArray, // 16 B
        val size: Long, // 8 B
        val modificationTs: Long, // 8 B
        val status: Status // 4 B
) {

    companion object : Marshaller<Inode> {
        val RECORD_SIZE: Int = 38

        override fun from(buf: ByteBuffer): Inode {
            buf.rewind()
            val checksum = ByteArray(16)
            buf.get(checksum)

            return Inode(
                    checksum,
                    buf.long,
                    buf.long,
                    Status.values()[buf.int]
            )
        }

        override fun to(obj: Inode): ByteBuffer {
            val buf = ByteBuffer.allocateDirect(RECORD_SIZE)
            buf.put(obj.checksum)
            buf.putLong(obj.size)
            buf.putLong(obj.modificationTs)
            buf.putInt(obj.status.ordinal)
            buf.flip()
            return buf
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Inode

        if (!Arrays.equals(checksum, other.checksum)) return false
        if (size != other.size) return false
        if (modificationTs != other.modificationTs) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
                Arrays.hashCode(checksum),
                size.hashCode(),
                modificationTs.hashCode(),
                status.hashCode()
        )
    }
}

internal class Filesystem(
        private val controlFile: AsyncFile<Index>,
        private val inodesFile: AsyncFile<Inode>,
        private val fsId: String) {

    private val logger = KotlinLogging.logger {}
    private val mutex = Mutex()
    private val pathsMap: MutableMap<String, String> = mutableMapOf()
    private val control: MutableMap<String, Index> = mutableMapOf()

    init {
        logger.info { "Reading control file for FS $fsId" }
        controlFile.readLines().forEach {
            if (it.position > -1) {
                pathsMap[it.relativePath] = it.id
                control[it.id] = it
            }
        }
    }

    suspend fun set(id: String, relativePath: String, entry: Inode) {
        mutex.withLock {
            if (relativePath !in this.pathsMap) {
                val newIndex = Index(id, relativePath, inodesFile.size())
                pathsMap[relativePath] = id
                control[id] = newIndex
                controlFile.writeLine(newIndex)
            }

            val pos = control[id]!!.position
            inodesFile.write(entry, pos)
        }
    }

    suspend fun get(id: String): Inode? {
        return mutex.withLock {
            control[id]?.let {
                inodesFile.read(it.position, Inode.RECORD_SIZE)
            }
        }
    }

    suspend fun remove(id: String) {
        mutex.withLock {
            control[id]?.let {
                val newIndex = Index(id, it.relativePath, -it.position)
                controlFile.writeLine(newIndex)
                control.remove(id)
                pathsMap.remove(it.relativePath)
            }
        }
    }

    suspend fun findByPath(relativePath: String): String? {
        mutex.withLock {
            return this.pathsMap[relativePath]
        }
    }

    companion object CachedFactory {
        private val cache: MutableMap<String, Filesystem> = mutableMapOf()
        fun get(fsId: String) =
                cache.getOrPut(fsId, {
                    val metadataDirectory = Configuration.directory
                    val control = AsyncFile(
                            metadataDirectory.resolve("$fsId.$CONTROL_FILE_SUFFIX"),
                            Index
                    )
                    val inodes = AsyncFile(
                            metadataDirectory.resolve("$fsId.$INODE_FILE_SUFFIX"), Inode
                    )
                    Filesystem(control, inodes, fsId)
                })

    }
}