package net.rafkos.ojkipojki.client.view

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.action.BoardActions
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.action.PointerCommandSender
import net.rafkos.ojkipojki.client.view.action.SpriteBagSpawnHandler
import net.rafkos.ojkipojki.client.view.input.BoardMouseController
import net.rafkos.ojkipojki.client.view.input.BoardWheelController
import net.rafkos.ojkipojki.client.view.panel.BoardPanel
import net.rafkos.ojkipojki.client.view.panel.LocalSpriteBagListPanel
import net.rafkos.ojkipojki.client.view.panel.SpriteBagListPanel
import net.rafkos.ojkipojki.client.view.panel.StatusBarPanel
import net.rafkos.ojkipojki.client.view.panel.ToolbarPanel
import net.rafkos.ojkipojki.client.view.render.TokenRenderer
import net.rafkos.ojkipojki.client.view.state.PointerAnimator
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.TokenAnimator
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.AppIcon
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import javax.swing.JLabel
import javax.swing.JSlider
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.KeyStroke
import javax.swing.WindowConstants

class MainWindow(
    serverIp: String,
    stateRepository: StateRepository,
    selectionState: SelectionState,
    viewportState: ViewportState,
    debouncer: CommandDebouncer,
    transmitter: CommandTransmitter,
) : JFrame(LocaleService.get("main.window.title")) {

    val boardPanel: BoardPanel
    val spriteBagListPanel: SpriteBagListPanel
    val localSpriteBagListPanel: LocalSpriteBagListPanel
    val toolbarPanel: ToolbarPanel
    val statusBarPanel: StatusBarPanel
    val tokenAnimator: TokenAnimator = TokenAnimator()
    val pointerAnimator: PointerAnimator = PointerAnimator()

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        AppIcon.image?.let { iconImage = it }
        setSize(1200, 800)

        val tokenRenderer = TokenRenderer()
        boardPanel = BoardPanel(stateRepository, selectionState, viewportState, tokenRenderer, tokenAnimator, pointerAnimator)

        val pointerSender = PointerCommandSender(transmitter, viewportState, boardPanel)
        boardPanel.addMouseMotionListener(pointerSender)

        val spawnHandler = SpriteBagSpawnHandler(transmitter, viewportState, boardPanel, stateRepository)
        val actions = BoardActions(stateRepository, selectionState, transmitter, debouncer, tokenAnimator)

        spriteBagListPanel = SpriteBagListPanel(spawnHandler, stateRepository)
        localSpriteBagListPanel = LocalSpriteBagListPanel(stateRepository, actions::uploadLocalBag, actions::uploadAllLocal, actions::refreshBags)

        val mouseController = BoardMouseController(boardPanel, stateRepository, selectionState, viewportState, debouncer, tokenRenderer, tokenAnimator, onRmbClick = actions::flip)
        val wheelController = BoardWheelController(boardPanel, viewportState, selectionState, stateRepository, debouncer)

        boardPanel.addMouseListener(mouseController)
        boardPanel.addMouseMotionListener(mouseController)
        boardPanel.addMouseWheelListener(wheelController)
        boardPanel.transferHandler = spawnHandler.createBoardDropHandler()

        setupKeyBindings(boardPanel, actions, viewportState)

        toolbarPanel = ToolbarPanel(actions, selectionState, stateRepository)

        statusBarPanel = StatusBarPanel(serverIp)

        // Sidebar: toggle strip + sprite panel
        val toggleBtn = JButton("«")
        toggleBtn.margin = Insets(4, 2, 4, 2)
        toggleBtn.isFocusPainted = false
        toggleBtn.toolTipText = LocaleService.get("sidebar.collapse")

        val stripPanel = JPanel(BorderLayout())
        stripPanel.add(toggleBtn, BorderLayout.NORTH)

        val sidebarContainer = object : JPanel(BorderLayout()) {
            override fun getMinimumSize(): Dimension = Dimension(stripPanel.preferredSize.width, 0)
        }
        sidebarContainer.add(stripPanel, BorderLayout.WEST)
        val sidebarSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, spriteBagListPanel, localSpriteBagListPanel)
        sidebarSplit.resizeWeight = 0.66
        sidebarSplit.isContinuousLayout = true
        sidebarContainer.add(sidebarSplit, BorderLayout.CENTER)

        val sizeSlider = JSlider(24, 128, 64)
        val sliderPanel = JPanel(BorderLayout(4, 0))
        sliderPanel.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        sliderPanel.add(JLabel(LocaleService.get("spritebag.size")), BorderLayout.WEST)
        sliderPanel.add(sizeSlider, BorderLayout.CENTER)
        sidebarContainer.add(sliderPanel, BorderLayout.SOUTH)
        sizeSlider.addChangeListener {
            spriteBagListPanel.previewSize = sizeSlider.value
            localSpriteBagListPanel.previewSize = sizeSlider.value
            spriteBagListPanel.rebuild()
            localSpriteBagListPanel.rebuild()
        }

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardPanel, sidebarContainer)
        splitPane.resizeWeight = 1.0
        splitPane.dividerLocation = 1200 - 372

        var sidebarExpanded = true
        var lastDividerLocation = 1200 - 372

        toggleBtn.addActionListener {
            if (sidebarExpanded) {
                lastDividerLocation = splitPane.dividerLocation
                splitPane.dividerLocation = splitPane.width - stripPanel.width - splitPane.dividerSize
                toggleBtn.text = "»"
                toggleBtn.toolTipText = LocaleService.get("sidebar.expand")
            } else {
                splitPane.dividerLocation = lastDividerLocation
                toggleBtn.text = "«"
                toggleBtn.toolTipText = LocaleService.get("sidebar.collapse")
            }
            sidebarExpanded = !sidebarExpanded
        }

        layout = BorderLayout()
        add(toolbarPanel, BorderLayout.WEST)
        add(splitPane, BorderLayout.CENTER)
        add(statusBarPanel, BorderLayout.SOUTH)

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
            PanBinding(KeyEvent.VK_A,      1,  0),
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

        fun bind(stroke: KeyStroke, name: String, action: () -> Unit) {
            im.put(stroke, name)
            am.put(name, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) = action()
            })
        }

        val ctrl  = InputEvent.CTRL_DOWN_MASK
        val shift = InputEvent.SHIFT_DOWN_MASK

        bind(KeyStroke.getKeyStroke(KeyEvent.VK_A,             ctrl),         "selectAll")   { actions.selectAll() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,        0),            "deselectAll") { actions.deselectAll() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_R,             0),            "rotate60")    { actions.rotate60() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_D,              0),            "bringToBack") { actions.bringToBack() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_D,              ctrl),         "indexDown")   { actions.indexDown() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F,              ctrl),         "indexUp")     { actions.indexUp() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F,              0),            "bringToFront"){ actions.bringToFront() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,          0),            "flip")        { actions.flip() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_X,              0),            "shuffle")     { actions.shuffle() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_S,              0),            "stack")       { actions.stack() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_H,              0),            "spreadH")     { actions.spreadHorizontal() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_V,              0),            "spreadV")     { actions.spreadVertical() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5,             0),            "refreshBags") { actions.refreshBags() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_G,              0),            "arrangeGrid") { actions.arrangeGrid() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_L,              0),            "toggleLock")  { actions.toggleLock() }
    }
}
