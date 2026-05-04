package net.rafkos.ojkipojki.server.protocol

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import java.net.Socket

class ConnectionManagerTest {

    private lateinit var manager: ConnectionManager

    @BeforeEach
    fun setup() {
        manager = ConnectionManager(0)
    }

    @AfterEach
    fun teardown() {
        runCatching { manager.shutdown() }
    }

    @Test
    fun `listener onClientConnected invoked when client connects`() {
        val listener = mock<ClientConnectionListener>()
        manager.startAcceptingConnections(listener)

        Socket("127.0.0.1", manager.port).use { }

        verify(listener, timeout(2000)).onClientConnected(any(), any())
    }

    @Test
    fun `shutdown rejects new connections`() {
        val port = manager.port
        manager.shutdown()

        assertThrows<Exception> {
            Socket("127.0.0.1", port)
        }
    }
}
