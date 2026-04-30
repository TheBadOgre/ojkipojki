package net.rafkos.ojkipojki.client

import org.apache.logging.log4j.LogManager
import java.net.Socket

class ServerConnection(
    private val host: String,
    private val port: Int,
    private val listener: ServerConnectionListener
) {
    private lateinit var socket: Socket

    fun connect() {
        socket = Socket(host, port)
        log.info("Connected to server at $host:$port")
        listener.onConnected(socket)
    }

    fun disconnect() {
        log.info("Disconnecting from server")
        if (::socket.isInitialized && !socket.isClosed) {
            socket.close()
        }
        listener.onDisconnected()
    }

    companion object {
        private val log = LogManager.getLogger(ServerConnection::class.java)
    }
}
