package net.rafkos.ojkipojki.server.protocol.event

import net.rafkos.ojkipojki.server.protocol.ClientSessionManager
import net.rafkos.ojkipojki.shared.protocol.event.Event
import org.apache.logging.log4j.LogManager

class EventBroadcastService(
    private val sessionManagerProvider: () -> ClientSessionManager
) {
    private val sessionManager get() = sessionManagerProvider()

    fun broadcast(event: Event) {
        sessionManager.getAllClientIds().forEach { clientId ->
            broadcast(event, clientId)
        }
    }

    fun broadcast(event: Event, clientId: String) {
        log.info("Sending ${event.javaClass.simpleName} to $clientId")
        try {
            sessionManager.getTransmitter(clientId)?.transmit(event)
        } catch (e: Exception) {
            log.error("Failed to send ${event.javaClass.simpleName} to $clientId", e)
        }
    }

    fun broadcastCustom(excludeClientId: String? = null, eventFactory: (String) -> Event) {
        sessionManager.getAllClientIds()
            .filter { it != excludeClientId }
            .forEach { clientId ->
                val event = eventFactory(clientId)
                log.info("Sending ${event.javaClass.simpleName} to $clientId")
                try {
                    sessionManager.getTransmitter(clientId)?.transmit(event)
                } catch (e: Exception) {
                    log.error("Failed to send ${event.javaClass.simpleName} to $clientId", e)
                }
            }
    }

    companion object {
        private val log = LogManager.getLogger(EventBroadcastService::class.java)
    }
}
