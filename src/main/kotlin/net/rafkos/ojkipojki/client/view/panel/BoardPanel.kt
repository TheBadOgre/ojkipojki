package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.input.DragRectOverlay
import net.rafkos.ojkipojki.client.view.render.TokenRenderer
import net.rafkos.ojkipojki.client.view.state.PointerAnimator
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.TokenAnimator
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.domain.Pointer
import net.rafkos.ojkipojki.shared.domain.Token
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class BoardPanel(
    val stateRepository: StateRepository,
    val selectionState: SelectionState,
    val viewportState: ViewportState,
    val tokenRenderer: TokenRenderer,
    val tokenAnimator: TokenAnimator,
    val pointerAnimator: PointerAnimator,
) : JPanel() {

    var dragRectOverlay: DragRectOverlay? = null
    private var lastTokens: List<Token> = emptyList()
    private var lastPointers: List<Pointer> = emptyList()

    init {
        background = Color(35, 35, 35)
        preferredSize = Dimension(800, 600)
        isFocusable = true

        Timer(16) {
            lastTokens = stateRepository.findAllTokens()
            tokenAnimator.tick(lastTokens)
            lastPointers = stateRepository.findAllPointers()
            pointerAnimator.tick(lastPointers)
            repaint()
        }.start()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

        val savedTransform = g2.transform
        g2.transform(viewportState.affineTransform(width, height))

        paintGrid(g2)

        val zoom = viewportState.zoom
        val ox = viewportState.offsetX
        val oy = viewportState.offsetY
        val pw = width.toDouble()
        val ph = height.toDouble()

        val tokens = lastTokens.sortedBy { it.index.value }
        for (token in tokens) {
            val sprite = stateRepository.findSpriteById(token.spriteId)
            val visual = tokenAnimator.visualize(token)

            if (sprite == null) {
                val screenCx = (visual.position.x + ox) * zoom + pw / 2
                val screenCy = (visual.position.y + oy) * zoom + ph / 2
                val screenR = sqrt((TokenRenderer.MISSING_W.toDouble().pow(2) + TokenRenderer.MISSING_H.toDouble().pow(2))) / 2 * zoom
                if (screenCx + screenR < 0 || screenCx - screenR > pw ||
                    screenCy + screenR < 0 || screenCy - screenR > ph) continue
                tokenRenderer.drawMissing(g2, visual, selectionState.contains(token.id))
                continue
            }

            val (frontImg, _) = tokenRenderer.getImages(sprite)
            val screenCx = (visual.position.x + ox) * zoom + pw / 2
            val screenCy = (visual.position.y + oy) * zoom + ph / 2
            val screenR = sqrt(frontImg.width.toDouble().pow(2) + frontImg.height.toDouble().pow(2)) / 2 * zoom
            if (screenCx + screenR < 0 || screenCx - screenR > pw ||
                screenCy + screenR < 0 || screenCy - screenR > ph) continue

            val (sx, sy) = tokenAnimator.flipScale(token.id)
            tokenRenderer.draw(g2, visual, sprite, selectionState.contains(token.id), token.locked, sx, sy)
        }

        for (pointer in lastPointers) {
            val (px, py) = pointerAnimator.visualize(pointer)
            g2.color = Color(pointer.red, pointer.green, pointer.blue, 150)
            val r = 32
            g2.fillOval((px - r).toInt(), (py - r).toInt(), r * 2, r * 2)
        }

        g2.transform = savedTransform

        dragRectOverlay?.let { overlay ->
            val rect = overlay.toRectangle()
            g2.color = Color(0, 100, 255, 50)
            g2.fillRect(rect.x, rect.y, rect.width, rect.height)
        }
    }

    private fun paintGrid(g2: Graphics2D) {
        val zoom = viewportState.zoom
        val ox   = viewportState.offsetX
        val oy   = viewportState.offsetY
        val w    = this.width.toDouble()
        val h    = this.height.toDouble()

        val worldLeft   = (-w / 2.0) / zoom - ox
        val worldRight  = ( w / 2.0) / zoom - ox
        val worldTop    = (-h / 2.0) / zoom - oy
        val worldBottom = ( h / 2.0) / zoom - oy

        val spacing = niceSpacing(20.0 / zoom)
        val major   = spacing * 5

        val x0 = (floor(worldLeft   / spacing) * spacing).toInt()
        val x1 = (ceil (worldRight  / spacing) * spacing).toInt()
        val y0 = (floor(worldTop    / spacing) * spacing).toInt()
        val y1 = (ceil (worldBottom / spacing) * spacing).toInt()

        val pixelStroke = BasicStroke((1f / zoom).toFloat())
        g2.stroke = pixelStroke

        g2.color = Color(45, 45, 45)
        var xi = x0; while (xi <= x1) { g2.drawLine(xi, y0, xi, y1); xi += spacing }
        var yi = y0; while (yi <= y1) { g2.drawLine(x0, yi, x1, yi); yi += spacing }

        g2.color = Color(55, 55, 55)
        xi = (floor(worldLeft / major) * major).toInt()
        while (xi <= x1) { g2.drawLine(xi, y0, xi, y1); xi += major }
        yi = (floor(worldTop / major) * major).toInt()
        while (yi <= y1) { g2.drawLine(x0, yi, x1, yi); yi += major }

        g2.color = Color(65, 65, 65)
        g2.drawLine(x0, 0, x1, 0)
        g2.drawLine(0, y0, 0, y1)
    }

    private fun niceSpacing(raw: Double): Int {
        val mag = 10.0.pow(floor(log10(raw.coerceAtLeast(1.0)))).toInt()
        val r = raw / mag
        return when {
            r < 2.0 -> mag
            r < 5.0 -> mag * 2
            else    -> mag * 5
        }
    }
}
