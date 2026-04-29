package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.command.CommandDispatcher
import net.rafkos.ojkipojki.server.model.Model
import org.apache.logging.log4j.LogManager
import org.example.net.rafkos.ojkipojki.server.event.EventBroadcastService

object ServerRunner {
    private val log = LogManager.getLogger(ServerRunner::class.java)

    fun startServer(serverPort: Int) {
        log.info("About to start server on port: $serverPort")

        val model = Model()
        val connectionManager = ConnectionManager(serverPort)
        val eventBroadcastService = EventBroadcastService(connectionManager)
        val commandDispatcher = CommandDispatcher(model, eventBroadcastService)
        val clientManager = ClientManager(commandDispatcher)

        eventBroadcastService.setClientManager(clientManager)

        val clientConnectionListener = ClientConnectionListener(clientManager)
        connectionManager.startAcceptingConnections(clientConnectionListener)

        Runtime.getRuntime().addShutdownHook(Thread {
            connectionManager.shutdown()
        })
    }
}