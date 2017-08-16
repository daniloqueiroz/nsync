package nsync.index

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class AsyncFileChannelIndex(metadataDirectory: Path, uid: String) : Index {
    private val mutex = Mutex()
    private val readBuf = ByteBuffer.allocateDirect(DataRecord.RECORD_SIZE)
    private val indexFile: Path = metadataDirectory.resolve("$uid.index")
    private val dataFile: Path = metadataDirectory.resolve("$uid.bin")
    private val dataChn: AsynchronousFileChannel
    private val indexChn: AsynchronousFileChannel
    private val index: MutableMap<String, IndexRecord> by lazy {
        this.loadIndex()
    }

    init {
        val dir = metadataDirectory.toFile()
        if (!dir.exists()) {
            dir.mkdirs()
        } else if (!metadataDirectory.toFile().isDirectory) {
            throw IllegalArgumentException("Not a metadataDirectory")
        }

        if (!dataFile.toFile().exists()) {
            dataFile.toFile().createNewFile()
        }

        if (!indexFile.toFile().exists()) {
            indexFile.toFile().createNewFile()
        }

        dataChn = AsynchronousFileChannel.open(dataFile, StandardOpenOption.WRITE,
                StandardOpenOption.READ, StandardOpenOption.DSYNC)

        indexChn = AsynchronousFileChannel.open(indexFile, StandardOpenOption.WRITE,
                StandardOpenOption.READ, StandardOpenOption.DSYNC)
    }

    private fun loadIndex(): MutableMap<String, IndexRecord> {
        val hash = HashMap<String, IndexRecord>()
        Files.lines(this.indexFile).forEach {
            val indexData = IndexRecord.fromRaw(it)
            if (indexData.position > -1) {
                hash.put(indexData.relativePath, indexData)
            }
        }
        return hash
    }

    override suspend fun set(relativePath: String, entry: DataRecord) {
        mutex.withLock {
            if (!contains(relativePath)) {
                val newIndex = IndexRecord(relativePath, dataChn.size())
                index[relativePath] = newIndex
                indexChn.write(ByteBuffer.wrap(newIndex.toRaw().toByteArray()), indexChn.size())
                indexChn.force(false)
            }

            val indexEntry: IndexRecord = index[relativePath]!!
            dataChn.aWrite(entry.toRaw(), indexEntry.position)
            dataChn.force(false)
        }
    }

    override suspend fun get(relativePath: String): DataRecord? {
       return mutex.withLock {
            index[relativePath]?.let {
                readBuf.rewind()
                dataChn.aRead(readBuf, it.position)
                readBuf.flip()
                DataRecord.fromRaw(readBuf)
            }
        }
    }

    override suspend fun remove(relativePath: String) {
        mutex.withLock {
            index[relativePath]?.let {
                val newIndex = IndexRecord(relativePath, -it.position)
                indexChn.write(ByteBuffer.wrap(newIndex.toRaw().toByteArray()), indexChn.size())
                index.remove(relativePath)
            }
        }
    }

    override suspend fun contains(relativePath: String): Boolean {
        return relativePath in this.index
    }
}