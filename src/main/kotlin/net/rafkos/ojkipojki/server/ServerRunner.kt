package net.rafkos.ojkipojki.server

import org.apache.logging.log4j.LogManager

object ServerRunner {
    val log = LogManager.getLogger(ServerRunner::class.java)

    fun startServer(serverPort: Int) {
        log.info("About to start server on port: $serverPort")

        val connectionManager = ConnectionManager(serverPort)
        val eventBroadcastService = EventBroadcastService(connectionManager)
        val clientConnectionListener = ClientConnectionListener(eventBroadcastService)
        connectionManager.startAcceptingConnections(clientConnectionListener)
    }
}