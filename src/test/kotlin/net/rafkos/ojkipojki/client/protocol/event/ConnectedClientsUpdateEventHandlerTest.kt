package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectedClientsUpdateEventHandlerTest {

    private val handler = ConnectedClientsUpdateEventHandler()

    @BeforeEach
    fun setup() {
        clientContextFixture()
    }

    @Test
    fun `invokes callback with payload count`() {
        var received = -1
        ClientContext.onConnectedClientsUpdated = { received = it }

        handler.handle(ConnectedClientsUpdateEvent(7))

        assertEquals(7, received)
    }

    @Test
    fun `null callback does not throw`() {
        ClientContext.onConnectedClientsUpdated = null
        assertDoesNotThrow { handler.handle(ConnectedClientsUpdateEvent(3)) }
    }
}
