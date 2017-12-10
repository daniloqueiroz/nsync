package commons

import java.io.Closeable
import java.util.*
import kotlin.reflect.KClass


interface Signal<out T> {
    val payload: T
}

interface Consumer {
    suspend fun handle(msg: Signal<*>)
}

class NoResponseException(cause: Exception): Exception(cause)

interface Connection<F, G : Signal<F>> : Consumer, Closeable {
    suspend fun send(msg: Signal<*>)

    suspend fun receive(msTimeout: Long = 500): G
}

interface SignalBus {
    suspend fun join()

    fun <E, T : Signal<E>> connect(eventKlass: KClass<T>): Connection<E, T>
    fun register(consumer: Consumer, evtTypes: List<KClass<*>>): UUID
    fun deregister(key: UUID)

    suspend fun <E, T : Signal<E>> publish(msg: T)
}


abstract class Server(private val bus: SignalBus, events: List<KClass<*>>) : Consumer {
    init {
        bus.register(this, events)
    }

    protected suspend fun <E, T : Signal<E>> publish(msg: T) {
        this.bus.publish(msg)
    }
}

