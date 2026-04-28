package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.shared.event.ModelChangedEvent
import org.apache.logging.log4j.LogManager

class ClientConnectionListener(
    private val eventBroadcastService: EventBroadcastService
) {
    
    fun onConnected(clientId: String) {
        log.info("Client $clientId connected")
        eventBroadcastService.broadcast(ModelChangedEvent(), clientId)
    }
    
    fun onDisconnected(clientId: String) {
        log.info("Client $clientId has been disconnected")
    }

    companion object {
        val log = LogManager.getLogger(ClientConnectionListener::class.java)
    }
}