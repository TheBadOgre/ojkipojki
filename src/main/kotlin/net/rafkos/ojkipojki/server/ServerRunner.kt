package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.application.AutoSaveService
import net.rafkos.ojkipojki.server.application.ClientColorRegistry
import net.rafkos.ojkipojki.server.application.GameLoader
import net.rafkos.ojkipojki.server.application.GamePersistence
import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.application.PointerRepository
import net.rafkos.ojkipojki.server.protocol.ClientSessionManager
import net.rafkos.ojkipojki.server.protocol.ConnectionManager
import net.rafkos.ojkipojki.server.protocol.command.CommandDispatcher
import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import org.apache.logging.log4j.LogManager

object ServerRunner {
    private val log = LogManager.getLogger(ServerRunner::class.java)

    private const val AUTO_SAVE_INTERVAL_MS = 5 * 60 * 1000L

    fun startServer(serverPort: Int) {
        log.info("Starting server on port $serverPort")

        ServerContext.modelRepository = ModelRepository()
        ServerContext.pointerRepository = PointerRepository()
        ServerContext.clientColorRegistry = ClientColorRegistry()
        GameLoader.tryLoad(ServerContext.modelRepository)

        val connectionManager = ConnectionManager(serverPort)

        lateinit var sessionManager: ClientSessionManager

        ServerContext.eventBroadcastService = EventBroadcastService(sessionManagerProvider = { sessionManager })
        ServerContext.commandDispatcher = CommandDispatcher()
        sessionManager = ClientSessionManager()

        connectionManager.startAcceptingConnections(sessionManager)

        val autoSave = AutoSaveService(ServerContext.modelRepository, AUTO_SAVE_INTERVAL_MS)
        autoSave.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            autoSave.stop()
            connectionManager.shutdown()
            try {
                GamePersistence.save(ServerContext.modelRepository)
                log.info("Saved game state on exit")
            } catch (e: Exception) {
                log.error("Save on exit failed: ${e.message}")
            }
        })
    }
}
