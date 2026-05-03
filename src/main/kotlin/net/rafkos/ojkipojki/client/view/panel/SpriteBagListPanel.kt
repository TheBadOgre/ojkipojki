package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.action.SpriteBagSpawnHandler
import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.locale.LocaleService
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteId
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import kotlin.math.ceil
import kotlin.math.sqrt

class SpriteBagListPanel(
    private val spawnHandler: SpriteBagSpawnHandler,
    private val stateRepository: StateRepository,
) : JPanel(BorderLayout()) {

    private val imageCache = mutableMapOf<SpriteId, BufferedImage?>()
    private val expandedBags = mutableSetOf<String>()

    private val contentPanel = object : JPanel(), Scrollable {
        init { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        override fun getScrollableTracksViewportWidth() = true
        override fun getScrollableTracksViewportHeight() = false
        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(r: Rectangle, o: Int, d: Int) = 16
        override fun getScrollableBlockIncrement(r: Rectangle, o: Int, d: Int) = 64
    }

    private val sizeSlider = JSlider(24, 128, 64)

    init {
        preferredSize = Dimension(220, 0)

        val scroll = JScrollPane(contentPanel)
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        add(scroll, BorderLayout.CENTER)

        val sliderPanel = JPanel(BorderLayout(4, 0))
        sliderPanel.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        sliderPanel.add(JLabel(LocaleService.get("spritebag.size")), BorderLayout.WEST)
        sliderPanel.add(sizeSlider, BorderLayout.CENTER)
        add(sliderPanel, BorderLayout.SOUTH)

        sizeSlider.addChangeListener { rebuild() }
    }

    fun refresh() {
        imageCache.clear()
        rebuild()
    }

    fun rebuild() {
        contentPanel.removeAll()
        val previewSize = sizeSlider.value
        val tokensBySpriteId = stateRepository.findAllTokens().groupBy { it.spriteId }

        val bagsByGroup = stateRepository.findAllSpriteBags()
            .groupBy { it.groupName }
            .toSortedMap()

        for ((groupName, bags) in bagsByGroup) {
            contentPanel.add(groupHeader(groupName))

            for (bag in bags.sortedBy { it.id.id }) {
                val totalOnBoard = bag.sprites.sumOf { tokensBySpriteId[it.id]?.size ?: 0 }
                val collapsed = bag.id.id !in expandedBags

                contentPanel.add(bagHeader(bag, totalOnBoard, previewSize, collapsed))

                if (!collapsed) {
                    val gridPanel = JPanel(WrapLayout(FlowLayout.LEFT, 4, 4))
                    gridPanel.border = BorderFactory.createEmptyBorder(2, 16, 6, 4)
                    gridPanel.isOpaque = false
                    gridPanel.alignmentX = Component.LEFT_ALIGNMENT
                    for (sprite in bag.sprites) {
                        val count = tokensBySpriteId[sprite.id]?.size ?: 0
                        gridPanel.add(buildTile(sprite, count, previewSize))
                    }
                    contentPanel.add(gridPanel)
                }
            }
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun groupHeader(name: String): JLabel {
        val lbl = JLabel(name)
        lbl.font = lbl.font.deriveFont(Font.BOLD, 13f)
        lbl.border = BorderFactory.createEmptyBorder(10, 6, 2, 6)
        lbl.alignmentX = Component.LEFT_ALIGNMENT
        return lbl
    }

    private fun bagHeader(bag: SpriteBag, totalOnBoard: Int, previewSize: Int, collapsed: Boolean): JPanel {
        val iconSize = maxOf(24, previewSize / 2)
        val images = bag.sprites.mapNotNull { getImage(it) }

        // Arrow toggle — click only this to expand/collapse
        val arrow = JLabel(if (collapsed) "▶" else "▼")
        arrow.font = arrow.font.deriveFont(10f)
        arrow.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        arrow.border = BorderFactory.createEmptyBorder(0, 0, 0, 2)
        arrow.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (bag.id.id in expandedBags) expandedBags.remove(bag.id.id) else expandedBags.add(bag.id.id)
                rebuild()
            }
        })

        // Grid composite icon
        val iconPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                if (images.isEmpty()) return
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val n = images.size
                val cols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
                val rows = ceil(n.toDouble() / cols).toInt()
                val cellW = iconSize / cols
                val cellH = iconSize / rows
                images.forEachIndexed { idx, img ->
                    val col = idx % cols
                    val row = idx / cols
                    val cellX = col * cellW
                    val cellY = row * cellH
                    val scale = minOf(cellW.toDouble() / img.width, cellH.toDouble() / img.height)
                    val dw = (img.width * scale).toInt()
                    val dh = (img.height * scale).toInt()
                    g2.drawImage(img, cellX + (cellW - dw) / 2, cellY + (cellH - dh) / 2, dw, dh, null)
                }
            }
        }
        iconPanel.preferredSize = Dimension(iconSize, iconSize)
        iconPanel.isOpaque = false

        val label = JLabel("${bag.id.id}  ($totalOnBoard/${bag.sprites.size})")
        label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        // Spawn row: icon + label — double-click spawns, drag exports bag
        val spawnRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        spawnRow.isOpaque = false
        spawnRow.add(iconPanel)
        spawnRow.add(label)
        spawnRow.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) spawnHandler.spawn(bag.id, null)
            }
        })
        spawnRow.transferHandler = object : TransferHandler() {
            override fun getSourceActions(c: JComponent) = COPY
            override fun createTransferable(c: JComponent) = StringSelection(spawnHandler.encodeBag(bag.id))
        }
        spawnRow.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                spawnRow.transferHandler?.exportAsDrag(spawnRow, e, TransferHandler.COPY)
            }
        })

        // Outer row: [arrow] [spawnRow]
        val row = JPanel(BorderLayout())
        row.border = BorderFactory.createEmptyBorder(4, 12, 0, 4)
        row.isOpaque = false
        row.alignmentX = Component.LEFT_ALIGNMENT

        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 2))
        leftPanel.isOpaque = false
        leftPanel.add(arrow)
        leftPanel.add(spawnRow)
        row.add(leftPanel, BorderLayout.WEST)

        return row
    }

    private fun buildTile(sprite: Sprite, tokenCount: Int, size: Int): JPanel {
        val img = getImage(sprite)

        val imgPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                img ?: return
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val scale = minOf(size.toDouble() / img.width, size.toDouble() / img.height)
                val dw = (img.width * scale).toInt()
                val dh = (img.height * scale).toInt()
                g2.drawImage(img, (size - dw) / 2, (size - dh) / 2, dw, dh, null)
            }
        }
        imgPanel.preferredSize = Dimension(size, size)
        imgPanel.isOpaque = false

        val tile = JPanel(BorderLayout(0, 2))
        tile.preferredSize = Dimension(size, size + 18)
        tile.isOpaque = false
        tile.add(imgPanel, BorderLayout.CENTER)

        val label = JLabel("$tokenCount", SwingConstants.CENTER)
        label.font = label.font.deriveFont(10f)
        label.border = BorderFactory.createEmptyBorder(0, 0, 1, 0)
        tile.add(label, BorderLayout.SOUTH)

        tile.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        tile.transferHandler = object : TransferHandler() {
            override fun getSourceActions(c: JComponent) = COPY
            override fun createTransferable(c: JComponent) = StringSelection(spawnHandler.encodeSprite(sprite.id))
        }
        tile.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                tile.transferHandler?.exportAsDrag(tile, e, TransferHandler.COPY)
            }
        })

        return tile
    }

    private fun getImage(sprite: Sprite): BufferedImage? =
        imageCache.getOrPut(sprite.id) {
            try { ImageIO.read(ByteArrayInputStream(sprite.frontImageBytes)) } catch (_: Exception) { null }
        }
}
