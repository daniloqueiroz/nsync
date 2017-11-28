package nsync.kernel.bus

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.yield
import mu.KLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

interface Consumer {
    suspend fun handle(msg: Signal<*>)
}

interface SignalBus {
    fun register(consumer: Consumer, vararg evtTypes: KClass<*>)
    suspend fun <E, T : Signal<E>> publish(type: (E) -> T, data: E)

    fun start()
    fun stop()
    suspend fun join()
}

class AsyncBus : SignalBus {
    private companion object : KLogging()

    private var loop: Job? = null
    private val subscribers: MutableMap<KClass<*>, MutableList<Consumer>> = mutableMapOf()
    private val chn = Channel<Signal<*>>(Channel.UNLIMITED)
    private val size = AtomicInteger(0)

    override fun start() {
        this.loop = launch(CommonPool) {
            logger.info { "Starting SignalBus service" }
            chn.consumeEach {
                val event = it
                val evtType = event::class
                subscribers.getOrDefault(evtType, mutableListOf()).forEach {
                    dispatch(evtType, it, event)
                }
                yield()
            }
        }
    }

    override fun stop() {
        this.loop?.let {
            logger.info { "Stopping SignalBus service" }
            it.cancel()
        }
    }

    override suspend fun join() {
        this.loop?.join()
    }

    override fun register(consumer: Consumer, vararg evtTypes: KClass<*>) {
        for (type in evtTypes) {
            logger.info { "Registering consumer ${consumer::class.java.simpleName} to ${type.java.simpleName}" }
            val existent = subscribers.getOrDefault(type, mutableListOf())
            existent.add(consumer)
            subscribers[type] = existent
        }
    }

    override suspend fun <E, T : Signal<E>> publish(type: (E) -> T, data: E) {
        val msg = type(data)
        logger.debug { "Publishing $msg" }
        size.incrementAndGet()
        chn.send(msg)
    }

    private suspend fun dispatch(evtType: KClass<out Signal<*>>, consumer: Consumer, msg: Signal<*>) {
        try {
            logger.debug { "Dispatching ${evtType.java.simpleName} msg to ${consumer::class.java.simpleName}" }
            logger.debug { "Bus pending message: ${size.getAndDecrement()}" }
            consumer.handle(msg)
        } catch (err: Exception) {
            logger.error(err) { "Error dispatching msg to $consumer" }
        }
    }
}
