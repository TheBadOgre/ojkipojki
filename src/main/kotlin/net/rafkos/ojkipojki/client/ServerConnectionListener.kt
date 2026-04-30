package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.shared.command.DummyCommand
import org.apache.logging.log4j.LogManager
import java.net.Socket

class ServerConnectionListener(
    private val commandTransmitter: CommandTransmitter
) {
    private var socket: Socket? = null

    fun onConnected(socket: Socket) {
        log.info("Connected to server")
        this.socket = socket

        commandTransmitter.transmit(DummyCommand("test"))
    }

    fun onDisconnected() {
        log.info("Disconnected from server")
        socket?.close()
        socket = null
    }

    companion object {
        private val log = LogManager.getLogger(ServerConnectionListener::class.java)
    }
}
