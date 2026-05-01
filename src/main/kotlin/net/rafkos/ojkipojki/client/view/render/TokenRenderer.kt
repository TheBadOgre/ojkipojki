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
    private val imageCache  = mutableMapOf<SpriteId, Pair<BufferedImage, BufferedImage>>()
    private val shadowCache = mutableMapOf<SpriteId, BufferedImage>()
    private val edgeCache   = mutableMapOf<SpriteId, BufferedImage>()

    fun clearCache() {
        imageCache.clear()
        shadowCache.clear()
        edgeCache.clear()
    }

    private fun toFastImage(src: BufferedImage): BufferedImage {
        if (src.type == BufferedImage.TYPE_INT_ARGB_PRE) return src
        val out = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB_PRE)
        val g = out.createGraphics()
        try { g.drawImage(src, 0, 0, null) } finally { g.dispose() }
        return out
    }

    fun getImages(sprite: Sprite): Pair<BufferedImage, BufferedImage> =
        imageCache.getOrPut(sprite.id) {
            val front = toFastImage(ImageIO.read(ByteArrayInputStream(sprite.frontImageBytes)))
            val back  = toFastImage(ImageIO.read(ByteArrayInputStream(sprite.backImageBytes)))
            Pair(front, back)
        }

    private fun getShadow(sprite: Sprite, src: BufferedImage): BufferedImage =
        shadowCache.getOrPut(sprite.id) {
            val w = src.width
            val h = src.height
            val srcPx = src.getRGB(0, 0, w, h, null, 0, w)
            val dst = IntArray(w * h)
            for (i in srcPx.indices) {
                val a = (srcPx[i] ushr 24) and 0xFF
                if (a > 10) dst[i] = (a * 0.45).toInt() shl 24
            }
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)
            out.setRGB(0, 0, w, h, dst, 0, w)
            out
        }

    private fun getDarkEdge(sprite: Sprite, src: BufferedImage): BufferedImage =
        edgeCache.getOrPut(sprite.id) {
            val w = src.width
            val h = src.height
            val srcPx = src.getRGB(0, 0, w, h, null, 0, w)
            val dst = IntArray(w * h)
            for (i in srcPx.indices) {
                val argb = srcPx[i]
                val a = (argb ushr 24) and 0xFF
                if (a > 0) {
                    val r = (((argb ushr 16) and 0xFF) * 0.35).toInt()
                    val g = (((argb ushr 8)  and 0xFF) * 0.35).toInt()
                    val b = (( argb          and 0xFF) * 0.35).toInt()
                    dst[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)
            out.setRGB(0, 0, w, h, dst, 0, w)
            out
        }

    fun draw(g2: Graphics2D, token: Token, sprite: Sprite, selected: Boolean, zoom: Double,
             scaleX: Double = 1.0, scaleY: Double = 1.0) {
        if (scaleX < 0.01 || scaleY < 0.01) return
        val (front, back) = getImages(sprite)
        val image    = if (token.flipped) back else front
        val shadow   = getShadow(sprite, image)
        val darkEdge = getDarkEdge(sprite, image)
        val w = image.width
        val h = image.height

        val saved = g2.transform
        val at = AffineTransform()
        at.translate(token.position.x.toDouble(), token.position.y.toDouble())
        at.rotate(Math.toRadians(token.rotation.degrees))
        at.scale(scaleX, scaleY)   // scale about token centre
        at.translate(-w / 2.0, -h / 2.0)
        g2.transform(at)

        g2.drawImage(shadow,   5, 5, null)
        g2.drawImage(darkEdge, 2, 2, null)
        g2.drawImage(image,    0, 0, null)

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
