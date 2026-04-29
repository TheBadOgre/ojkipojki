package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.shared.event.ModelChangedEvent
import org.apache.logging.log4j.LogManager
import java.net.Socket

class ClientConnectionListener(
    private val clientManager: ClientManager
) {
    fun onConnected(clientId: String, socket: Socket) {
        log.info("Client $clientId connected")
        clientManager.addClient(clientId, socket)

        // TODO change to something else
        clientManager.getTransmitter(clientId)?.transmit(ModelChangedEvent())
    }

    fun onDisconnected(clientId: String) {
        log.info("Client $clientId has been disconnected")
        clientManager.removeClient(clientId)
    }

    companion object {
        val log = LogManager.getLogger(ClientConnectionListener::class.java)
    }
}