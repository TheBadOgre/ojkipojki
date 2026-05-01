package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.view.action.BoardActions
import javax.swing.JButton
import javax.swing.JToolBar

class ToolbarPanel(private val actions: BoardActions) : JToolBar() {
    init {
        isFloatable = false
        btn("Select All")   { actions.selectAll() }
        btn("Deselect All") { actions.deselectAll() }
        btn("Rotate +60°")  { actions.rotate60() }
        btn("Index −")      { actions.indexDown() }
        btn("Index +")      { actions.indexUp() }
        btn("Delete")       { actions.delete() }
        btn("Refresh Bags") { actions.refreshBags() }
    }

    private fun btn(text: String, action: () -> Unit): JButton {
        val b = JButton(text)
        b.addActionListener { action() }
        add(b)
        return b
    }
}
