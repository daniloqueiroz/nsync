package nsync

import kotlinx.coroutines.experimental.runBlocking
import nsync.signals.Consumer
import nsync.signals.Signal
import nsync.signals.SignalBus
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

    override fun register(consumer: Consumer, evtTypes: List<KClass<*>>) {
        evtTypes.forEach {
            val list = if (it in consumers) consumers[it]!! else mutableListOf()
            list.add(consumer)
            consumers[it] = list
        }
    }

    override suspend fun join() {}

    fun reset() {
        signals.clear()
        consumers.clear()
    }
}

/**
 * Sugar for ``runBlocking<Unit>`` to be used on tests
 */
fun asyncTest(block: suspend () -> Unit): Unit = runBlocking<Unit> { block() }