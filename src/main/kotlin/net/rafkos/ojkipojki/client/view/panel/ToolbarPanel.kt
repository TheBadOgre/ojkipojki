package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.view.action.BoardActions
import net.rafkos.ojkipojki.client.view.icon.Icons
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JToolBar

class ToolbarPanel(private val actions: BoardActions) : JToolBar() {
    init {
        isFloatable = false
        iconBtn(Icons.selectAll,   "Select All")            { actions.selectAll() }
        iconBtn(Icons.deselectAll, "Deselect All")          { actions.deselectAll() }
        iconBtn(Icons.rotate60,    "Rotate +60°")           { actions.rotate60() }
        iconBtn(Icons.indexDown,   "Index −  (send back)")  { actions.indexDown() }
        iconBtn(Icons.indexUp,     "Index +  (bring forward)") { actions.indexUp() }
        iconBtn(Icons.delete,      "Delete selected")       { actions.delete() }
        iconBtn(Icons.refreshBags, "Reload sprite bags from disk") { actions.refreshBags() }
        addSeparator()
        iconBtn(Icons.flip,    "Flip selected tokens")                 { actions.flip() }
        iconBtn(Icons.stack,   "Stack selected tokens")                { actions.stack() }
        iconBtn(Icons.shuffle, "Shuffle selected tokens")              { actions.shuffle() }
        iconBtn(Icons.spreadH, "Spread selected tokens horizontally")  { actions.spreadHorizontal() }
        iconBtn(Icons.spreadV, "Spread selected tokens vertically")    { actions.spreadVertical() }
        iconBtn(Icons.grid,    "Arrange selected tokens in a grid")    { actions.arrangeGrid() }
    }

    private fun iconBtn(icon: ImageIcon, tooltip: String, action: () -> Unit): JButton {
        val b = JButton(icon)
        b.toolTipText = tooltip
        b.addActionListener { action() }
        add(b)
        return b
    }
}
