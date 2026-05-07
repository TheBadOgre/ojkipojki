package net.rafkos.ojkipojki.client.view.render

import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PrewarmResult(
    val images: Map<SpriteId, Pair<BufferedImage, BufferedImage>>,
    val composites: Map<Pair<SpriteId, Boolean>, BufferedImage>,
)

class TokenRenderer {
    companion object {
        private const val SHADOW_SPREAD = 10
        private const val EDGE_SPREAD   = 4
        const val MISSING_W = 80
        const val MISSING_H = 80
    }

    private val imageCache     = mutableMapOf<SpriteId, Pair<BufferedImage, BufferedImage>>()
    private val compositeCache = mutableMapOf<Pair<SpriteId, Boolean>, BufferedImage>()

    fun clearCache() {
        imageCache.clear()
        compositeCache.clear()
    }

    fun seedImages(images: Map<SpriteId, Pair<BufferedImage, BufferedImage>>) {
        images.forEach { (id, pair) ->
            imageCache[id] = Pair(toFastImage(pair.first), toFastImage(pair.second))
        }
    }

    fun buildPrewarm(sprites: List<Sprite>): PrewarmResult {
        val localImages = mutableMapOf<SpriteId, Pair<BufferedImage, BufferedImage>>()
        val localComposites = mutableMapOf<Pair<SpriteId, Boolean>, BufferedImage>()
        for (sprite in sprites) {
            val images = localImages.getOrPut(sprite.id) {
                Pair(
                    toFastImage(ImageIO.read(ByteArrayInputStream(sprite.frontImageBytes))),
                    toFastImage(ImageIO.read(ByteArrayInputStream(sprite.backImageBytes))),
                )
            }
            localComposites[sprite.id to false] = buildCompositeFrom(false, images)
            localComposites[sprite.id to true]  = buildCompositeFrom(true, images)
        }
        return PrewarmResult(localImages, localComposites)
    }

    fun installPrewarm(result: PrewarmResult) {
        imageCache.putAll(result.images)
        compositeCache.putAll(result.composites)
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

    private fun buildComposite(sprite: Sprite, flipped: Boolean): BufferedImage =
        buildCompositeFrom(flipped, getImages(sprite))

    private fun buildCompositeFrom(flipped: Boolean, images: Pair<BufferedImage, BufferedImage>): BufferedImage {
        val (front, back) = images
        val image = if (flipped) back else front
        val w = front.width
        val h = front.height
        val frontPx = front.getRGB(0, 0, w, h, null, 0, w)

        // Shadow: alpha-only black, 36% of source alpha (original 45% × 0.8)
        val shadowPx = IntArray(w * h)
        for (i in frontPx.indices) {
            val a = (frontPx[i] ushr 24) and 0xFF
            if (a > 10) shadowPx[i] = (a * 0.18).toInt() shl 24
        }
        val shadowImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)
        shadowImg.setRGB(0, 0, w, h, shadowPx, 0, w)

        // Dark edge: 35% brightness, 80% of source alpha (20% more transparent)
        val edgePx = IntArray(w * h)
        for (i in frontPx.indices) {
            val argb = frontPx[i]
            val a = (argb ushr 24) and 0xFF
            if (a > 0) {
                val r = (((argb ushr 16) and 0xFF) * 0.35).toInt()
                val g = (((argb ushr 8)  and 0xFF) * 0.35).toInt()
                val b = (( argb          and 0xFF) * 0.35).toInt()
                edgePx[i] = ((a * 0.8).toInt() shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val edgeImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)
        edgeImg.setRGB(0, 0, w, h, edgePx, 0, w)

        val ss = SHADOW_SPREAD
        val es = EDGE_SPREAD
        val composite = BufferedImage(w + 2 * ss, h + 2 * ss, BufferedImage.TYPE_INT_ARGB_PRE)
        val cg = composite.createGraphics()
        try {
            // Shadow: 8 positions at ±ss around center — equal halo all sides
            for (dx in intArrayOf(-ss, 0, ss)) for (dy in intArrayOf(-ss, 0, ss)) {
                if (dx == 0 && dy == 0) continue
                cg.drawImage(shadowImg, ss + dx, ss + dy, null)
            }
            // Dark edge: 8 positions at ±es — equal stroke all sides
            for (dx in intArrayOf(-es, 0, es)) for (dy in intArrayOf(-es, 0, es)) {
                if (dx == 0 && dy == 0) continue
                cg.drawImage(edgeImg, ss + dx, ss + dy, null)
            }
            // Main image centered
            cg.drawImage(image, ss, ss, null)
        } finally { cg.dispose() }
        return composite
    }

    fun draw(g2: Graphics2D, token: Token, sprite: Sprite, selected: Boolean, locked: Boolean,
             scaleX: Double = 1.0, scaleY: Double = 1.0) {
        if (scaleX < 0.01 || scaleY < 0.01) return
        val (front, _) = getImages(sprite)
        val w = front.width
        val h = front.height
        val composite = compositeCache.getOrPut(sprite.id to token.flipped) { buildComposite(sprite, token.flipped) }
        val ss = SHADOW_SPREAD

        val saved = g2.transform
        val at = AffineTransform()
        at.translate(token.position.x.toDouble(), token.position.y.toDouble())
        at.rotate(Math.toRadians(token.rotation.degrees))
        at.scale(scaleX, scaleY)
        // Shift so the image portion (at ss,ss in composite) is centered on token position
        at.translate(-w / 2.0 - ss, -h / 2.0 - ss)
        g2.transform(at)

        g2.drawImage(composite, 0, 0, null)

        val prevColor  = g2.color
        val prevStroke = g2.stroke
        // Derive pixel width from the actual combined transform so it stays 1px on screen
        // regardless of viewport zoom, token flip scale, or HiDPI base scale.
        val currentAt = g2.transform
        val det = abs(currentAt.scaleX * currentAt.scaleY - currentAt.shearX * currentAt.shearY)
        val pw = (1.0 / sqrt(det)).toFloat()

        if (selected) {
            g2.color  = Color(80, 160, 255)
            g2.stroke = BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                floatArrayOf(pw * 6, pw * 4), 0f)
            g2.drawRect(ss, ss, w, h)
        }

        if (locked) {
            val inset = (pw * 4).toInt().coerceAtLeast(3)
            g2.color  = Color(180, 180, 180, 200)
            g2.stroke = BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                floatArrayOf(pw * 5, pw * 3), pw * 3)
            g2.drawRect(ss + inset, ss + inset, w - inset * 2, h - inset * 2)
        }

        g2.color  = prevColor
        g2.stroke = prevStroke

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

    fun missingHitTest(token: Token, worldX: Double, worldY: Double): Boolean {
        val dx = worldX - token.position.x
        val dy = worldY - token.position.y
        val rad = -Math.toRadians(token.rotation.degrees)
        val lx = dx * cos(rad) - dy * sin(rad)
        val ly = dx * sin(rad) + dy * cos(rad)
        return lx >= -MISSING_W / 2.0 && lx <= MISSING_W / 2.0 &&
               ly >= -MISSING_H / 2.0 && ly <= MISSING_H / 2.0
    }

    fun drawMissing(g2: Graphics2D, token: Token, selected: Boolean) {
        val w = MISSING_W
        val h = MISSING_H

        val saved = g2.transform
        val at = AffineTransform()
        at.translate(token.position.x.toDouble(), token.position.y.toDouble())
        at.rotate(Math.toRadians(token.rotation.degrees))
        at.translate(-w / 2.0, -h / 2.0)
        g2.transform(at)

        g2.color = Color(255, 105, 180)
        g2.fillRect(0, 0, w, h)

        g2.color = Color.BLACK
        val fm = g2.fontMetrics
        val text = LocaleService.get("spritebag.missing")
        val tx = (w - fm.stringWidth(text)) / 2
        val ty = (h - fm.height) / 2 + fm.ascent
        g2.drawString(text, tx, ty)

        val prevStroke = g2.stroke
        val prevColor = g2.color
        val currentAt = g2.transform
        val det = abs(currentAt.scaleX * currentAt.scaleY - currentAt.shearX * currentAt.shearY)
        val pw = (1.0 / sqrt(det)).toFloat()

        if (selected) {
            g2.color = Color(80, 160, 255)
            g2.stroke = BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                floatArrayOf(pw * 6, pw * 4), 0f)
            g2.drawRect(0, 0, w, h)
        }

        g2.stroke = prevStroke
        g2.color = prevColor
        g2.transform = saved
    }
}
