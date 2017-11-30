package nsync.extensions

import java.nio.ByteBuffer
import java.nio.charset.Charset

val UTF_8 = Charset.forName("UTF-8")

internal fun String.toByteBuffer(): ByteBuffer {
    return UTF_8.encode(this)
}

fun asString(buf: ByteBuffer): String {
    val content = ByteArray(buf.limit())
    buf.get(content)
    return content.toString(UTF_8)
}
