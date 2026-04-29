package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.event.EventDispatcher
import net.rafkos.ojkipojki.client.event.EventReceiver
import net.rafkos.ojkipojki.shared.command.DummyCommand
import org.apache.logging.log4j.LogManager
import java.net.Socket

object ClientRunner {
    private val log = LogManager.getLogger(ClientRunner::class.java)

    fun startClient(serverHost: String, serverPort: Int) {
        log.info("About to start client")

        val socket = Socket(serverHost, serverPort)
        val eventDispatcher = EventDispatcher()
        val eventReceiver = EventReceiver(socket, eventDispatcher)
        val commandTransmitter = CommandTransmitter(socket)

        eventReceiver.start()

        // TODO remove
        commandTransmitter.transmit(DummyCommand("test"))

        Runtime.getRuntime().addShutdownHook(Thread {
            socket.close()
        })
    }
}