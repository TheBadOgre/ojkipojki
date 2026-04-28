package net.rafkos.ojkipojki.client

import org.apache.logging.log4j.LogManager

object ClientRunner {
    val log = LogManager.getLogger(ClientRunner::class.java)

    fun startClient(serverHost: String, serverPort: Int) {
        log.info("About to start client")

        val clientEventListener = ClientEventListener()
        val clientConnectionStatusListener = ClientConnectionStatusListener()
        val connectionManager = ConnectionManager(clientEventListener, clientConnectionStatusListener)
        connectionManager.connect(serverHost, serverPort)
    }
}