package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.input.DragRectOverlay
import net.rafkos.ojkipojki.client.view.render.TokenRenderer
import net.rafkos.ojkipojki.client.view.state.BackgroundColorState
import net.rafkos.ojkipojki.client.view.state.PointerAnimator
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.TokenAnimator
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.domain.Pointer
import net.rafkos.ojkipojki.shared.domain.Token
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.Random
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class BoardPanel(
    val stateRepository: StateRepository,
    val selectionState: SelectionState,
    val viewportState: ViewportState,
    val tokenRenderer: TokenRenderer,
    val tokenAnimator: TokenAnimator,
    val pointerAnimator: PointerAnimator,
    val backgroundColorState: BackgroundColorState,
) : JPanel() {

    var dragRectOverlay: DragRectOverlay? = null
    private var lastTokens: List<Token> = emptyList()
    private var lastPointers: List<Pointer> = emptyList()
    @Volatile private var sortedTokensDirty: Boolean = true
    @Volatile private var interacting: Boolean = false
    @Volatile private var pointersDirty: Boolean = true
    @Volatile private var animating: Boolean = true
    private var interactionEndTimer: Timer? = null
    @Volatile private var tileVariants: Array<BufferedImage> = buildVariants(backgroundColorState.color, SHARED_EDGES)

    init {
        background = backgroundColorState.color
        backgroundColorState.addListener { c ->
            background = c
            Thread {
                tileVariants = buildVariants(c, SHARED_EDGES)
                SwingUtilities.invokeLater { repaint() }
            }.start()
        }
        preferredSize = Dimension(800, 600)
        isFocusable = true

        Timer(16) {
            if (sortedTokensDirty) {
                lastTokens = stateRepository.findAllTokens().sortedBy { it.index.value }
                sortedTokensDirty = false
                animating = true
            }
            if (pointersDirty) {
                lastPointers = stateRepository.findAllPointers()
                pointersDirty = false
                animating = true
            }
            if (!animating && !interacting) return@Timer
            val tokenChanged = tokenAnimator.tick(lastTokens)
            val pointerChanged = pointerAnimator.tick(lastPointers)
            animating = tokenChanged || pointerChanged
            repaint()
        }.start()
    }

    /** Called by state listener when token set/positions/indexes change.
     *  Triggers re-snapshot + re-sort + animation kick on next tick. */
    fun markTokensDirty() {
        sortedTokensDirty = true
        animating = true
    }

    /** Called by state listener when pointer set changes. Refreshes pointer snapshot. */
    fun markPointersDirty() {
        pointersDirty = true
        animating = true
    }

    /** Marks the view as actively interacting (pan/drag/zoom/rotate).
     *  While set, paint uses NEAREST interpolation and disables antialiasing
     *  for big throughput gain on many tokens. Auto-clears 150ms after last call. */
    fun markInteracting() {
        interacting = true
        interactionEndTimer?.stop()
        interactionEndTimer = Timer(150) {
            interacting = false
            repaint()
        }.apply { isRepeats = false; start() }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            if (interacting) RenderingHints.VALUE_ANTIALIAS_OFF else RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val savedTransform = g2.transform
        g2.transform(viewportState.affineTransform(width, height))

        paintBackground(g2)

        val zoom = viewportState.zoom
        val ox = viewportState.offsetX
        val oy = viewportState.offsetY
        val pw = width.toDouble()
        val ph = height.toDouble()

        for (token in lastTokens) {
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

    private fun paintBackground(g2: Graphics2D) {
        val zoom = viewportState.zoom
        val ox   = viewportState.offsetX
        val oy   = viewportState.offsetY
        val w    = this.width.toDouble()
        val h    = this.height.toDouble()

        val worldLeft   = (-w / 2.0) / zoom - ox
        val worldRight  = ( w / 2.0) / zoom - ox
        val worldTop    = (-h / 2.0) / zoom - oy
        val worldBottom = ( h / 2.0) / zoom - oy

        val tiles = tileVariants
        val tileW = tiles[0].width
        val tileH = tiles[0].height
        val startX = (floor(worldLeft / tileW) * tileW).toInt()
        val startY = (floor(worldTop  / tileH) * tileH).toInt()
        val endX   = ceil(worldRight  / tileW).toInt() * tileW
        val endY   = ceil(worldBottom / tileH).toInt() * tileH

        var ty = startY
        while (ty < endY) {
            val row = ty / tileH
            var tx = startX
            while (tx < endX) {
                val col = tx / tileW
                val variant = tileVariant(col, row)
                g2.drawImage(tiles[variant], tx, ty, null)
                tx += tileW
            }
            ty += tileH
        }
    }

    companion object {
        private const val TILE_SIZE = 512
        private const val VARIANT_COUNT = 8

        // Octave specs: (gridCells, weight). Large-scale weight kept low so
        // tile boundaries between different variants aren't visually jarring.
        private val OCTAVE_SPECS = listOf(4 to 0.25, 12 to 0.40, 36 to 0.25, 100 to 0.10)

        // Per octave: [corner, left[1..cells-1], top[1..cells-1]] — shared across all variants.
        // Shared edges guarantee seamless joins between any two adjacent variants.
        val SHARED_EDGES: List<DoubleArray> by lazy {
            val edgeRng = Random(0xBEEFCAFEL)
            OCTAVE_SPECS.map { (cells, _) ->
                DoubleArray(2 * cells - 1) { edgeRng.nextDouble() }
            }
        }

        fun tileVariant(col: Int, row: Int): Int {
            var h = col * 1664525 xor row * 1013904223
            h = h xor (h ushr 16)
            h *= 0x45d9f3b
            h = h xor (h ushr 16)
            return Math.floorMod(h, VARIANT_COUNT)
        }

        fun buildVariants(color: Color, sharedEdges: List<DoubleArray>): Array<BufferedImage> =
            Array(VARIANT_COUNT) { v -> buildTile(v.toLong(), sharedEdges, color) }

        private fun buildTile(seed: Long, sharedEdges: List<DoubleArray>, color: Color): BufferedImage {
            val rng = Random(seed xor 0x4F4A4BL)

            fun smooth(t: Double) = t * t * (3.0 - 2.0 * t)
            fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

            val grids = OCTAVE_SPECS.mapIndexed { oi, (cells, _) ->
                val se = sharedEdges[oi]
                // se[0]          = corner  (all 4 corners are equal in a seamless tile)
                // se[1..cells-1] = left/right edge interior values
                // se[cells..2*cells-2] = top/bottom edge interior values
                Array(cells + 1) { i ->
                    DoubleArray(cells + 1) { j ->
                        val onLeft = j == 0 || j == cells
                        val onTop  = i == 0 || i == cells
                        when {
                            onLeft && onTop -> se[0]
                            onTop           -> se[cells + j - 1]
                            onLeft          -> se[i]
                            else            -> rng.nextDouble()
                        }
                    }
                }
            }

            val baseR = color.red
            val baseG = color.green
            val baseB = color.blue
            val pixels = IntArray(TILE_SIZE * TILE_SIZE)
            for (y in 0 until TILE_SIZE) {
                for (x in 0 until TILE_SIZE) {
                    var v = 0.0
                    for ((oi, spec) in OCTAVE_SPECS.withIndex()) {
                        val (cells, weight) = spec
                        val grid = grids[oi]
                        val px = x.toDouble() / TILE_SIZE * cells
                        val py = y.toDouble() / TILE_SIZE * cells
                        val ix = px.toInt(); val iy = py.toInt()
                        val tx = smooth(px - ix); val ty = smooth(py - iy)
                        v += lerp(
                            lerp(grid[iy][ix], grid[iy][ix + 1], tx),
                            lerp(grid[iy + 1][ix], grid[iy + 1][ix + 1], tx),
                            ty
                        ) * weight
                    }
                    val factor = 0.88 + v * 0.12
                    val cr = (baseR * factor).toInt().coerceIn(0, 255)
                    val cg = (baseG * factor).toInt().coerceIn(0, 255)
                    val cb = (baseB * factor).toInt().coerceIn(0, 255)
                    pixels[y * TILE_SIZE + x] = (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb
                }
            }

            val img = BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB)
            img.setRGB(0, 0, TILE_SIZE, TILE_SIZE, pixels, 0, TILE_SIZE)
            return img
        }
    }
}
