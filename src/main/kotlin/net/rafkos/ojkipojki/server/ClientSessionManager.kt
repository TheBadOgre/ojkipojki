package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.command.CommandDispatcher
import net.rafkos.ojkipojki.server.command.CommandReceiver
import net.rafkos.ojkipojki.server.event.EventTransmitter
import org.apache.logging.log4j.LogManager
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class ClientSessionManager(
    private val commandDispatcher: CommandDispatcher
) : ClientConnectionListener {

    private val sessions = ConcurrentHashMap<String, ClientSession>()

    override fun onClientConnected(clientId: String, socket: Socket) {
        val transmitter = EventTransmitter(socket)
        val receiver = CommandReceiver(
            socket = socket,
            commandDispatcher = commandDispatcher,
            onDisconnected = { onClientDisconnected(clientId) }
        )
        sessions[clientId] = ClientSession(receiver, transmitter)
        receiver.start()
        log.info("Session created for client $clientId")
    }

    override fun onClientDisconnected(clientId: String) {
        sessions.remove(clientId)?.receiver?.shutdown()
        log.info("Session removed for client $clientId")
    }

    fun getTransmitter(clientId: String): EventTransmitter? =
        sessions[clientId]?.transmitter

    fun getAllClientIds(): Set<String> = sessions.keys.toSet()

    private data class ClientSession(
        val receiver: CommandReceiver,
        val transmitter: EventTransmitter
    )

    companion object {
        private val log = LogManager.getLogger(ClientSessionManager::class.java)
    }
}