package net.rafkos.ojkipojki.shared

import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

abstract class Dispatcher<A : Any>(
    private val handlers: Map<KClass<out A>, Handler<in A>>
) {
    fun dispatch(action: A) {
        val handler = findHandler(action::class)
        if (handler != null) {
            handler.handle(action)
        } else {
            log.warn("Handler for ${action::class.simpleName} not found")
        }
    }

    private fun findHandler(clazz: KClass<out A>): Handler<in A>? {
        return handlers[clazz]
    }

    companion object {
        private val log = LogManager.getLogger(Dispatcher::class.java)
    }
}