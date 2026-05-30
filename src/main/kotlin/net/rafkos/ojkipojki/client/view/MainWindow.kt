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
import net.rafkos.ojkipojki.client.view.state.BackgroundColorState
import net.rafkos.ojkipojki.client.view.state.PointerAnimator
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.TokenAnimator
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.AppIcon
import net.rafkos.ojkipojki.shared.locale.LocaleService
import net.rafkos.ojkipojki.client.view.icon.Icons
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
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
        minimumSize = Dimension(1024, 960)

        val tokenRenderer = TokenRenderer()
        val backgroundColorState = BackgroundColorState()
        boardPanel = BoardPanel(stateRepository, selectionState, viewportState, tokenRenderer, tokenAnimator, pointerAnimator, backgroundColorState)

        val pointerSender = PointerCommandSender(transmitter, viewportState, boardPanel)
        boardPanel.addMouseMotionListener(pointerSender)

        val spawnHandler = SpriteBagSpawnHandler(transmitter, viewportState, boardPanel, stateRepository)
        val actions = BoardActions(
            stateRepository, selectionState, transmitter, debouncer, tokenAnimator,
            onLocalRegistryReloaded = { localSpriteBagListPanel.refresh() },
        )

        spriteBagListPanel = SpriteBagListPanel(spawnHandler, stateRepository)
        localSpriteBagListPanel = LocalSpriteBagListPanel(stateRepository, actions::uploadLocalBag)

        val mouseController = BoardMouseController(boardPanel, stateRepository, selectionState, viewportState, debouncer, tokenRenderer, tokenAnimator, onRmbClick = actions::flip)
        val wheelController = BoardWheelController(boardPanel, viewportState, selectionState, stateRepository, debouncer)

        boardPanel.addMouseListener(mouseController)
        boardPanel.addMouseMotionListener(mouseController)
        boardPanel.addMouseWheelListener(wheelController)
        boardPanel.transferHandler = spawnHandler.createBoardDropHandler()

        setupKeyBindings(boardPanel, actions, viewportState)

        toolbarPanel = ToolbarPanel(actions, selectionState, backgroundColorState)

        statusBarPanel = StatusBarPanel(serverIp)

        // Sidebar: toggle strip + sprite panel
        val toggleBtn = JButton("»")
        toggleBtn.margin = Insets(4, 2, 4, 2)
        toggleBtn.isFocusPainted = false
        toggleBtn.toolTipText = LocaleService.get("sidebar.collapse")

        val stripPanel = JPanel(BorderLayout())
        stripPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, javax.swing.UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        )
        stripPanel.add(toggleBtn, BorderLayout.NORTH)

        var sidebarCollapsed = false
        val sidebarContainer = object : JPanel(BorderLayout()) {
            override fun getMinimumSize(): Dimension =
                if (sidebarCollapsed) Dimension(stripPanel.preferredSize.width, 0)
                else Dimension(250, 0)
        }
        sidebarContainer.add(stripPanel, BorderLayout.WEST)

        val uploadAllBtn = JButton(Icons.uploadAllBags)
        uploadAllBtn.toolTipText = LocaleService.get("spritebag.uploadAll")
        uploadAllBtn.isFocusPainted = false
        uploadAllBtn.margin = Insets(2, 2, 2, 2)
        uploadAllBtn.addActionListener { actions.uploadAllLocal() }

        val reloadBtn = JButton(Icons.refreshBags)
        reloadBtn.toolTipText = LocaleService.get("toolbar.refreshBags")
        reloadBtn.isFocusPainted = false
        reloadBtn.margin = Insets(2, 2, 2, 2)
        reloadBtn.addActionListener { actions.refreshBags() }

        val localButtonBar = JPanel(FlowLayout(FlowLayout.LEFT, 2, 2))
        localButtonBar.isOpaque = false
        localButtonBar.add(uploadAllBtn)
        localButtonBar.add(reloadBtn)

        val localWrapper = JPanel(BorderLayout())
        localWrapper.add(localButtonBar, BorderLayout.NORTH)
        localWrapper.add(localSpriteBagListPanel, BorderLayout.CENTER)

        val sidebarSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, spriteBagListPanel, localWrapper)
        sidebarSplit.resizeWeight = 0.66
        sidebarSplit.isContinuousLayout = true
        sidebarSplit.border = null
        sidebarSplit.dividerSize = 4

        val sizeSlider = JSlider(24, 128, 64)
        val sliderPanel = JPanel(BorderLayout(4, 0))
        sliderPanel.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        sliderPanel.add(JLabel(LocaleService.get("spritebag.size")), BorderLayout.WEST)
        sliderPanel.add(sizeSlider, BorderLayout.CENTER)
        sizeSlider.addChangeListener {
            spriteBagListPanel.previewSize = sizeSlider.value
            localSpriteBagListPanel.previewSize = sizeSlider.value
            spriteBagListPanel.rebuild()
            localSpriteBagListPanel.rebuild()
        }

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(sidebarSplit, BorderLayout.CENTER)
        rightPanel.add(sliderPanel, BorderLayout.SOUTH)
        sidebarContainer.add(rightPanel, BorderLayout.CENTER)

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardPanel, sidebarContainer)
        splitPane.resizeWeight = 1.0
        splitPane.dividerLocation = 1200 - 372

        var sidebarExpanded = true
        var lastDividerLocation = 1200 - 372

        toggleBtn.addActionListener {
            if (sidebarExpanded) {
                lastDividerLocation = splitPane.dividerLocation
                sidebarCollapsed = true
                rightPanel.isVisible = false
                sidebarContainer.revalidate()
                splitPane.dividerLocation = splitPane.width - stripPanel.width - splitPane.dividerSize
                toggleBtn.text = "«"
                toggleBtn.toolTipText = LocaleService.get("sidebar.expand")
            } else {
                sidebarCollapsed = false
                rightPanel.isVisible = true
                sidebarContainer.revalidate()
                splitPane.dividerLocation = lastDividerLocation
                toggleBtn.text = "»"
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
                    boardPanel.markInteracting()
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
