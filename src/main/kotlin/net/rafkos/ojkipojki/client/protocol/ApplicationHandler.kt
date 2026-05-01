package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.MainWindow
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.loader.SpriteBagDirectoryLoader
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class ApplicationHandler(
    private val serverHost: String,
    private val selectionState: SelectionState,
    private val viewportState: ViewportState,
) {
    private var mainWindow: MainWindow? = null

    fun onSessionReady(transmitter: CommandTransmitter) {
        val debouncer = CommandDebouncer(transmitter)

        val window = MainWindow(serverHost, ClientContext.stateRepository, selectionState, viewportState, debouncer, transmitter)
        mainWindow = window

        ClientContext.onTokensUpdated = {
            val tokens = ClientContext.stateRepository.findAllTokens()
            SwingUtilities.invokeLater {
                window.tokenAnimator.syncWithTokens(tokens)
                selectionState.pruneAgainst(tokens)
                window.toolbarPanel.refresh()
            }
        }
        ClientContext.onSpriteBagsUpdated = {
            SwingUtilities.invokeLater {
                window.boardPanel.tokenRenderer.clearCache()
                window.spriteBagListPanel.refresh()
            }
        }
        ClientContext.onPointersUpdated = {
            val pointers = ClientContext.stateRepository.findAllPointers()
            SwingUtilities.invokeLater {
                window.pointerAnimator.syncWithPointers(pointers)
            }
        }
        ClientContext.onConnectedClientsUpdated = { count ->
            SwingUtilities.invokeLater {
                window.statusBarPanel.updateClientCount(count)
            }
        }

        SwingUtilities.invokeLater { window.isVisible = true }

        val bags = SpriteBagDirectoryLoader.loadAll()
        if (bags.isNotEmpty()) transmitter.transmit(UploadSpriteBagsCommand(bags))
    }

    fun onSessionClosed() {
        SwingUtilities.invokeLater {
            val win = mainWindow ?: return@invokeLater
            mainWindow = null
            if (!win.isShowing) return@invokeLater
            JOptionPane.showMessageDialog(
                win,
                "Lost connection to server at $serverHost.",
                "Disconnected",
                JOptionPane.ERROR_MESSAGE,
            )
            win.dispose()
            System.exit(1)
        }
    }
}
