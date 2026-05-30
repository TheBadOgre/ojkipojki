package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.protocol.event.EventReceiver
import net.rafkos.ojkipojki.shared.protocol.command.AuthCommand
import net.rafkos.ojkipojki.shared.protocol.event.AuthResultEvent
import org.apache.logging.log4j.LogManager
import java.net.Socket

class ClientSession(
    private val lifecycleListener: SessionLifecycleListener,
    private val password: String = "",
) : ServerConnectionListener {

    private var receiver: EventReceiver? = null
    private var transmitter: CommandTransmitter? = null

    override fun onConnected(socket: Socket) {
        val tx = CommandTransmitter(socket)
        tx.transmit(AuthCommand(password))

        val rx = EventReceiver(socket, onDisconnected = { onDisconnected() })
        val authResult = rx.receive()
        if (authResult !is AuthResultEvent || !authResult.accepted) {
            runCatching { socket.close() }
            throw AuthFailedException()
        }

        transmitter = tx
        receiver = rx
        log.info("Session started — auth accepted")
        lifecycleListener.onSessionReady(tx)
        rx.start()
    }

    override fun onDisconnected() {
        receiver?.shutdown()
        receiver = null
        transmitter = null
        log.info("Session torn down")
        lifecycleListener.onSessionClosed()
    }

    companion object {
        private val log = LogManager.getLogger(ClientSession::class.java)
    }
}
