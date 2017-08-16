package nsync.index

import java.nio.ByteBuffer
import kotlinx.coroutines.experimental.Deferred
import java.util.*

enum class SynchronizationStatus {
    PENDING, TRANSFERRING, SYNCHRONIZED
}


data class IndexRecord(
        val relativePath: String,
        val position: Long) {

    fun toRaw(): String {
        return "$relativePath:$position\n"
    }

    companion object {
        fun fromRaw(buf: String): IndexRecord {
            val tks = buf.split(":")
            return IndexRecord(tks[0], tks[1].toLong())
        }
    }
}


data class DataRecord(
        val checksum: ByteArray, // 16 B
        val size: Long, // 8 B
        val modificationTs: Long, // 8 B
        val status: SynchronizationStatus // 4 B
) {

    companion object {
        val RECORD_SIZE: Int = 38

        fun fromRaw(buf: ByteBuffer): DataRecord? {
            buf.rewind()
            val checksum = ByteArray(16)
            buf.get(checksum)

            return DataRecord(
                    checksum,
                    buf.long,
                    buf.long,
                    SynchronizationStatus.values()[buf.int])
        }
    }

    fun toRaw(): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(DataRecord.RECORD_SIZE)
        buf.put(checksum)
        buf.putLong(size)
        buf.putLong(modificationTs)
        buf.putInt(status.ordinal)
        buf.flip()
        return buf
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataRecord

        if (!Arrays.equals(checksum, other.checksum)) return false
        if (size != other.size) return false
        if (modificationTs != other.modificationTs) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(checksum)
        result = 31 * result + size.hashCode()
        result = 31 * result + modificationTs.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

}

interface Index {
    suspend fun set(relativePath: String, entry: DataRecord): Unit
    suspend fun get(relativePath: String): DataRecord?
    suspend fun contains(relativePath: String): Boolean
    suspend fun remove(relativePath: String): Unit
}

