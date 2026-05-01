package net.rafkos.ojkipojki.server.protocol

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.protocol.command.CommandReceiver
import net.rafkos.ojkipojki.server.protocol.event.EventTransmitter
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent
import net.rafkos.ojkipojki.shared.protocol.event.PointersUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent
import org.apache.logging.log4j.LogManager
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class ClientSessionManager : ClientConnectionListener {

    private val sessions = ConcurrentHashMap<String, ClientSession>()

    override fun onClientConnected(clientId: String, socket: Socket) {
        ServerContext.clientColorRegistry.assign(clientId)

        val transmitter = EventTransmitter(socket)
        val receiver = CommandReceiver(
            socket = socket,
            clientId = clientId,
            onDisconnected = { onClientDisconnected(clientId) },
        )
        sessions[clientId] = ClientSession(receiver, transmitter)
        receiver.start()
        log.info("Session created for client $clientId")

        ServerContext.eventBroadcastService.broadcast(SpriteBagsUpdatedEvent(ServerContext.modelRepository.findAllSpriteBags().map { it.toState() }), clientId)
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(ServerContext.modelRepository.findAllTokens().map { it.toState() }), clientId)
        ServerContext.eventBroadcastService.broadcast(PointersUpdatedEvent(ServerContext.pointerRepository.findAllExcept(clientId)), clientId)
        ServerContext.eventBroadcastService.broadcast(ConnectedClientsUpdateEvent(sessions.size))
    }

    override fun onClientDisconnected(clientId: String) {
        sessions.remove(clientId)?.receiver?.shutdown()
        ServerContext.pointerRepository.remove(clientId)
        ServerContext.clientColorRegistry.release(clientId)
        log.info("Session removed for client $clientId")

        ServerContext.eventBroadcastService.broadcast(ConnectedClientsUpdateEvent(sessions.size))
        ServerContext.eventBroadcastService.broadcastCustom { recipientId ->
            PointersUpdatedEvent(ServerContext.pointerRepository.findAllExcept(recipientId))
        }
    }

    fun getTransmitter(clientId: String): EventTransmitter? =
        sessions[clientId]?.transmitter

    fun getAllClientIds(): Set<String> = sessions.keys.toSet()

    private data class ClientSession(
        val receiver: CommandReceiver,
        val transmitter: EventTransmitter,
    )

    companion object {
        private val log = LogManager.getLogger(ClientSessionManager::class.java)
    }
}
