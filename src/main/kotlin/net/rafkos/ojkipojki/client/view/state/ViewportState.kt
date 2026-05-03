package net.rafkos.ojkipojki.client.view.state

import net.rafkos.ojkipojki.shared.domain.Position
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D

class ViewportState {
    var zoom: Double = 1.0 / 3.0
    var offsetX: Double = 0.0
    var offsetY: Double = 0.0

    fun worldToScreen(p: Position, panelW: Int, panelH: Int): Point2D.Double {
        val sx = (p.x + offsetX) * zoom + panelW / 2.0
        val sy = (p.y + offsetY) * zoom + panelH / 2.0
        return Point2D.Double(sx, sy)
    }

    fun screenToWorld(p: Point2D, panelW: Int, panelH: Int): Position {
        val wx = ((p.x - panelW / 2.0) / zoom - offsetX).toInt()
        val wy = ((p.y - panelH / 2.0) / zoom - offsetY).toInt()
        return Position(wx, wy)
    }

    fun screenDeltaToWorld(dx: Double, dy: Double): Pair<Int, Int> =
        Pair((dx / zoom).toInt(), (dy / zoom).toInt())

    fun affineTransform(panelW: Int, panelH: Int): AffineTransform {
        val at = AffineTransform()
        at.translate(panelW / 2.0, panelH / 2.0)
        at.scale(zoom, zoom)
        at.translate(offsetX, offsetY)
        return at
    }
}
