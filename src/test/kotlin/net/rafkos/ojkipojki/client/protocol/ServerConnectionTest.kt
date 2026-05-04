package net.rafkos.ojkipojki.client.protocol

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.ServerSocket
import java.net.Socket

class ServerConnectionTest {

    private val openSockets = mutableListOf<Socket>()
    private var serverSocket: ServerSocket? = null

    @AfterEach
    fun teardown() {
        openSockets.forEach { runCatching { it.close() } }
        serverSocket?.let { runCatching { it.close() } }
    }

    private fun startAcceptingServer(): Int {
        val ss = ServerSocket(0).also { serverSocket = it }
        Thread {
            try { openSockets += ss.accept() } catch (_: Exception) {}
        }.start()
        return ss.localPort
    }

    @Test
    fun `connect invokes onConnected with client socket`() {
        val port = startAcceptingServer()
        val listener = mock<ServerConnectionListener>()
        val conn = ServerConnection("127.0.0.1", port, listener)

        conn.connect()

        val captor = argumentCaptor<Socket>()
        verify(listener).onConnected(captor.capture())
        assertFalse(captor.firstValue.isClosed)
        openSockets += captor.firstValue
    }

    @Test
    fun `disconnect invokes onDisconnected and closes socket`() {
        val port = startAcceptingServer()
        val listener = mock<ServerConnectionListener>()
        val conn = ServerConnection("127.0.0.1", port, listener)

        conn.connect()

        val captor = argumentCaptor<Socket>()
        verify(listener).onConnected(captor.capture())
        val socket = captor.firstValue
        openSockets += socket

        conn.disconnect()

        verify(listener).onDisconnected()
        assertTrue(socket.isClosed)
    }

    @Test
    fun `disconnect before connect fires onDisconnected without throwing`() {
        val listener = mock<ServerConnectionListener>()
        val conn = ServerConnection("127.0.0.1", 1, listener)

        assertDoesNotThrow { conn.disconnect() }
        verify(listener).onDisconnected()
    }
}
