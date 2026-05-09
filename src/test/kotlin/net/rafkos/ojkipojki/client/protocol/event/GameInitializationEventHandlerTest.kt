package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.application.ClientStateListener
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameInitializationEventHandlerTest {

    private val handler = GameInitializationEventHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.client.support.ClientContextFixture

    @BeforeEach
    fun setup() {
        fixture = clientContextFixture()
    }

    @Test
    fun `invokes listener with the event verbatim`() {
        var received: GameInitializationEvent? = null
        fixture.notifier.addListener(object : ClientStateListener {
            override fun onGameInitializationUpdate(event: GameInitializationEvent) { received = event }
        })

        val event = GameInitializationEvent(GameInitializationEvent.Status.IN_PROGRESS, "loading", 0.5)
        handler.handle(event)

        assertSame(event, received)
    }

    @Test
    fun `no listener does not throw`() {
        assertDoesNotThrow {
            handler.handle(GameInitializationEvent(GameInitializationEvent.Status.DONE, "done", 1.0))
        }
    }
}
