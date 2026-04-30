package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.shared.command.DummyCommand
import org.apache.logging.log4j.LogManager

object ClientRunner {
    private val log = LogManager.getLogger(ClientRunner::class.java)

    fun startClient(serverHost: String, serverPort: Int) {
        log.info("Starting client, connecting to $serverHost:$serverPort")

        val applicationHandler = object : ApplicationHandler {
            override fun onSessionReady(transmitter: CommandTransmitter) {
                log.info("Session ready — sending initial command")
                transmitter.transmit(DummyCommand("test"))
            }

            override fun onSessionClosed() {
                log.info("Session closed")
            }
        }

        val clientSession = ClientSession(applicationHandler)
        val serverConnection = ServerConnection(serverHost, serverPort, clientSession)

        serverConnection.connect()

        Runtime.getRuntime().addShutdownHook(Thread {
            serverConnection.disconnect()
        })
    }
}
