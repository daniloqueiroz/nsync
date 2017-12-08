package commons

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import extensions.toByteBuffer
import nsync.metadata.Marshaller
import nsync.metadata.ObjectStorage
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Stream

class AsyncFile<T> (
        private val file: Path,
        override val translator: Marshaller<T>
): ObjectStorage<T> {
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

    override suspend fun write(obj: T, pos: Long) {
        val data = this.translator.to(obj)
        chn.aWrite(data, pos)
        chn.force(false)
    }

    override suspend fun read(pos:Long, size: Int): T {
        val buf = ByteBuffer.allocateDirect(size)
        chn.aRead(buf, pos)
        buf.flip()
        return translator.from(buf)
    }

    override fun readLines(): Stream<T> {
        return Files.lines(this.file).map {
            translator.from(it.toByteBuffer())
        }
    }

    override suspend fun writeLine(obj: T) {
        val data = this.translator.to(obj)
        chn.aWrite(data, chn.size())
        chn.aWrite("\n".toByteBuffer(), chn.size())
        chn.force(false)
    }

    override fun size(): Long {
        return chn.size()
    }
}