package nsync.kernel.bus

import nsync.kernel.RemoteFile
import nsync.kernel.LocalFile
import nsync.kernel.SyncFolder
import nsync.kernel.TransferStatus

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

// Notify
class FolderAdded(data: SyncFolder): Signal<SyncFolder>(data)
class FileModified(data: LocalFile): Signal<LocalFile>(data)
class FileDeleted(data: LocalFile): Signal<LocalFile>(data)

// Action
class TransferFile(data: RemoteFile) : Signal<RemoteFile>(data)
class DeleteFile(data: RemoteFile) : Signal<RemoteFile>(data)
class ChangeStatus(data: TransferStatus) : Signal<TransferStatus>(data)
