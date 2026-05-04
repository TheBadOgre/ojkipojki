package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.action.BoardActions
import net.rafkos.ojkipojki.client.view.icon.Icons
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.shared.locale.LocaleService
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JToolBar

class ToolbarPanel(
    private val actions: BoardActions,
    private val selectionState: SelectionState,
    private val stateRepository: StateRepository,
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

        // Sprites presence
        always(Icons.refreshBags, LocaleService.get("toolbar.refreshBags"))           { actions.refreshBags() }
        cond({ actions.hasUnlockedSelected() },
            Icons.delete,      LocaleService.get("toolbar.delete"))                  { actions.delete() }

        selectionState.addListener { refreshButtons() }
        refreshButtons()
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
}
