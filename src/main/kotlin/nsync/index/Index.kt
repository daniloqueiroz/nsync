package nsync.index

import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.channels.FileChannel.MapMode
import java.nio.ByteBuffer
import java.util.Scanner
import java.nio.file.Files

enum class SynchronizationStatus {
    PENDING, SYNCHRONIZED
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
        val checksum: ByteArray,
        val size: Long,
        val modificationTs: Long,
        val status: SynchronizationStatus) {

    companion object {
        val RECORD_SIZE: Int = 38

        fun fromRaw(buf: ByteBuffer): DataRecord? {
            buf.rewind()
            val checksum = ByteArray(16)
            buf.get(checksum)

            return DataRecord(
                    checksum,
                    buf.getLong(),
                    buf.getLong(),
                    SynchronizationStatus.values()[buf.getInt()])
        }
    }

    fun toRaw(): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(DataRecord.RECORD_SIZE);
        buf.put(checksum)
        buf.putLong(size)
        buf.putLong(modificationTs)
        buf.putInt(status.ordinal)
        return buf
    }
}

interface Index {
    fun record(relativePath: String, entry: DataRecord): Unit
    fun find(relativePath: String): DataRecord?
}

class MappedFileIndex(directory: Path) : Index {
    val MIN_FILE_SIZE: Long = 5 * 1000 * 1000;
    val indexFile: Path = directory.resolve("index")
    val dataFile: Path = directory.resolve("data")
    val dataChn: FileChannel
    val indexChn: FileChannel
    val index: MutableMap<String, IndexRecord> by lazy {
        this.loadIndex()
    }

    init {
        val dir = directory.toFile()
        if (!dir.exists())
            dir.mkdirs()
        else if (!directory.toFile().isDirectory())
            throw IllegalArgumentException("Not a directory")

        dataChn = FileChannel.open(dataFile, StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE)

        indexChn = FileChannel.open(indexFile, StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE)
    }

    private fun loadIndex(): MutableMap<String, IndexRecord> {
        val hash = HashMap<String, IndexRecord>()
        Files.lines(this.indexFile).forEach {
            val indexData = IndexRecord.fromRaw(it)
            hash.put(indexData.relativePath, indexData)
        }
        return hash
    }

    override fun record(relativePath: String, entry: DataRecord) {
        val indexEntry = IndexRecord(relativePath, this.dataChn.position())
        this.dataChn.write(entry.toRaw())
        if (relativePath in this.index) {
            this.indexChn.write(ByteBuffer.wrap(indexEntry.toRaw().toByteArray()))
        }
        this.index[relativePath] = indexEntry
    }

    override fun find(relativePath: String): DataRecord? {
        return this.index.get(relativePath)?.let {
            val memBuf = this.dataChn.map(MapMode.READ_ONLY, it.position, DataRecord.RECORD_SIZE.toLong())
            return DataRecord.fromRaw(memBuf)
        }
    }
}