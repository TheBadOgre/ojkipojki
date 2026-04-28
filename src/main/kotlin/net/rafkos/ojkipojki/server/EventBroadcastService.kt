package net.rafkos.ojkipojki.server

import org.apache.logging.log4j.LogManager
import net.rafkos.ojkipojki.shared.event.Event

class EventBroadcastService(private val connectionManager: ConnectionManager) {
    fun broadcast(event: Event) {
        connectionManager.getActiveClients().forEach { clientId ->
            broadcast(event, clientId)
        }
    }
    
    fun broadcast(event: Event, clientId: String) {
        log.info("About to send event ${event.javaClass.simpleName} to $clientId")
        try {
            connectionManager.sendMessage(clientId, event)
        } catch (e: Exception) {
            log.error("Failed to send event ${event.javaClass.simpleName} to $clientId", e)
        }
    }

    companion object {
        private val log = LogManager.getLogger(EventBroadcastService::class.java)
    }
}