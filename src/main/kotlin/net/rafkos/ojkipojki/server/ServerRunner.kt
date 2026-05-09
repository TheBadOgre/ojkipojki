package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.application.AutoSaveService
import net.rafkos.ojkipojki.server.application.ClientColorRegistry
import net.rafkos.ojkipojki.server.application.GameLoader
import net.rafkos.ojkipojki.server.application.persistence.GamePersistence
import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.application.PointerRepository
import net.rafkos.ojkipojki.server.protocol.ClientSessionManager
import net.rafkos.ojkipojki.server.protocol.TokenSyncHeartbeatService
import net.rafkos.ojkipojki.server.protocol.ConnectionManager
import net.rafkos.ojkipojki.server.protocol.command.CommandDispatcher
import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import org.apache.logging.log4j.LogManager
import java.awt.GraphicsEnvironment
import java.io.File
import java.util.concurrent.Executors
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object ServerRunner {
    private val log = LogManager.getLogger(ServerRunner::class.java)

    private const val AUTO_SAVE_INTERVAL_MS = 5 * 60 * 1000L
    private const val TOKEN_SYNC_INTERVAL_MS = 5_000L

    /**
     * @param saveFile File to load from; null = start fresh (no save loaded).
     */
    fun startServer(serverPort: Int, saveFile: File? = GamePersistence.autoSaveFile) {
        log.info("Starting server on port $serverPort")

        ServerContext.modelRepository = ModelRepository()
        ServerContext.pointerRepository = PointerRepository()
        ServerContext.clientColorRegistry = ClientColorRegistry()
        ServerContext.commandExecutor = Executors.newSingleThreadExecutor()

        val saveLoaded = if (saveFile != null) {
            log.info("Starting from saved game: $saveFile")
            GameLoader.tryLoad(ServerContext.modelRepository, saveFile)
        } else {
            log.info("Starting fresh session (no save loaded)")
            true
        }

        if (!saveLoaded) {
            val msg = "Save file '${saveFile!!.name}' is corrupted and could not be loaded.\nStarting a fresh session."
            if (GraphicsEnvironment.isHeadless()) {
                log.error(msg)
            } else {
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(null, msg, "Corrupted Save File", JOptionPane.WARNING_MESSAGE)
                }
            }
        }

        val connectionManager = ConnectionManager(serverPort)
        lateinit var sessionManager: ClientSessionManager
        ServerContext.eventBroadcastService = EventBroadcastService(sessionManagerProvider = { sessionManager })
        ServerContext.commandDispatcher = CommandDispatcher()
        sessionManager = ClientSessionManager()
        connectionManager.startAcceptingConnections(sessionManager)

        val autoSave = AutoSaveService(ServerContext.modelRepository, AUTO_SAVE_INTERVAL_MS)
        autoSave.start()

        val tokenSyncHeartbeat = TokenSyncHeartbeatService(
            ServerContext.modelRepository, ServerContext.eventBroadcastService, TOKEN_SYNC_INTERVAL_MS
        )
        tokenSyncHeartbeat.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            autoSave.stop()
            tokenSyncHeartbeat.stop()
            connectionManager.shutdown()
            ServerContext.commandExecutor.shutdown()
            try {
                val timestamped = GamePersistence.timestampedAutoSaveFile()
                GamePersistence.save(ServerContext.modelRepository, timestamped)
                GamePersistence.save(ServerContext.modelRepository)
                log.info("Saved game state on exit to '${timestamped.name}' and 'autosave.sav'")
            } catch (e: Exception) {
                log.error("Save on exit failed: ${e.message}")
            }
        })
    }
}
