package nsync

import commons.Connection
import commons.Consumer
import commons.Signal
import commons.SignalBus
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import kotlin.reflect.KClass

class SimpleConnection<E, T : Signal<E>>(
        val bus: SimpleBus,
        val outK: KClass<T>
) : Connection<E, T> {

    private val id: UUID
    private var result: T? = null

    init {
        id = bus.register(this, listOf(outK))
    }

    override fun close() {
        bus.deregister(id)
    }

    suspend override fun receive(): T {
        outK
        return this.result!!
    }

    suspend override fun handle(msg: Signal<*>) {
        if (outK.isInstance(msg::class)) {
            result = msg as T
        }
    }

    suspend override fun send(msg: Signal<*>) {
        bus.publish(msg)
    }

}

class SimpleBus : SignalBus {

    val signals: MutableList<Signal<*>> = mutableListOf()
    val consumers: MutableMap<KClass<*>, MutableList<UUID>> = mutableMapOf()
    val keys: MutableMap<UUID, Consumer> = mutableMapOf()

    override fun <E, T : Signal<E>> connect(eventKlass: KClass<T>): Connection<E, T> {
        return SimpleConnection(this, eventKlass)
    }

    suspend override fun <E, T : Signal<E>> publish(msg: T) {
        signals.add(msg)
    }

    override fun register(consumer: Consumer, evtTypes: List<KClass<*>>): UUID {
        val key = UUID.randomUUID()
        for (type in evtTypes) {
            val existent = consumers.getOrDefault(type, mutableListOf())
            existent.add(key)
            consumers[type] = existent
            keys[key] = consumer
        }
        return key
    }

    override fun deregister(key: UUID) {
        val consumer = this.keys.remove(key)
        consumer?.let {
            consumers.values.forEach {
                it.remove(key)
            }
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