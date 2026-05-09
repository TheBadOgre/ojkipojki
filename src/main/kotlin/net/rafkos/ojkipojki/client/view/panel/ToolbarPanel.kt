package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.action.BoardActions
import net.rafkos.ojkipojki.client.view.icon.Icons
import net.rafkos.ojkipojki.client.view.state.BackgroundColorState
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.Box
import javax.swing.ButtonGroup
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JToggleButton
import javax.swing.JToolBar

class ToolbarPanel(
    private val actions: BoardActions,
    private val selectionState: SelectionState,
    private val stateRepository: StateRepository,
    private val backgroundColorState: BackgroundColorState,
) : JToolBar(JToolBar.VERTICAL) {

    private val lockBtn = JButton()
    private val conditionalButtons = mutableListOf<Pair<JButton, () -> Boolean>>()

    init {
        isFloatable = false

        // Selection
        always(Icons.selectAll,   LocaleService.get("toolbar.selectAll"))              { actions.selectAll() }
        cond({ actions.hasSelection() },
             Icons.deselectAll, LocaleService.get("toolbar.deselectAll"))             { actions.deselectAll() }
        addSeparator()

        // Ordering / depth
        cond({ actions.hasUnlockedSelected() },
             Icons.bringToBack,  LocaleService.get("toolbar.bringToBack"))            { actions.bringToBack() }
        cond({ actions.hasUnlockedSelected() },
             Icons.indexDown,   LocaleService.get("toolbar.indexDown"))               { actions.indexDown() }
        cond({ actions.hasUnlockedSelected() },
             Icons.indexUp,     LocaleService.get("toolbar.indexUp"))                 { actions.indexUp() }
        cond({ actions.hasUnlockedSelected() },
             Icons.bringToFront, LocaleService.get("toolbar.bringToFront"))           { actions.bringToFront() }
        addSeparator()

        // Actions
        cond({ actions.hasUnlockedSelected() },
             Icons.rotate60,    LocaleService.get("toolbar.rotate60"))                { actions.rotate60() }
        cond({ actions.hasUnlockedSelected() },
             Icons.flip,    LocaleService.get("toolbar.flip"))                        { actions.flip() }
        cond({ actions.hasAtLeast2UnlockedSelected() },
             Icons.shuffle, LocaleService.get("toolbar.shuffle"))                     { actions.shuffle() }
        addSeparator()

        // Layout
        cond({ actions.hasUnlockedSelected() },
            Icons.stack,   LocaleService.get("toolbar.stack"))                        { actions.stack() }
        cond({ actions.hasAtLeast2UnlockedSelected() },
             Icons.spreadH, LocaleService.get("toolbar.spreadH"))                     { actions.spreadHorizontal() }
        cond({ actions.hasAtLeast2UnlockedSelected() },
             Icons.spreadV, LocaleService.get("toolbar.spreadV"))                     { actions.spreadVertical() }
        cond({ actions.hasUnlockedSelected() },
             Icons.grid,    LocaleService.get("toolbar.arrangeGrid"))                 { actions.arrangeGrid() }
        addSeparator()

        // Lock
        lockBtn.addActionListener { actions.toggleLock() }
        add(lockBtn)
        addSeparator()

        cond({ actions.hasUnlockedSelected() },
            Icons.delete,      LocaleService.get("toolbar.delete"))                  { actions.delete() }

        selectionState.addListener { refreshButtons() }
        refreshButtons()

        // Background color picker — pushed to the bottom
        add(Box.createVerticalGlue())
        addSeparator()

        val bgColors = listOf(
            Color(110,  98,  82) to LocaleService.get("toolbar.bg.darkBeige"),
            Color(176, 164, 145) to LocaleService.get("toolbar.bg.beige"),
            Color(224, 220, 212) to LocaleService.get("toolbar.bg.softWhite"),
            Color(128, 128, 124) to LocaleService.get("toolbar.bg.neutralGrey"),
            Color( 58,  58,  56) to LocaleService.get("toolbar.bg.darkGrey"),
        )
        val bgGroup = ButtonGroup()
        for ((color, tooltip) in bgColors) {
            val btn = JToggleButton(SwatchIcon(color))
            btn.toolTipText = tooltip
            btn.margin = Insets(3, 3, 3, 3)
            btn.isSelected = color == backgroundColorState.color
            bgGroup.add(btn)
            btn.addActionListener { backgroundColorState.setColor(color) }
            add(btn)
        }
    }

    fun refresh() = refreshButtons()

    private fun refreshButtons() {
        conditionalButtons.forEach { (btn, condition) -> btn.isEnabled = condition() }
        lockBtn.isEnabled = actions.hasSelection()
        val allLocked = actions.isAllSelectedLocked()
        lockBtn.icon        = if (allLocked) Icons.unlock else Icons.lock
        lockBtn.toolTipText = if (allLocked) LocaleService.get("toolbar.unlock") else LocaleService.get("toolbar.lock")
    }

    private fun always(icon: ImageIcon, tooltip: String, action: () -> Unit): JButton {
        val b = JButton(icon)
        b.toolTipText = tooltip
        b.addActionListener { action() }
        add(b)
        return b
    }

    private fun cond(condition: () -> Boolean, icon: ImageIcon, tooltip: String, action: () -> Unit): JButton {
        val b = JButton(icon)
        b.toolTipText = tooltip
        b.addActionListener { action() }
        add(b)
        conditionalButtons.add(b to condition)
        return b
    }

    private class SwatchIcon(private val c: Color, private val size: Int = 20) : Icon {
        override fun getIconWidth()  = size
        override fun getIconHeight() = size
        override fun paintIcon(comp: Component, g: Graphics, x: Int, y: Int) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val pad = 2
            g2.color = c
            g2.fillRoundRect(x + pad, y + pad, size - pad * 2, size - pad * 2, 4, 4)
            g2.color = Color(0, 0, 0, 100)
            g2.drawRoundRect(x + pad, y + pad, size - pad * 2 - 1, size - pad * 2 - 1, 4, 4)
        }
    }
}
