package net.rafkos.ojkipojki.client.view.panel

import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout

class WrapLayout(align: Int = LEFT, hgap: Int = 0, vgap: Int = 0) : FlowLayout(align, hgap, vgap) {

    override fun preferredLayoutSize(target: Container) = layoutSize(target, true)
    override fun minimumLayoutSize(target: Container) = layoutSize(target, false)

    private fun layoutSize(target: Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            var targetWidth = target.size.width
            if (targetWidth == 0) targetWidth = target.parent?.size?.width ?: Int.MAX_VALUE

            val insets = target.insets
            val maxWidth = targetWidth - insets.left - insets.right - hgap * 2

            var height = vgap + insets.top + insets.bottom
            var rowWidth = 0
            var rowHeight = 0

            for (i in 0 until target.componentCount) {
                val m = target.getComponent(i)
                if (!m.isVisible) continue
                val d = if (preferred) m.preferredSize else m.minimumSize
                if (rowWidth > 0 && rowWidth + hgap + d.width > maxWidth) {
                    height += rowHeight + vgap
                    rowWidth = 0
                    rowHeight = 0
                }
                rowWidth += d.width + hgap
                rowHeight = maxOf(rowHeight, d.height)
            }
            height += rowHeight
            return Dimension(maxWidth, height)
        }
    }
}
