package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.sqrt

class LocalSpriteBagListPanel(
    private val stateRepository: StateRepository,
    private val onUpload: (SpriteBag) -> Unit,
    private val onUploadAll: () -> Unit,
) : JPanel(BorderLayout()) {

    var previewSize = 32

    private val imageCache = mutableMapOf<SpriteId, BufferedImage?>()

    private val contentPanel = object : JPanel(), Scrollable {
        init { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        override fun getScrollableTracksViewportWidth() = true
        override fun getScrollableTracksViewportHeight() = false
        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(r: Rectangle, o: Int, d: Int) = 16
        override fun getScrollableBlockIncrement(r: Rectangle, o: Int, d: Int) = 64
    }

    init {
        val headerBar = JPanel(BorderLayout(4, 0))
        headerBar.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        val titleLabel = JLabel(LocaleService.get("spritebag.localPanel.title"))
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 12f)
        val uploadAllBtn = JButton(LocaleService.get("spritebag.uploadAll"))
        uploadAllBtn.isFocusPainted = false
        uploadAllBtn.addActionListener { onUploadAll() }
        headerBar.add(titleLabel, BorderLayout.CENTER)
        headerBar.add(uploadAllBtn, BorderLayout.EAST)
        add(headerBar, BorderLayout.NORTH)

        val scroll = JScrollPane(contentPanel)
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        add(scroll, BorderLayout.CENTER)
    }

    fun refresh() {
        rebuild()
    }

    fun rebuild() {
        contentPanel.removeAll()
        val stateIds: Set<SpriteBagId> = stateRepository.findAllSpriteBags().map { it.id }.toSet()
        val bags = ClientContext.localSpriteBagRegistry.bags()
        val bagsByGroup = bags.groupBy { it.groupName }.toSortedMap()

        for ((groupName, groupBags) in bagsByGroup) {
            contentPanel.add(groupHeader(groupName))
            for (bag in groupBags.sortedBy { it.id.id }) {
                contentPanel.add(bagRow(bag, bag.id in stateIds))
            }
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun groupHeader(name: String): JLabel {
        val lbl = JLabel(name)
        lbl.font = lbl.font.deriveFont(Font.BOLD, 12f)
        lbl.border = BorderFactory.createEmptyBorder(8, 6, 2, 6)
        lbl.alignmentX = Component.LEFT_ALIGNMENT
        return lbl
    }

    private fun bagRow(bag: SpriteBag, inServer: Boolean): JPanel {
        val iconSize = maxOf(24, previewSize / 2)
        val images = bag.sprites.mapNotNull { getImage(it.id, it.frontImageBytes) }

        val iconPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                if (images.isEmpty()) return
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                val n = images.size
                val cols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
                val rows = ceil(n.toDouble() / cols).toInt()
                val cellW = iconSize / cols
                val cellH = iconSize / rows
                images.forEachIndexed { idx, img ->
                    val col = idx % cols; val row = idx / cols
                    val scale = minOf(cellW.toDouble() / img.width, cellH.toDouble() / img.height)
                    val dw = (img.width * scale).toInt(); val dh = (img.height * scale).toInt()
                    g2.drawImage(img, col * cellW + (cellW - dw) / 2, row * cellH + (cellH - dh) / 2, dw, dh, null)
                }
            }
        }
        iconPanel.preferredSize = Dimension(iconSize, iconSize)
        iconPanel.isOpaque = false

        val nameLabel = JLabel(bag.id.id)
        if (inServer) {
            nameLabel.text = "${bag.id.id}  ✓"
            nameLabel.foreground = Color(80, 160, 80)
        }

        val uploadBtn = JButton(LocaleService.get("spritebag.upload"))
        uploadBtn.isFocusPainted = false
        uploadBtn.margin = Insets(2, 6, 2, 6)
        uploadBtn.addActionListener { onUpload(bag) }

        val row = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        row.isOpaque = false
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.add(uploadBtn)
        row.add(iconPanel)
        row.add(nameLabel)
        return row
    }

    private fun getImage(id: SpriteId, bytes: ByteArray): BufferedImage? =
        imageCache.getOrPut(id) {
            try { ImageIO.read(ByteArrayInputStream(bytes)) } catch (_: Exception) { null }
        }
}
