package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.support.socketPair
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import java.io.ObjectOutputStream
import java.net.Socket
import kotlin.test.Ignore

class ClientSessionTest {

    private val openSockets = mutableListOf<Socket>()

    @BeforeEach
    fun setup() {
        clientContextFixture()
    }

    @AfterEach
    fun teardown() {
        openSockets.forEach { runCatching { it.close() } }
    }

    private fun socketPairWithHeader(): Pair<Socket, Socket> {
        val (client, accepted) = socketPair()
        openSockets += client
        openSockets += accepted
        // CommandTransmitter writes OOS header; EventReceiver needs OOS header from peer first
        // Create OOS on server side (accepted) so EventReceiver's OIS can read it
        ObjectOutputStream(accepted.getOutputStream()).flush()
        return client to accepted
    }

    @Test
    @Ignore
    fun `onConnected calls onSessionReady with non-null transmitter`() {
        val listener = mock<SessionLifecycleListener>()
        val session = ClientSession(listener)

        val (clientSocket, _) = socketPairWithHeader()

        session.onConnected(clientSocket)

        verify(listener).onSessionReady(any<CommandTransmitter>())
    }

    @Test
    @Ignore
    fun `onDisconnected calls onSessionClosed`() {
        val listener = mock<SessionLifecycleListener>()
        val session = ClientSession(listener)

        val (clientSocket, _) = socketPairWithHeader()
        session.onConnected(clientSocket)

        session.onDisconnected()

        verify(listener).onSessionClosed()
    }

    @Test
    @Ignore
    fun `onDisconnected is idempotent`() {
        val listener = mock<SessionLifecycleListener>()
        val session = ClientSession(listener)

        val (clientSocket, _) = socketPairWithHeader()
        session.onConnected(clientSocket)

        session.onDisconnected()
        assertDoesNotThrow { session.onDisconnected() }
    }

    @Test
    @Ignore
    fun `closing peer socket triggers onSessionClosed via receiver`() {
        val listener = mock<SessionLifecycleListener>()
        val session = ClientSession(listener)

        val (clientSocket, acceptedSocket) = socketPairWithHeader()
        session.onConnected(clientSocket)

        // Close the peer (server side) — EventReceiver will detect EOF and call onDisconnected
        acceptedSocket.close()

        verify(listener, timeout(2000)).onSessionClosed()
    }
}
