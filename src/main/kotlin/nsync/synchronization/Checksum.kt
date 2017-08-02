package nsync.synchronization


import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.nio.aRead
import mu.KLogging
import nsync.toHexString
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.StandardOpenOption
import java.security.MessageDigest


class Checksum(val file: File) {
    private companion object : KLogging()

    private val md5sum = MessageDigest.getInstance("md5")
    private val bufSize = 1024  // 1KB
    private val buf = ByteBuffer.allocateDirect(bufSize)

    suspend fun compute(): ByteArray {
        logger.debug { "Calculating checksum for ${file.absolutePath}" }
        var chn = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)
        md5sum.reset()
        var pos: Long = 0
        do {
            buf.rewind()
            val read = chn!!.aRead(buf, pos)
            pos += read
            buf.flip()
            md5sum.update(buf)
        } while (read == bufSize)
        val md5 = md5sum.digest()!!
        logger.info { "Checksum for ${file.absolutePath} is ${md5.toHexString()}" }
        return md5
    }
}