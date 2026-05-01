package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.action.BoardActions
import net.rafkos.ojkipojki.client.view.icon.Icons
import net.rafkos.ojkipojki.client.view.state.SelectionState
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JToolBar

class ToolbarPanel(
    private val actions: BoardActions,
    private val selectionState: SelectionState,
    private val stateRepository: StateRepository,
) : JToolBar() {

    private val lockBtn = JButton()
    private val conditionalButtons = mutableListOf<Pair<JButton, () -> Boolean>>()

    init {
        isFloatable = false

        always(Icons.selectAll,   "Select All  (Ctrl+A)")              { actions.selectAll() }
        cond({ actions.hasSelection() },
             Icons.deselectAll, "Deselect All  (Esc)")                 { actions.deselectAll() }
        cond({ actions.hasUnlockedSelected() },
             Icons.rotate60,    "Rotate +60°  (R)")                    { actions.rotate60() }
        cond({ actions.hasUnlockedSelected() },
             Icons.bringToBack,  "Send to Back  (Ctrl+Shift+[)")       { actions.bringToBack() }
        cond({ actions.hasUnlockedSelected() },
             Icons.indexDown,   "Index −  Send Back  (Ctrl+[)")        { actions.indexDown() }
        cond({ actions.hasUnlockedSelected() },
             Icons.indexUp,     "Index +  Bring Forward  (Ctrl+])")    { actions.indexUp() }
        cond({ actions.hasUnlockedSelected() },
             Icons.bringToFront,"Bring to Front  (Ctrl+Shift+])")      { actions.bringToFront() }
        cond({ actions.hasUnlockedSelected() },
             Icons.delete,      "Delete Selected  (Del)")              { actions.delete() }
        always(Icons.refreshBags,"Reload Sprite Bags from Disk  (F5)") { actions.refreshBags() }
        addSeparator()
        cond({ actions.hasUnlockedSelected() },
             Icons.flip,    "Flip Selected Tokens  (F)")               { actions.flip() }
        cond({ actions.hasUnlockedSelected() },
             Icons.stack,   "Stack Selected Tokens")                   { actions.stack() }
        cond({ actions.hasAtLeast2UnlockedSelected() },
             Icons.shuffle, "Shuffle Selected Tokens")                 { actions.shuffle() }
        cond({ actions.hasAtLeast2UnlockedSelected() },
             Icons.spreadH, "Spread Tokens Horizontally")              { actions.spreadHorizontal() }
        cond({ actions.hasAtLeast2UnlockedSelected() },
             Icons.spreadV, "Spread Tokens Vertically")                { actions.spreadVertical() }
        cond({ actions.hasUnlockedSelected() },
             Icons.grid,    "Arrange Tokens in Grid  (G)")             { actions.arrangeGrid() }
        addSeparator()

        lockBtn.addActionListener { actions.toggleLock() }
        add(lockBtn)

        selectionState.addListener { refreshButtons() }
        refreshButtons()
    }

    fun refresh() = refreshButtons()

    private fun refreshButtons() {
        conditionalButtons.forEach { (btn, condition) -> btn.isEnabled = condition() }
        lockBtn.isEnabled = actions.hasSelection()
        val allLocked = actions.isAllSelectedLocked()
        lockBtn.icon        = if (allLocked) Icons.unlock else Icons.lock
        lockBtn.toolTipText = if (allLocked) "Unlock Selected Tokens  (L)" else "Lock Selected Tokens  (L)"
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
