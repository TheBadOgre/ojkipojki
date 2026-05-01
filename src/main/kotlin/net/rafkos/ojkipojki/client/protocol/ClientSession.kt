package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.protocol.event.EventReceiver
import org.apache.logging.log4j.LogManager
import java.net.Socket

class ClientSession(
    private val applicationHandler: ApplicationHandler
) : ServerConnectionListener {

    private var receiver: EventReceiver? = null
    private var transmitter: CommandTransmitter? = null

    override fun onConnected(socket: Socket) {
        transmitter = CommandTransmitter(socket)
        log.info("Session started — receiver and transmitter ready")
        applicationHandler.onSessionReady(transmitter!!)
        receiver = EventReceiver(socket, onDisconnected = { onDisconnected() }).also { it.start() }
    }

    override fun onDisconnected() {
        receiver?.shutdown()
        receiver = null
        transmitter = null
        log.info("Session torn down")
        applicationHandler.onSessionClosed()
    }

    companion object {
        private val log = LogManager.getLogger(ClientSession::class.java)
    }
}
