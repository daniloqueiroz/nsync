package nsync.metadata

import java.nio.ByteBuffer
import java.util.stream.Stream

internal const val FSTAB_FILENAME = "filesystems"
internal const val CONTROL_FILE_SUFFIX = "control"
internal const val INODE_FILE_SUFFIX = "inodes"

interface Marshaller<T> {
    fun from(buf: ByteBuffer): T
    fun to(obj: T): ByteBuffer
}

interface ObjectStorage<T> {
    val translator: Marshaller<T>
    suspend fun write(obj: T, pos: Long)

    suspend fun read(pos:Long, size: Int): T

    fun readLines(): Stream<T>

    suspend fun writeLine(obj: T)

    fun size(): Long
}