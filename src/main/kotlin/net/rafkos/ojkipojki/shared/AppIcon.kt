package net.rafkos.ojkipojki.shared

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object AppIcon {
    val image: BufferedImage? by lazy {
        try {
            AppIcon::class.java.classLoader.getResourceAsStream("icons/app_icon.png")
                ?.let { ImageIO.read(it) }
        } catch (_: Exception) { null }
    }
}
