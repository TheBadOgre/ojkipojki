package net.rafkos.ojkipojki.client.view.input

import java.awt.Rectangle
import kotlin.math.abs

class DragRectOverlay(val startX: Int, val startY: Int, var endX: Int = startX, var endY: Int = startY) {
    fun toRectangle(): Rectangle {
        val x = minOf(startX, endX)
        val y = minOf(startY, endY)
        val w = abs(endX - startX)
        val h = abs(endY - startY)
        return Rectangle(x, y, w, h)
    }
}
