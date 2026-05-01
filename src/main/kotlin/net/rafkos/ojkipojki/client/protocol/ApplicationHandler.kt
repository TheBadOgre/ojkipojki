package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.MainWindow
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.loader.SpriteBagDirectoryLoader
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import javax.swing.SwingUtilities

class ApplicationHandler(
    private val serverHost: String,
    private val selectionState: SelectionState,
    private val viewportState: ViewportState,
) {
    fun onSessionReady(transmitter: CommandTransmitter) {
        val debouncer = CommandDebouncer(transmitter)

        val mainWindow = MainWindow(serverHost, ClientContext.stateRepository, selectionState, viewportState, debouncer, transmitter)

        ClientContext.onTokensUpdated = {
            val tokens = ClientContext.stateRepository.findAllTokens()
            SwingUtilities.invokeLater {
                mainWindow.tokenAnimator.syncWithTokens(tokens)
                selectionState.pruneAgainst(tokens)
            }
        }
        ClientContext.onSpriteBagsUpdated = {
            SwingUtilities.invokeLater {
                mainWindow.spriteBagListPanel.refresh()
            }
        }

        SwingUtilities.invokeLater { mainWindow.isVisible = true }

        val bags = SpriteBagDirectoryLoader.loadAll()
        if (bags.isNotEmpty()) transmitter.transmit(UploadSpriteBagsCommand(bags))
    }

    fun onSessionClosed() {}
}
