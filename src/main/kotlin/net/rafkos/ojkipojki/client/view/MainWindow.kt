package net.rafkos.ojkipojki.client.view

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.action.BoardActions
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.action.SpriteBagSpawnHandler
import net.rafkos.ojkipojki.client.view.input.BoardMouseController
import net.rafkos.ojkipojki.client.view.input.BoardWheelController
import net.rafkos.ojkipojki.client.view.panel.BoardPanel
import net.rafkos.ojkipojki.client.view.panel.SpriteBagListPanel
import net.rafkos.ojkipojki.client.view.panel.StatusBarPanel
import net.rafkos.ojkipojki.client.view.panel.ToolbarPanel
import net.rafkos.ojkipojki.client.view.render.TokenRenderer
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.WindowConstants

class MainWindow(
    serverIp: String,
    stateRepository: StateRepository,
    selectionState: SelectionState,
    viewportState: ViewportState,
    debouncer: CommandDebouncer,
    transmitter: CommandTransmitter,
) : JFrame("ojkipojki") {

    val boardPanel: BoardPanel
    val spriteBagListPanel: SpriteBagListPanel

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        setSize(1200, 800)

        val tokenRenderer = TokenRenderer()
        boardPanel = BoardPanel(stateRepository, selectionState, viewportState, tokenRenderer)

        val spawnHandler = SpriteBagSpawnHandler(transmitter, viewportState, boardPanel)
        val actions = BoardActions(stateRepository, selectionState, transmitter, debouncer)

        spriteBagListPanel = SpriteBagListPanel(spawnHandler)

        val mouseController = BoardMouseController(boardPanel, stateRepository, selectionState, viewportState, debouncer, tokenRenderer)
        val wheelController = BoardWheelController(boardPanel, viewportState, selectionState, stateRepository, debouncer)

        boardPanel.addMouseListener(mouseController)
        boardPanel.addMouseMotionListener(mouseController)
        boardPanel.addMouseWheelListener(wheelController)
        boardPanel.transferHandler = spawnHandler.createBoardDropHandler()

        setupKeyBindings(boardPanel, actions, viewportState)

        layout = BorderLayout()
        add(ToolbarPanel(actions), BorderLayout.NORTH)
        add(spriteBagListPanel, BorderLayout.EAST)
        add(boardPanel, BorderLayout.CENTER)
        add(StatusBarPanel(serverIp), BorderLayout.SOUTH)

        setLocationRelativeTo(null)
    }

    private fun setupKeyBindings(boardPanel: BoardPanel, actions: BoardActions, viewportState: ViewportState) {
        val im = boardPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am = boardPanel.actionMap

        data class PanBinding(val key: Int, val dx: Int, val dy: Int)
        val panBindings = listOf(
            PanBinding(KeyEvent.VK_RIGHT, -1,  0),
            PanBinding(KeyEvent.VK_LEFT,   1,  0),
            PanBinding(KeyEvent.VK_DOWN,   0, -1),
            PanBinding(KeyEvent.VK_UP,     0,  1),
            PanBinding(KeyEvent.VK_D,     -1,  0),
            PanBinding(KeyEvent.VK_A,      1,  0),
            PanBinding(KeyEvent.VK_S,      0, -1),
            PanBinding(KeyEvent.VK_W,      0,  1),
        )

        for ((key, dx, dy) in panBindings) {
            val name = "pan-$key"
            im.put(KeyStroke.getKeyStroke(key, 0), name)
            am.put(name, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val step = 50.0 / viewportState.zoom
                    viewportState.offsetX += dx * step
                    viewportState.offsetY += dy * step
                    boardPanel.repaint()
                }
            })
        }

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete")
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete")
        am.put("delete", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) { actions.delete() }
        })
    }
}
