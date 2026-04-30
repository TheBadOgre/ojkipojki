package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.protocol.ClientSessionManager
import net.rafkos.ojkipojki.server.protocol.ConnectionManager
import net.rafkos.ojkipojki.server.protocol.command.CommandDispatcher
import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import org.apache.logging.log4j.LogManager

object ServerRunner {
    private val log = LogManager.getLogger(ServerRunner::class.java)

    fun startServer(serverPort: Int) {
        log.info("Starting server on port $serverPort")

        val connectionManager = ConnectionManager(serverPort)

        lateinit var sessionManager: ClientSessionManager

        val broadcastService = EventBroadcastService(sessionManagerProvider = { sessionManager })
        val commandDispatcher = CommandDispatcher(broadcastService)
        sessionManager = ClientSessionManager(commandDispatcher)

        connectionManager.startAcceptingConnections(sessionManager)

        Runtime.getRuntime().addShutdownHook(Thread {
            connectionManager.shutdown()
        })
    }
}
