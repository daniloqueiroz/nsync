package nsync

import nsync.signals.Consumer
import nsync.signals.Signal
import nsync.signals.SignalBus
import kotlin.reflect.KClass


abstract class Server(private val bus: SignalBus, events: List<KClass<*>>): Consumer {
    init{
        bus.register(this, events)
    }

    protected suspend fun <E, T : Signal<E>> publish(type: (E) -> T, data: E) {
        this.bus.publish(type, data)
    }
}