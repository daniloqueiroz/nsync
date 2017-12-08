package nsync

import java.io.Closeable
import java.util.*
import kotlin.reflect.KClass

interface Consumer {
    suspend fun handle(msg: Signal<*>)
}

interface Connection<F, G : Signal<F>> : Consumer, Closeable {
    suspend fun send(msg: Signal<*>)

    suspend fun receive(): G
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

