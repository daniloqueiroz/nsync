package nsync.kernel.bus

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import mu.KotlinLogging
import nsync.extensions.forEach
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

object NBus {
    private val logger = KotlinLogging.logger {}
    private val subscribers: MutableMap<KClass<*>, MutableList<Consumer>> = mutableMapOf()
    private val chn = Channel<Signal<*>>(Channel.UNLIMITED)
    private val size = AtomicInteger(0)

    fun register(consumer: Consumer, vararg evtTypes: KClass<*>) {
        for (type in evtTypes) {
            logger.info { "Registering consumer ${consumer::class.java.simpleName} to ${type.java.simpleName}" }
            val existent = subscribers.getOrDefault(type, mutableListOf())
            existent.add(consumer)
            subscribers[type] = existent
        }
    }

    suspend fun <E,T: Signal<E>> publish(type: (E) -> T, data: E) {
        val msg = type(data)
        logger.debug { "Publishing $msg" }
        size.incrementAndGet()
        chn.send(msg)
    }

    fun start() {
        launch(CommonPool) {
            chn.forEach {
                val event = it
                val evtType = event::class
                subscribers.getOrDefault(evtType, mutableListOf()).forEach {
                    dispatch(evtType, it, event)
                }
                yield()
            }
        }
    }

    private suspend fun dispatch(evtType: KClass<out Signal<*>>, consumer: Consumer, msg: Signal<*>) {
        try {
            logger.debug { "Dispatching ${evtType.java.simpleName} msg to ${consumer::class.java.simpleName}" }
            logger.debug { "Bus pending message: ${size.getAndDecrement()}" }
            consumer.onEvent(msg)
        } catch (err: Exception) {
            logger.error(err) { "Error dispatching msg to $consumer" }
        }
    }
}

interface Consumer {
    suspend fun onEvent(msg: Signal<*>)
}
