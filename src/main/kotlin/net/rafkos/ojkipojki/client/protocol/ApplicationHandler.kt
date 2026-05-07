package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.InitializationProgressDialog
import net.rafkos.ojkipojki.client.view.MainWindow
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import java.awt.Cursor
import java.awt.KeyboardFocusManager
import java.awt.KeyEventDispatcher
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.Timer

class ApplicationHandler(
    private val serverHost: String,
    private val selectionState: SelectionState,
    private val viewportState: ViewportState,
) {
    private var mainWindow: MainWindow? = null
    private var initDialog: InitializationProgressDialog? = null
    private var initDoneTimer: Timer? = null
    private var keyBlocker: KeyEventDispatcher? = null

    fun onSessionReady(transmitter: CommandTransmitter) {
        val debouncer = CommandDebouncer(transmitter)
        ClientContext.commandTransmitter = transmitter

        val window = MainWindow(serverHost, ClientContext.stateRepository, selectionState, viewportState, debouncer, transmitter)
        mainWindow = window

        val glassPane = object : JComponent() {
            init {
                isOpaque = false
                isFocusable = false
                cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                addMouseListener(object : MouseAdapter() {})
                addMouseMotionListener(object : MouseMotionAdapter() {})
            }
        }
        window.glassPane = glassPane

        ClientContext.onTokensUpdated = {
            val tokens = ClientContext.stateRepository.findAllTokens()
            SwingUtilities.invokeLater {
                window.tokenAnimator.syncWithTokens(tokens)
                selectionState.pruneAgainst(tokens)
                window.toolbarPanel.refresh()
            }
        }
        ClientContext.onTokensCountChanged = {
            SwingUtilities.invokeLater {
                window.spriteBagListPanel.rebuild()
            }
        }
        ClientContext.onSpriteBagsUpdated = {
            val sprites = ClientContext.stateRepository.findAllSprites()
            SwingUtilities.invokeLater {
                window.boardPanel.tokenRenderer.clearCache()
                val localImages = ClientContext.localSpriteBagRegistry.images()
                if (localImages.isNotEmpty()) window.boardPanel.tokenRenderer.seedImages(localImages)
                window.spriteBagListPanel.refresh()
                window.localSpriteBagListPanel.refresh()
                Thread {
                    val prewarm = window.boardPanel.tokenRenderer.buildPrewarm(sprites)
                    SwingUtilities.invokeLater { window.boardPanel.tokenRenderer.installPrewarm(prewarm) }
                }.apply { isDaemon = true; start() }
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
        ClientContext.onGameInitializationUpdate = { event ->
            SwingUtilities.invokeLater {
                val dialog = initDialog ?: return@invokeLater
                handleInitEvent(event, window, dialog)
            }
        }

        SwingUtilities.invokeLater {
            val dialog = InitializationProgressDialog(window)
            initDialog = dialog
            lockUI(window, dialog)
            window.isVisible = true
        }
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
            ClientContext.commandTransmitter = null
            System.exit(1)
        }
    }

    private fun handleInitEvent(event: GameInitializationEvent, window: MainWindow, dialog: InitializationProgressDialog) {
        initDoneTimer?.stop()
        initDoneTimer = null

        dialog.update(event.message, event.progress)

        if (event.status == GameInitializationEvent.Status.IN_PROGRESS && !dialog.isVisible) {
            lockUI(window, dialog)
        }

        if (event.status == GameInitializationEvent.Status.DONE) {
            initDoneTimer = Timer(1000) {
                dialog.isVisible = false
                unlockUI()
            }.apply { isRepeats = false; start() }
        }
    }

    private fun lockUI(window: MainWindow, dialog: InitializationProgressDialog) {
        window.glassPane.isVisible = true
        val blocker = KeyEventDispatcher { e -> e.consume(); true }
        keyBlocker = blocker
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(blocker)
        dialog.setLocationRelativeTo(window)
        dialog.isVisible = true
    }

    private fun unlockUI() {
        mainWindow?.glassPane?.isVisible = false
        keyBlocker?.let {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it)
            keyBlocker = null
        }
    }
}
