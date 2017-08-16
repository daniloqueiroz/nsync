package nsync

import kotlinx.coroutines.experimental.runBlocking
import nsync.kernel.SyncFolder
import nsync.kernel.bus.Consumer
import nsync.kernel.bus.Signal
import nsync.kernel.bus.SignalBus
import kotlin.reflect.KClass

class SimpleBus: SignalBus {
    val signals: MutableList<Signal<*>> = mutableListOf()
    val consumers: MutableMap<KClass<*>, MutableList<Consumer>> = mutableMapOf()

    override fun start() {
    }

    override fun stop() {
    }

    suspend override fun <E, T : Signal<E>> publish(type: (E) -> T, data: E) {
        signals.add(type(data))
    }

    override fun register(consumer: Consumer, vararg evtTypes: KClass<*>) {
        evtTypes.forEach {
            val list = if (it in consumers) consumers[it]!! else mutableListOf<Consumer>()
            list.add(consumer)
            consumers[it] = list
        }
    }

    fun reset() {
        signals.clear()
        consumers.clear()
    }
}

fun confOf(vararg folders: SyncFolder): Configuration {
    val dirs = mutableMapOf<String, ConfSyncFolder>()
    for(f in folders) {
        dirs[f.folderId] = ConfSyncFolder(f.folderId, f.localFolder, f.remoteFolder)
    }
    val conf = Configuration()
    conf.synchronization = dirs
    return conf
}

/**
 * Sugar for ``runBlocking<Unit>`` to be used on tests
 */
fun asyncTest(block: suspend () -> Unit): Unit = runBlocking<Unit> { block() }