package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.application.ClientStateListener
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectedClientsUpdateEventHandlerTest {

    private val handler = ConnectedClientsUpdateEventHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.client.support.ClientContextFixture

    @BeforeEach
    fun setup() {
        fixture = clientContextFixture()
    }

    @Test
    fun `invokes listener with payload count`() {
        var received = -1
        fixture.notifier.addListener(object : ClientStateListener {
            override fun onConnectedClientsUpdated(count: Int) { received = count }
        })

        handler.handle(ConnectedClientsUpdateEvent(7))

        assertEquals(7, received)
    }

    @Test
    fun `no listener does not throw`() {
        assertDoesNotThrow { handler.handle(ConnectedClientsUpdateEvent(3)) }
    }
}
