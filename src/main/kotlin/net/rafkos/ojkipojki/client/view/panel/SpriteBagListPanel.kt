package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.view.action.SpriteBagSpawnHandler
import net.rafkos.ojkipojki.client.view.loader.SpriteBagDirectoryLoader
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class SpriteBagListPanel(
    private val spawnHandler: SpriteBagSpawnHandler,
) : JPanel(BorderLayout()) {

    private val root      = DefaultMutableTreeNode("Sprites")
    private val treeModel = DefaultTreeModel(root)
    val tree              = JTree(treeModel)

    init {
        preferredSize = Dimension(180, 0)
        add(JLabel("Sprite Bags"), BorderLayout.NORTH)
        add(JScrollPane(tree), BorderLayout.CENTER)

        tree.isRootVisible   = false
        tree.showsRootHandles = true

        tree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(
                tree: JTree, value: Any?, selected: Boolean, expanded: Boolean,
                leaf: Boolean, row: Int, hasFocus: Boolean,
            ): Component {
                val display = when (val obj = (value as? DefaultMutableTreeNode)?.userObject) {
                    is SpriteBagId -> obj.id
                    else           -> obj?.toString() ?: ""
                }
                return super.getTreeCellRendererComponent(tree, display, selected, expanded, leaf, row, hasFocus)
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val path = tree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val bagId = node.userObject as? SpriteBagId ?: return
                spawnHandler.spawn(bagId, null)
            }
        })

        tree.dragEnabled   = true
        tree.transferHandler = object : TransferHandler() {
            override fun getSourceActions(c: JComponent) = COPY
            override fun createTransferable(c: JComponent): Transferable? {
                val t     = c as? JTree ?: return null
                val node  = t.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return null
                val bagId = node.userObject as? SpriteBagId ?: return null
                return StringSelection(bagId.id)
            }
        }
    }

    fun refresh() {
        root.removeAllChildren()
        val structure = SpriteBagDirectoryLoader.getFolderStructure()
        for ((folderName, bagIds) in structure) {
            val folderNode = DefaultMutableTreeNode(folderName)
            for (bagId in bagIds) folderNode.add(DefaultMutableTreeNode(bagId))
            root.add(folderNode)
        }
        treeModel.reload()
        for (i in 0 until tree.rowCount) tree.expandRow(i)
    }
}
