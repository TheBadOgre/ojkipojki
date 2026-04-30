package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.protocol.ApplicationHandler
import net.rafkos.ojkipojki.client.protocol.ClientSession
import net.rafkos.ojkipojki.client.protocol.ServerConnection
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher
import org.apache.logging.log4j.LogManager

object ClientRunner {
    private val log = LogManager.getLogger(ClientRunner::class.java)

    fun startClient(serverHost: String, serverPort: Int) {
        log.info("Starting client, connecting to $serverHost:$serverPort")

        ClientContext.eventDispatcher = EventDispatcher()

        val clientSession = ClientSession(ApplicationHandler())
        val serverConnection = ServerConnection(serverHost, serverPort, clientSession)

        serverConnection.connect()

        Runtime.getRuntime().addShutdownHook(Thread {
            serverConnection.disconnect()
        })
    }
}
