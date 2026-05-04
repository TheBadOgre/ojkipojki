package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameInitializationEventHandlerTest {

    private val handler = GameInitializationEventHandler()

    @BeforeEach
    fun setup() {
        clientContextFixture()
    }

    @Test
    fun `invokes callback with the event verbatim`() {
        var received: GameInitializationEvent? = null
        ClientContext.onGameInitializationUpdate = { received = it }

        val event = GameInitializationEvent(GameInitializationEvent.Status.IN_PROGRESS, "loading", 0.5)
        handler.handle(event)

        assertSame(event, received)
    }

    @Test
    fun `null callback does not throw`() {
        ClientContext.onGameInitializationUpdate = null
        assertDoesNotThrow {
            handler.handle(GameInitializationEvent(GameInitializationEvent.Status.DONE, "done", 1.0))
        }
    }
}
