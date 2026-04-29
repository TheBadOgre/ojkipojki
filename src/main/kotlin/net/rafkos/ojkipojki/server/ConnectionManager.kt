package net.rafkos.ojkipojki.server

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(serverPort: Int) {
    private val serverSocket: ServerSocket = ServerSocket(serverPort)
    private val clients: ConcurrentHashMap<String, Socket> = ConcurrentHashMap()
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
                    clientConnectionListener.onConnected(clientId, clientSocket)
                } catch (e: IOException) {
                    if (running) {
                        log.error("Error accepting connection: ${e.message}")
                    }
                }
            }
        }.start()
    }

    fun getActiveClients(): Set<String> = HashSet(clients.keys)

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