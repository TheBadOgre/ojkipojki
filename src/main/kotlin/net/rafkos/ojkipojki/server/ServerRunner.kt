package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.protocol.ClientSessionManager
import net.rafkos.ojkipojki.server.protocol.ConnectionManager
import net.rafkos.ojkipojki.server.protocol.command.CommandDispatcher
import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import org.apache.logging.log4j.LogManager

object ServerRunner {
    private val log = LogManager.getLogger(ServerRunner::class.java)

    fun startServer(serverPort: Int) {
        log.info("Starting server on port $serverPort")

        ServerContext.modelRepository = ModelRepository()

        val connectionManager = ConnectionManager(serverPort)

        lateinit var sessionManager: ClientSessionManager

        ServerContext.eventBroadcastService = EventBroadcastService(sessionManagerProvider = { sessionManager })
        ServerContext.commandDispatcher = CommandDispatcher()
        sessionManager = ClientSessionManager()

        connectionManager.startAcceptingConnections(sessionManager)

        Runtime.getRuntime().addShutdownHook(Thread {
            connectionManager.shutdown()
        })
    }
}
