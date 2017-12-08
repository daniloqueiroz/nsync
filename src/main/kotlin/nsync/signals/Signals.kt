package nsync.signals

import java.net.URI
import kotlin.reflect.KClass

/**
 * Signals are any entity that are passed using SignalBus. All Signal are broadcast, so any component can register
 * to receive them.
 *
 * Semantically there are two kind of Signals:
 *
 * * Notify Signals: Informs that an event has happened. Usually their name include a verb in the past, to indicate
 * which event has happened;
 * * Action Signal: Indicates an action MUST be taken by interested parties. Usually named using imperative verbs.
 *
 * Signals can contains some data as its payload.
 */
sealed class Signal<out T>(
        val payload: T
)

interface Consumer {
    suspend fun handle(msg: Signal<*>)
}

interface SignalBus {
    fun register(consumer: Consumer, evtTypes: List<KClass<*>>)
    suspend fun <E, T : Signal<E>> publish(type: (E) -> T, data: E)

    fun start()
    fun stop()
    suspend fun join()
}

// Action
class AddFS(data: FS): Signal<FS>(data)
class TransferFile(data: RemoteFile) : Signal<RemoteFile>(data)
class DeleteFile(data: RemoteFile) : Signal<RemoteFile>(data)
class ChangeStatus(data: TransferStatus) : Signal<TransferStatus>(data)


// Notify
class FSAdded(data: FS): Signal<FS>(data)
class FileModified(data: LocalFile): Signal<LocalFile>(data)
class FileDeleted(data: LocalFile): Signal<LocalFile>(data)

data class FS(
        val localFolder: URI,
        val remoteFolder: URI,
        val identifier: URI? = null)

