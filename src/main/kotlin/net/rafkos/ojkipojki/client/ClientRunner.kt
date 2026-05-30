package net.rafkos.ojkipojki.client

import kotlinx.coroutines.runBlocking
import net.rafkos.ojkipojki.client.application.ClientStateNotifier
import net.rafkos.ojkipojki.client.application.LocalSpriteBagRegistry
import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.ClientSession
import net.rafkos.ojkipojki.client.protocol.ServerConnection
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher
import net.rafkos.ojkipojki.client.view.MainWindowController
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import org.apache.logging.log4j.LogManager

object ClientRunner {
    private val log = LogManager.getLogger(ClientRunner::class.java)

    fun startClient(serverHost: String, serverPort: Int, password: String = "") {
        log.info("Starting client, connecting to $serverHost:$serverPort")

        ClientContext.localSpriteBagRegistry = LocalSpriteBagRegistry().also { runBlocking { it.reload() } }
        ClientContext.eventDispatcher = EventDispatcher()
        ClientContext.stateRepository = StateRepository()
        ClientContext.notifier = ClientStateNotifier()

        val selectionState = SelectionState()
        val viewportState  = ViewportState()

        val controller = MainWindowController(serverHost, selectionState, viewportState)
        val clientSession = ClientSession(controller, password)
        val serverConnection = ServerConnection(serverHost, serverPort, clientSession)

        serverConnection.connect()

        Runtime.getRuntime().addShutdownHook(Thread {
            serverConnection.disconnect()
        })
    }
}
