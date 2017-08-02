package nsync.storage

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import mu.KLogging
import nsync.FileChangedEvent
import nsync.SyncFolder
import nsync.index.SynchronizationStatus
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class LocalFileStorage(
        private val folder: SyncFolder
) : StorageBackend {
    private companion object : KLogging()

    override suspend fun syncFile(event: FileChangedEvent, callback: (FileChangedEvent, SynchronizationStatus) -> Unit) {
        val src = event.localFilePath
        val dst = Paths.get(folder.pathRemote, folder.fileRelativePath(event.localFilePath))
        logger.info { "LocalFileStorage: Transferring file $src to $dst" }

        callback(event, SynchronizationStatus.TRANSFERING)
        if (AsyncFileChannelTransfer(src, dst).call()) {
            callback(event, SynchronizationStatus.SYNCHRONIZED)
            logger.info { "File $src successfully transferred to $dst" }
        } else {
            callback(event, SynchronizationStatus.PENDING)
        }
    }
}

internal class AsyncFileChannelTransfer(val srcFile: Path, val dstFile: Path) {
    companion object : KLogging()

    val bufSize = 1024 * 1024  // 1MB
    val buf = ByteBuffer.allocateDirect(bufSize)
    val src: AsynchronousFileChannel by lazy { AsynchronousFileChannel.open(srcFile, StandardOpenOption.READ) }
    val dst: AsynchronousFileChannel by lazy { AsynchronousFileChannel.open(dstFile, StandardOpenOption.WRITE,
            StandardOpenOption.CREATE) }

    suspend fun call(): Boolean {
        var result = true
        var pos: Long = 0

        try {
            do {
                buf.rewind()
                val read = src.aRead(buf, pos)
                buf.flip()
                dst.aWrite(buf, pos)
                pos += read
                logger.debug { "Transferring ${srcFile} to ${dstFile}: ${pos} bytes transferred" }
            } while (read == bufSize)
        } catch (err: IOException) {
            logger.error(err) { "Error during transfer from ${srcFile} to ${dstFile}" }
            result = false
        } finally {
            dst.close()
            src.close()
        }
        return result
    }
}