package nsync.extensions

import kotlinx.coroutines.experimental.channels.ReceiveChannel

internal suspend fun <T> ReceiveChannel<T>.forEach(block: suspend (T) -> Unit) : Unit {
    for (element in this) block(element)
}

