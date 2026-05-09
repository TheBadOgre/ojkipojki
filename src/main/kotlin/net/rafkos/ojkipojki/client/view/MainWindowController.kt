package net.rafkos.ojkipojki.client.view

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.application.ClientStateListener
import net.rafkos.ojkipojki.client.protocol.SessionLifecycleListener
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
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

class MainWindowController(
    private val serverHost: String,
    private val selectionState: SelectionState,
    private val viewportState: ViewportState,
) : SessionLifecycleListener, ClientStateListener {

    private var mainWindow: MainWindow? = null
    private var initDialog: InitializationProgressDialog? = null
    private var initDoneTimer: Timer? = null
    private var keyBlocker: KeyEventDispatcher? = null

    override fun onSessionReady(transmitter: CommandTransmitter) {
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

        ClientContext.notifier.addListener(this)

        SwingUtilities.invokeLater {
            val dialog = InitializationProgressDialog(window)
            initDialog = dialog
            lockUI(window, dialog)
            window.isVisible = true
        }
    }

    override fun onSessionClosed() {
        ClientContext.notifier.removeListener(this)
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

    override fun onTokensUpdated() {
        val tokens = ClientContext.stateRepository.findAllTokens()
        val win = mainWindow ?: return
        SwingUtilities.invokeLater {
            win.tokenAnimator.syncWithTokens(tokens)
            selectionState.pruneAgainst(tokens)
            win.toolbarPanel.refresh()
        }
    }

    override fun onTokensCountChanged() {
        val win = mainWindow ?: return
        SwingUtilities.invokeLater {
            win.spriteBagListPanel.rebuild()
        }
    }

    override fun onSpriteBagsUpdated() {
        val sprites = ClientContext.stateRepository.findAllSprites()
        val win = mainWindow ?: return
        SwingUtilities.invokeLater {
            win.boardPanel.tokenRenderer.clearCache()
            val localImages = ClientContext.localSpriteBagRegistry.images()
            if (localImages.isNotEmpty()) win.boardPanel.tokenRenderer.seedImages(localImages)
            win.spriteBagListPanel.refresh()
            win.localSpriteBagListPanel.refresh()
            Thread {
                val prewarm = win.boardPanel.tokenRenderer.buildPrewarm(sprites)
                SwingUtilities.invokeLater { win.boardPanel.tokenRenderer.installPrewarm(prewarm) }
            }.apply { isDaemon = true; start() }
        }
    }

    override fun onPointersUpdated() {
        val pointers = ClientContext.stateRepository.findAllPointers()
        val win = mainWindow ?: return
        SwingUtilities.invokeLater {
            win.pointerAnimator.syncWithPointers(pointers)
        }
    }

    override fun onConnectedClientsUpdated(count: Int) {
        val win = mainWindow ?: return
        SwingUtilities.invokeLater {
            win.statusBarPanel.updateClientCount(count)
        }
    }

    override fun onGameInitializationUpdate(event: GameInitializationEvent) {
        SwingUtilities.invokeLater {
            val dialog = initDialog ?: return@invokeLater
            val win = mainWindow ?: return@invokeLater
            handleInitEvent(event, win, dialog)
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
