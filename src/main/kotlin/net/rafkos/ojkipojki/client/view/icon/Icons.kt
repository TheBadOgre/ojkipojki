package net.rafkos.ojkipojki.client.view.icon

import javax.imageio.ImageIO
import javax.swing.ImageIcon

object Icons {
    private fun load(name: String): ImageIcon {
        val stream = Icons::class.java.classLoader.getResourceAsStream("icons/$name.png")
            ?: return ImageIcon()
        return ImageIcon(ImageIO.read(stream))
    }

    val selectAll   = load("select_all")
    val deselectAll = load("deselect_all")
    val rotate60    = load("rotate_60")
    val indexDown   = load("index_down")
    val indexUp     = load("index_up")
    val delete      = load("delete")
    val refreshBags = load("refresh_bags")
    val flip        = load("flip")
    val stack       = load("stack")
    val shuffle     = load("shuffle")
    val spreadH     = load("spread_horizontal")
    val spreadV     = load("spread_vertical")
    val grid        = load("grid")
}
