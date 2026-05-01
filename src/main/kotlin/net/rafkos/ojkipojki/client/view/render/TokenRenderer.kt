package net.rafkos.ojkipojki.client.view.render

import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.Token
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

class TokenRenderer {
    private val imageCache = mutableMapOf<SpriteId, Pair<BufferedImage, BufferedImage>>()

    fun getImages(sprite: Sprite): Pair<BufferedImage, BufferedImage> =
        imageCache.getOrPut(sprite.id) {
            val front = ImageIO.read(ByteArrayInputStream(sprite.frontImageBytes))
            val back  = ImageIO.read(ByteArrayInputStream(sprite.backImageBytes))
            Pair(front, back)
        }

    fun draw(g2: Graphics2D, token: Token, sprite: Sprite, selected: Boolean, zoom: Double) {
        val (front, back) = getImages(sprite)
        val image = if (token.flipped) back else front
        val w = image.width
        val h = image.height

        val saved = g2.transform
        val at = AffineTransform()
        at.translate(token.position.x.toDouble(), token.position.y.toDouble())
        at.rotate(Math.toRadians(token.rotation.degrees))
        at.translate(-w / 2.0, -h / 2.0)
        g2.transform(at)

        g2.drawImage(image, 0, 0, null)

        if (selected) {
            val prevColor  = g2.color
            val prevStroke = g2.stroke
            val pw = (1.0 / zoom).toFloat()
            g2.color  = Color(80, 160, 255)
            g2.stroke = BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                floatArrayOf(pw * 6, pw * 4), 0f)
            g2.drawRect(0, 0, w, h)
            g2.color  = prevColor
            g2.stroke = prevStroke
        }

        g2.transform = saved
    }

    fun hitTest(token: Token, sprite: Sprite, worldX: Double, worldY: Double): Boolean {
        val (front, _) = getImages(sprite)
        val w = front.width
        val h = front.height
        val dx = worldX - token.position.x
        val dy = worldY - token.position.y
        val rad = -Math.toRadians(token.rotation.degrees)
        val lx = dx * cos(rad) - dy * sin(rad)
        val ly = dx * sin(rad) + dy * cos(rad)
        return lx >= -w / 2.0 && lx <= w / 2.0 && ly >= -h / 2.0 && ly <= h / 2.0
    }
}
