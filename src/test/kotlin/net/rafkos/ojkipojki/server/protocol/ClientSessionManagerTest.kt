package net.rafkos.ojkipojki.server.protocol

import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent
import net.rafkos.ojkipojki.support.await
import net.rafkos.ojkipojki.support.socketPair
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.verify
import java.io.ObjectOutputStream
import java.net.Socket

class ClientSessionManagerTest {

    private val manager = ClientSessionManager()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture
    private val openSockets = mutableListOf<Socket>()

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        openSockets.forEach { runCatching { it.close() } }
    }

    private fun connect(clientId: String): Pair<Socket, Socket> {
        val (clientSocket, acceptedSocket) = socketPair()
        openSockets += clientSocket
        openSockets += acceptedSocket

        // Send OOS stream header from client side before manager creates OIS in CommandReceiver
        val bgThread = Thread { ObjectOutputStream(clientSocket.getOutputStream()).flush() }.also { it.start() }
        manager.onClientConnected(clientId, acceptedSocket)
        bgThread.join(1000)
        return clientSocket to acceptedSocket
    }

    @Test
    fun `onClientConnected registers session and assigns color`() {
        connect("c1")

        assertTrue(manager.getAllClientIds().contains("c1"))
        assertNotNull(fixture.clientColorRegistry.getColor("c1"))
        assertNotNull(manager.getTransmitter("c1"))
    }

    @Test
    fun `onClientConnected broadcasts ConnectedClientsUpdateEvent`() {
        connect("c1")

        val captor = argumentCaptor<net.rafkos.ojkipojki.shared.protocol.event.Event>()
        verify(fixture.eventBroadcastService, atLeast(1)).broadcast(captor.capture())

        val counts = captor.allValues.filterIsInstance<ConnectedClientsUpdateEvent>()
        assertTrue(counts.any { it.numOfClients == 1 }) {
            "Expected ConnectedClientsUpdateEvent(1), got: ${counts.map { it.numOfClients }}"
        }
    }

    @Test
    fun `onClientDisconnected unregisters session and releases color`() {
        connect("c1")

        manager.onClientDisconnected("c1")

        assertTrue(manager.getAllClientIds().isEmpty())
        assertNull(fixture.clientColorRegistry.getColor("c1"))
    }

    @Test
    fun `onClientDisconnected broadcasts ConnectedClientsUpdateEvent with decremented count`() {
        connect("c1")

        manager.onClientDisconnected("c1")

        val captor = argumentCaptor<net.rafkos.ojkipojki.shared.protocol.event.Event>()
        verify(fixture.eventBroadcastService, atLeast(1)).broadcast(captor.capture())

        val counts = captor.allValues.filterIsInstance<ConnectedClientsUpdateEvent>()
        assertTrue(counts.any { it.numOfClients == 0 }) {
            "Expected ConnectedClientsUpdateEvent(0) after disconnect"
        }
    }

    @Test
    fun `onClientDisconnected invokes broadcastCustom for pointer update`() {
        connect("c1")

        manager.onClientDisconnected("c1")

        verify(fixture.eventBroadcastService, atLeast(1)).broadcastCustom(org.mockito.kotlin.anyOrNull(), any())
    }

    @Test
    fun `multiple clients have distinct colors`() {
        connect("c1")
        connect("c2")
        connect("c3")

        assertEquals(3, manager.getAllClientIds().size)
        val colors = setOf(
            fixture.clientColorRegistry.getColor("c1"),
            fixture.clientColorRegistry.getColor("c2"),
            fixture.clientColorRegistry.getColor("c3"),
        )
        assertEquals(3, colors.size)
    }

    @Test
    fun `disconnect one of multiple clients leaves others registered`() {
        connect("c1")
        connect("c2")
        connect("c3")

        manager.onClientDisconnected("c2")

        assertEquals(2, manager.getAllClientIds().size)
        assertTrue(manager.getAllClientIds().containsAll(setOf("c1", "c3")))
    }

    @Test
    fun `onClientConnected sends MissingSpriteBagsEvent unicast after SpriteBagsUpdatedEvent`() {
        connect("c1")

        val eventCaptor = argumentCaptor<net.rafkos.ojkipojki.shared.protocol.event.Event>()
        val clientCaptor = argumentCaptor<String>()
        verify(fixture.eventBroadcastService, atLeast(1)).broadcast(eventCaptor.capture(), clientCaptor.capture())

        val unicastEvents = eventCaptor.allValues
        val unicastClients = clientCaptor.allValues

        val spriteBagsIdx = unicastEvents.indexOfFirst { it is SpriteBagsUpdatedEvent }
        val missingIdx = unicastEvents.indexOfFirst { it is net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent }

        assertTrue(missingIdx >= 0) { "MissingSpriteBagsEvent not sent unicast" }
        assertTrue(missingIdx > spriteBagsIdx) { "MissingSpriteBagsEvent must come after SpriteBagsUpdatedEvent" }
        assertEquals("c1", unicastClients[missingIdx])
    }
}
