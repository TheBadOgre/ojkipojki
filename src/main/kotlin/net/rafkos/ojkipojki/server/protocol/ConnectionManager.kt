package net.rafkos.ojkipojki.server.protocol

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ConnectionManager(serverPort: Int) {
    private val serverSocket: ServerSocket = ServerSocket(serverPort)
    internal val port: Int get() = serverSocket.localPort
    @Volatile private var running = true

    fun startAcceptingConnections(listener: ClientConnectionListener) {
        Thread {
            while (running) {
                try {
                    val socket = serverSocket.accept()
                    val clientId = clientIdFrom(socket)
                    log.info("Client $clientId connected")
                    listener.onClientConnected(clientId, socket)
                } catch (e: IOException) {
                    if (running) log.error("Error accepting connection: ${e.message}")
                }
            }
        }.start()
    }

    fun shutdown() {
        running = false
        try {
            serverSocket.close()
        } catch (e: IOException) {
            log.error("Error closing server socket: ${e.message}")
        }
    }

    private fun clientIdFrom(socket: Socket) =
        "${socket.inetAddress.hostAddress}:${socket.port}"

    companion object {
        private val log = LogManager.getLogger(ConnectionManager::class.java)
    }
}
