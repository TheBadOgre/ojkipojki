package net.rafkos.ojkipojki.client

import java.io.ObjectInputStream
import java.net.Socket
import org.apache.logging.log4j.LogManager
import net.rafkos.ojkipojki.shared.event.Event

class ConnectionManager(
    private val clientEventListener: ClientEventListener,
    private val clientConnectionStatusListener: ClientConnectionStatusListener
) {
    private var socket: Socket? = null
    private var connection: Connection? = null
    private var listenerThread: Thread? = null

    @Volatile private var running = false

    private val log = LogManager.getLogger(ConnectionManager::class.java)

    @Throws(ConnectionException::class)
    fun connect(serverHost: String, serverPort: Int): Connection {
        if (connection != null && connection!!.isActive()) {
            throw ConnectionException.AlreadyConnectedException()
        }

        try {
            socket = Socket(serverHost, serverPort)
            connection = ConnectionImpl(socket!!)
            running = true
            log.info("Connected to $serverHost:$serverPort")

            clientConnectionStatusListener.onConnected(connection!!)

            val inputStream = ObjectInputStream(socket!!.getInputStream())
            listenerThread = Thread {
                while (running) {
                    try {
                        val obj = inputStream.readObject()
                        if (obj is Event) {
                            clientEventListener.onEvent(obj)
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
                clientConnectionStatusListener.onDisconnected()
            }.apply { start() }

        } catch (e: Exception) {
            throw ConnectionException.UnableToConnectException()
        }

        return connection!!
    }

    fun getConnection(): Connection? = connection

    fun shutdown() {
        running = false
        try {
            socket?.close()
        } catch (e: Exception) {
            log.error("Error closing socket", e)
        }
        connection = null
        listenerThread = null
    }

    private inner class ConnectionImpl(private val clientSocket: Socket) : Connection {
        override fun isActive(): Boolean = clientSocket.isConnected && !clientSocket.isClosed
    }
}