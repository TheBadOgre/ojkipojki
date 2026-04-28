package net.rafkos.ojkipojki.client

import org.apache.logging.log4j.LogManager

class ClientConnectionStatusListener {

    fun onConnected(connection: Connection) {
        log.info("Connected to server")
    }

    fun onDisconnected() {
        log.info("Disconnected from server")
    }

    companion object {
        val log = LogManager.getLogger(ClientConnectionStatusListener::class.java)
    }
}