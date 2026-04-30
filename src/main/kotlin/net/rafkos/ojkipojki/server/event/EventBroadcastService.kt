package org.example.net.rafkos.ojkipojki.server.event

import net.rafkos.ojkipojki.server.ClientManager
import net.rafkos.ojkipojki.server.ConnectionManager
import net.rafkos.ojkipojki.shared.event.Event
import org.apache.logging.log4j.LogManager

class EventBroadcastService(
    private val connectionManager: ConnectionManager
) {
    private lateinit var clientManager: ClientManager

    fun setClientManager(clientManager: ClientManager) {
        this.clientManager = clientManager
    }

    fun broadcast(event: Event) {
        connectionManager.getActiveClients().forEach { clientId ->
            broadcast(event, clientId)
        }
    }

    @Synchronized
    fun broadcast(event: Event, clientId: String) {
        log.info("About to send event ${event.javaClass.simpleName} to $clientId")
        try {
            if (::clientManager.isInitialized) {
                clientManager.getTransmitter(clientId)?.transmit(event)
            }
        } catch (e: Exception) {
            log.error("Failed to send event ${event.javaClass.simpleName} to $clientId", e)
        }
    }

    companion object {
        private val log = LogManager.getLogger(EventBroadcastService::class.java)
    }
}