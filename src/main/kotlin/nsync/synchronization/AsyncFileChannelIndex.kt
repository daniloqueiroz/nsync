package nsync.index

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class AsyncFileChannelIndex(metadataDirectory: Path, uid: String) : Index {

    val indexFile: Path = metadataDirectory.resolve("${uid}.index")
    val dataFile: Path = metadataDirectory.resolve("${uid}.bin")
    val dataChn: AsynchronousFileChannel
    val indexChn: AsynchronousFileChannel
    val index: MutableMap<String, IndexRecord> by lazy {
        this.loadIndex()
    }

    init {
        val dir = metadataDirectory.toFile()
        if (!dir.exists()) {
            dir.mkdirs()
        } else if (!metadataDirectory.toFile().isDirectory()) {
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
            hash.put(indexData.relativePath, indexData)
        }
        return hash
    }

    override fun set(relativePath: String, entry: DataRecord): Deferred<Unit> = async(CommonPool) {
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

    override fun get(relativePath: String): Deferred<DataRecord?> = async(CommonPool) {
        index.get(relativePath)?.let {
            val memBuf = ByteBuffer.allocateDirect(DataRecord.RECORD_SIZE)
            dataChn.aRead(memBuf, it.position)
            memBuf.flip()
            DataRecord.fromRaw(memBuf)
        }
    }

    override fun contains(relativePath: String): Boolean {
        return relativePath in this.index
    }
}