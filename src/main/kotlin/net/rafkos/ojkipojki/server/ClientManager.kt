package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.command.CommandDispatcher
import net.rafkos.ojkipojki.server.command.CommandReceiver
import net.rafkos.ojkipojki.server.event.EventTransmitter
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class ClientManager(
    private val commandDispatcher: CommandDispatcher
) {
    private val clients = ConcurrentHashMap<String, ClientContext>()

    fun addClient(clientId: String, socket: Socket) {
        val transmitter = EventTransmitter(socket)
        val receiver = CommandReceiver(socket, commandDispatcher)
        clients[clientId] = ClientContext(receiver, transmitter)
        receiver.start()
        log.info("Client $clientId added with receiver and transmitter")
    }

    fun removeClient(clientId: String) {
        clients.remove(clientId)?.receiver?.shutdown()
    }

    fun getTransmitter(clientId: String): EventTransmitter? = clients[clientId]?.transmitter

    private data class ClientContext(
        val receiver: CommandReceiver,
        val transmitter: EventTransmitter
    )

    companion object {
        private val log = org.apache.logging.log4j.LogManager.getLogger(ClientManager::class.java)
    }
}
