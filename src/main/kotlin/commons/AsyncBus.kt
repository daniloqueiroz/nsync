package commons

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import mu.KLogging
import nsync.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class AsyncConnection<F, G : Signal<F>>(
        private val bus: SignalBus,
        outK: KClass<G>
) : Connection<F, G> {

    private val id: UUID
    private val chn = Channel<Signal<*>>()

    init {
        id = bus.register(this, listOf(outK))
    }

    suspend override fun handle(msg: Signal<*>) {
        this.chn.send(msg)
    }

    suspend override fun send(msg: Signal<*>) = bus.publish(msg)

    suspend override fun receive(timeout: Long): G = runBlocking {
        try {
            withTimeout(timeout, TimeUnit.MILLISECONDS, {
                chn.receive() as G
            })
        } catch (err: Exception) {
            throw NoResponseException(err)
        }
    }

    override fun close() {
        this.bus.deregister(this.id)
        this.chn.close()
    }
}


class AsyncBus : SignalBus {
    private companion object : KLogging()

    private lateinit var loop: Job
    private val subscribers: MutableMap<KClass<*>, MutableList<UUID>> = mutableMapOf()
    private val keys: MutableMap<UUID, Consumer> = mutableMapOf()
    private val chn = Channel<Signal<*>>(Channel.UNLIMITED)
    private val size = AtomicInteger(0)

    init {
        this.loop = launch(CommonPool) {
            logger.info { "Starting SignalBus service" }
            chn.consumeEach {
                val event = it
                val evtType = event::class
                subscribers.getOrDefault(evtType, mutableListOf()).forEach {
                    dispatch(evtType, it, event)
                }
                when (event) {
                    is Stop -> {
                        logger.info { "Stopping SignalBus service" }
                        loop.cancel()
                        chn.close()
                    }
                }
                yield()
            }
        }
    }

    override suspend fun join() {
        this.loop?.join()
    }

    override fun <E, T : Signal<E>> connect(eventKlass: KClass<T>): Connection<E, T> {
        return AsyncConnection(this, eventKlass)
    }

    override fun register(consumer: Consumer, evtTypes: List<KClass<*>>): UUID {
        val key = UUID.randomUUID()
        for (type in evtTypes) {
            logger.info { "Registering consumer ${consumer::class.java.simpleName} to ${type.java.simpleName}" }
            val existent = subscribers.getOrDefault(type, mutableListOf())
            existent.add(key)
            subscribers[type] = existent
            keys[key] = consumer
        }
        return key
    }

    override fun deregister(key: UUID) {
        val consumer = this.keys.remove(key)
        consumer?.let {
            logger.info { "Deregistering consumer ${it::class.java.simpleName}" }
            subscribers.values.forEach {
                it.remove(key)
            }
        }
    }

    override suspend fun <E, T : Signal<E>> publish(msg: T) {
        logger.debug { "Publishing $msg" }
        size.incrementAndGet()
        chn.send(msg)
    }

    private suspend fun dispatch(evtType: KClass<out Signal<*>>, consumerId: UUID, msg: Signal<*>) {
        keys[consumerId]?.let {
            try {
                logger.debug { "Dispatching ${evtType.java.simpleName} msg to ${it::class.java.simpleName}" }
                logger.debug { "Bus pending message: ${size.getAndDecrement()}" }
                it.handle(msg)
            } catch (err: Exception) {
                logger.error(err) { "Error dispatching msg to $it" }
            }
        }
    }
}
