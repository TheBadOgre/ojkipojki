package net.rafkos.ojkipojki.server

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ConnectionManager(serverPort: Int) {
    private val serverSocket: ServerSocket = ServerSocket(serverPort)
    private val clients: ConcurrentMap<String, Socket> = ConcurrentHashMap()
    private var clientConnectionListener: ClientConnectionListener? = null
    @Volatile private var running = true

    fun startAcceptingConnections(clientConnectionListener: ClientConnectionListener) {
        this.clientConnectionListener = clientConnectionListener
        Thread {
            while (running) {
                try {
                    val clientSocket = serverSocket.accept()
                    val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                    clients[clientId] = clientSocket
                    clientConnectionListener.onConnected(clientId)
                } catch (e: IOException) {
                    if (running) {
                        log.error("Error accepting connection: ${e.message}")
                    }
                }
            }
        }.start()
    }

    fun getActiveClients(): Set<String> {
        return HashSet(clients.keys)
    }

    @Throws(IOException::class)
    fun sendMessage(clientId: String, message: Any) {
        val clientSocket: Socket = clients[clientId] ?: throw IllegalArgumentException("Client not found: $clientId")
        try {
            val out = ObjectOutputStream(clientSocket.getOutputStream())
            out.writeObject(message)
            out.flush()
        } catch (e: IOException) {
            if (!isConnected(clientSocket)) {
                disconnectClient(clientId)
            } else {
                throw e
            }
        }
    }

    private fun isConnected(socket: Socket): Boolean {
        return !socket.isClosed && socket.isConnected && !socket.isInputShutdown
    }

    private fun disconnectClient(clientId: String) {
        clients.remove(clientId)
        clientConnectionListener?.onDisconnected(clientId)
    }

    fun shutdown() {
        running = false
        try {
            serverSocket.close()
        } catch (e: IOException) {
            log.error("Error closing server socket: ${e.message}")
        }
        val clientIds = clients.keys.toList()
        for (clientId in clientIds) {
            try {
                clients[clientId]?.close()
                disconnectClient(clientId)
            } catch (e: IOException) {
                log.error("Error closing client socket: ${e.message}")
            }
        }
    }

    companion object {
        private val log = LogManager.getLogger(ConnectionManager::class.java)
    }
}