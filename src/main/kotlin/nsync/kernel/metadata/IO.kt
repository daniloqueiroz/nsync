package nsync.kernel.metadata

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import nsync.extensions.toByteBuffer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Stream

interface Marshaller<T> {
    fun from(buf: ByteBuffer): T
    fun to(obj: T): ByteBuffer
}

class AsyncFile<T>(
        private val file: Path,
        private val translator: Marshaller<T>
) {
    private val chn: AsynchronousFileChannel

    init {
        if (!file.toFile().exists()) {
            file.toFile().createNewFile()
        }

        chn = AsynchronousFileChannel.open(
                file, StandardOpenOption.WRITE,
                StandardOpenOption.READ, StandardOpenOption.DSYNC
        )
    }

    suspend fun write(obj: T, pos: Long) {
        val data = this.translator.to(obj)
        chn.aWrite(data, pos)
        chn.force(false)
    }

    suspend fun read(pos:Long, size: Int): T {
        val buf = ByteBuffer.allocateDirect(size)
        chn.aRead(buf, pos)
        buf.flip()
        return translator.from(buf)
    }

    fun readLines(): Stream<T> {
        return Files.lines(this.file).map {
            translator.from(it.toByteBuffer())
        }
    }

    suspend fun writeLine(obj: T) {
        val data = this.translator.to(obj)
        chn.aWrite(data, chn.size())
        chn.aWrite("\n".toByteBuffer(), chn.size())
        chn.force(false)
    }

    fun size(): Long {
        return chn.size()
    }
}