package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.shared.command.DummyCommand
import org.apache.logging.log4j.LogManager

class ServerConnectionListener(
    private val commandTransmitter: CommandTransmitter
) {

    fun onConnected() {
        log.info("Connected to server")
        commandTransmitter.transmit(DummyCommand("test"))
    }

    fun onDisconnected() {
        log.info("Disconnected from server")
    }

    companion object {
        private val log = LogManager.getLogger(ServerConnectionListener::class.java)
    }
}
