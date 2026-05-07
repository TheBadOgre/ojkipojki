package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.event.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class EventDispatcherTest {

    private val expectedEventTypes: Set<KClass<out Event>> = setOf(
        SpriteBagsUpdatedEvent::class,
        TokensSyncEvent::class,
        SomeTokensUpdatedEvent::class,
        PointersUpdatedEvent::class,
        ConnectedClientsUpdateEvent::class,
        GameInitializationEvent::class,
        MissingSpriteBagsEvent::class,
    )

    @Test
    fun `EventDispatcher has handler for every known event type`() {
        val dispatcher = EventDispatcher()
        val handlersField = Dispatcher::class.java.getDeclaredField("handlers").also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val handlers = handlersField.get(dispatcher) as Map<KClass<*>, *>

        expectedEventTypes.forEach { type ->
            assertTrue(handlers.containsKey(type)) {
                "Missing handler for ${type.simpleName}"
            }
        }
    }
}
