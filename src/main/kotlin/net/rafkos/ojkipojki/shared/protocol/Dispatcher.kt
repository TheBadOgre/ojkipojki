package net.rafkos.ojkipojki.shared.protocol

import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

abstract class Dispatcher<A : Any>(
    private val handlers: Map<KClass<out A>, Handler<in A>>
) {
    fun dispatch(action: A) {
        val handler = handlers[action::class]
        if (handler != null) {
            handler.handle(action)
        } else {
            log.warn("Handler for ${action::class.simpleName} not found")
        }
    }

    companion object {
        private val log = LogManager.getLogger(Dispatcher::class.java)
    }
}
