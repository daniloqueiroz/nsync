package nsync.storage

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import mu.KLogging
import mu.KotlinLogging
import nsync.*
import nsync.index.SynchronizationStatus
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class LocalFileStorage: StorageBackend, Consumer {
    companion object : KLogging()

    init {
        NBus.register(this, SyncRequest::class)
    }

    suspend override fun onEvent(event: NSyncEvent) {
        val req = event as SyncRequest
        logger.debug {"Sync request received ${req.folder.schemeRemote}"}
        if (req.folder.schemeRemote.equals("file")) {
            this.syncFile(req.syncId, req.localFilePath, req.folder)
        }
    }

    override suspend fun syncFile(syncId: String, localFile: Path, folder: SyncFolder) {
        val src = localFile
        val dst = Paths.get(folder.pathRemote, folder.fileRelativePath(localFile))
        logger.info { "LocalFileStorage: Transferring file $src to $dst" }

        NBus.publish(SyncStatus(syncId, SynchronizationStatus.TRANSFERING))
        if (AsyncFileChannelTransfer(src, dst).call()) {
            NBus.publish(SyncStatus(syncId, SynchronizationStatus.SYNCHRONIZED))
            logger.info { "File $src successfully transferred to $dst" }
        } else {
            NBus.publish(SyncStatus(syncId, SynchronizationStatus.PENDING))
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
        dstFile.toFile().parentFile?.mkdirs() // Ensure dest path (dirs/ subdirs) exists
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