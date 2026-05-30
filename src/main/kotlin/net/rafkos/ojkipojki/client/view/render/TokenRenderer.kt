package net.rafkos.ojkipojki.client.view.render

import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
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
        private val OFFSETS = intArrayOf(-1, 0, 1)
    }

    private val imageCache     = mutableMapOf<SpriteId, Pair<BufferedImage, BufferedImage>>()
    private val compositeCache = mutableMapOf<Pair<SpriteId, Boolean>, BufferedImage>()

    // Mip pyramid per composite: index 0 is the full composite, each level half-size.
    // Zoom-independent — built once, lazily, on first draw of a token.
    private val mipCache = mutableMapOf<Pair<SpriteId, Boolean>, List<BufferedImage>>()

    fun clearCache() {
        imageCache.clear()
        compositeCache.clear()
        mipCache.clear()
    }

    fun seedImages(images: Map<SpriteId, Pair<BufferedImage, BufferedImage>>) {
        images.forEach { (id, pair) ->
            imageCache[id] = Pair(toFastImage(pair.first), toFastImage(pair.second))
        }
    }

    fun buildPrewarm(sprites: List<Sprite>): PrewarmResult {
        val localImages    = ConcurrentHashMap<SpriteId, Pair<BufferedImage, BufferedImage>>()
        val localComposites = ConcurrentHashMap<Pair<SpriteId, Boolean>, BufferedImage>()
        sprites.parallelStream().forEach { sprite ->
            val images = localImages.computeIfAbsent(sprite.id) {
                Pair(
                    toFastImage(ImageIO.read(ByteArrayInputStream(sprite.frontImageBytes))),
                    toFastImage(ImageIO.read(ByteArrayInputStream(sprite.backImageBytes))),
                )
            }
            val (front, back) = images
            val baseLayer = buildBaseLayer(front)
            localComposites[sprite.id to false] = compositeWithMain(baseLayer, front, front.width, front.height)
            localComposites[sprite.id to true]  = compositeWithMain(baseLayer, back,  front.width, front.height)
        }
        return PrewarmResult(localImages, localComposites)
    }

    fun installPrewarm(result: PrewarmResult) {
        imageCache.putAll(result.images)
        compositeCache.putAll(result.composites)
        mipCache.clear()
    }

    /** Mip levels are zoom-independent and built lazily on first draw, so no per-zoom
     *  rebuild is needed. Kept as the caller's idle hook: just repaint. */
    fun scheduleScaledRebuild(@Suppress("UNUSED_PARAMETER") zoom: Double, onDone: () -> Unit) {
        onDone()
    }

    // Halving pyramid down to 1px. Index 0 is the full composite. Each level is a
    // high-quality half-step of the previous, so drawing always downsamples <=2x —
    // a single large-ratio step samples only 2x2 source pixels → aliasing/blur.
    private fun buildMips(base: BufferedImage): List<BufferedImage> {
        val list = ArrayList<BufferedImage>()
        list.add(base)
        var cur = base
        while (cur.width > 1 && cur.height > 1) {
            val nw = (cur.width / 2).coerceAtLeast(1)
            val nh = (cur.height / 2).coerceAtLeast(1)
            cur = scaleStep(cur, nw, nh)
            list.add(cur)
        }
        return list
    }

    private fun scaleStep(src: BufferedImage, w: Int, h: Int): BufferedImage {
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(src, 0, 0, w, h, null)
        g.dispose()
        return out
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

    private fun buildComposite(sprite: Sprite, flipped: Boolean): BufferedImage {
        val (front, back) = getImages(sprite)
        val baseLayer = buildBaseLayer(front)
        return compositeWithMain(baseLayer, if (flipped) back else front, front.width, front.height)
    }

    // Scatter shadow + dark edge from front into a (w+2ss)×(h+2ss) IntArray.
    // Shared between flipped and non-flipped composites — front pixels only, no main image.
    private fun buildBaseLayer(front: BufferedImage): IntArray {
        val w  = front.width
        val h  = front.height
        val ss = SHADOW_SPREAD
        val es = EDGE_SPREAD
        val cw = w + 2 * ss
        val outData = IntArray(cw * (h + 2 * ss))
        val src = (front.raster.dataBuffer as DataBufferInt).data

        for (sy in 0 until h) {
            val srcRowOff = sy * w
            for (sx in 0 until w) {
                val px   = src[srcRowOff + sx]
                val aPre = (px ushr 24) and 0xFF
                if (aPre == 0) continue

                // Shadow: black at ~18% of alpha — premultiplied black = alpha channel only
                val shadowA = aPre * 46 / 255  // 46/255 ≈ 0.18
                if (shadowA > 0) {
                    val shadowPx = shadowA shl 24
                    for (di in OFFSETS) for (dj in OFFSETS) {
                        if (di == 0 && dj == 0) continue
                        val idx = (ss + sy + dj * ss) * cw + (ss + sx + di * ss)
                        outData[idx] = srcOverPre(shadowPx, outData[idx])
                    }
                }

                // Dark edge: 35% brightness × 80% alpha = 0.28 factor on premultiplied components
                val edgePx = ((aPre * 204 / 255) shl 24) or
                             (((px ushr 16) and 0xFF) * 71 / 255 shl 16) or
                             (((px ushr 8)  and 0xFF) * 71 / 255 shl 8)  or
                              ((px         and 0xFF) * 71 / 255)
                for (di in OFFSETS) for (dj in OFFSETS) {
                    if (di == 0 && dj == 0) continue
                    val idx = (ss + sy + dj * es) * cw + (ss + sx + di * es)
                    outData[idx] = srcOverPre(edgePx, outData[idx])
                }
            }
        }
        return outData
    }

    // Copy base layer into final BufferedImage, then blend main image (front or back) on top.
    private fun compositeWithMain(baseLayer: IntArray, image: BufferedImage, w: Int, h: Int): BufferedImage {
        val ss = SHADOW_SPREAD
        val cw = w + 2 * ss
        val composite = BufferedImage(cw, h + 2 * ss, BufferedImage.TYPE_INT_ARGB_PRE)
        val outData  = (composite.raster.dataBuffer as DataBufferInt).data
        System.arraycopy(baseLayer, 0, outData, 0, outData.size)

        val mainData = (image.raster.dataBuffer as DataBufferInt).data
        for (sy in 0 until h) {
            val srcOff = sy * w
            val dstOff = (ss + sy) * cw + ss
            for (sx in 0 until w) {
                val mp = mainData[srcOff + sx]
                if ((mp ushr 24) == 0) continue
                outData[dstOff + sx] = srcOverPre(mp, outData[dstOff + sx])
            }
        }
        return composite
    }

    // Porter-Duff SRC_OVER for TYPE_INT_ARGB_PRE (premultiplied).
    // outC_pre = srcC_pre + dstC_pre * (1 - srcA)  — no unpremultiply needed.
    private fun srcOverPre(src: Int, dst: Int): Int {
        val sa = (src ushr 24) and 0xFF
        if (sa == 0) return dst
        if (sa >= 255) return src
        val inv = 255 - sa
        return ((sa + (dst ushr 24 and 0xFF) * inv / 255) shl 24) or
               (((src ushr 16 and 0xFF) + (dst ushr 16 and 0xFF) * inv / 255) shl 16) or
               (((src ushr 8  and 0xFF) + (dst ushr 8  and 0xFF) * inv / 255) shl 8)  or
                ((src         and 0xFF) + (dst         and 0xFF) * inv / 255)
    }

    fun draw(g2: Graphics2D, token: Token, sprite: Sprite, selected: Boolean, locked: Boolean,
             scaleX: Double = 1.0, scaleY: Double = 1.0, viewportZoom: Double = -1.0) {
        if (scaleX < 0.01 || scaleY < 0.01) return
        val (front, _) = getImages(sprite)
        val w  = front.width
        val h  = front.height
        val ss = SHADOW_SPREAD
        val key = sprite.id to token.flipped
        val wx  = token.position.x.toDouble()
        val wy  = token.position.y.toDouble()
        val deg = Math.toRadians(token.rotation.degrees)

        // Mip selection: pick the smallest level still at least the on-screen size, then
        // let g2 downsample <=2x from it. A single full-res -> tiny step is what caused the
        // blur; capping every draw at <=2x keeps tokens crisp at any zoom and during
        // gestures. effScale is the world->screen factor (g2 already holds scale(zoom);
        // viewportZoom carries it here, scaleX is the per-token extra, normally 1.0).
        val mips = mipCache.getOrPut(key) {
            buildMips(compositeCache.getOrPut(key) { buildComposite(sprite, token.flipped) })
        }
        val base = mips[0]
        val effScale = if (viewportZoom > 0) viewportZoom else 1.0
        val targetPx = base.width * effScale * abs(scaleX)
        var level = 0
        while (level < mips.size - 1 && mips[level + 1].width >= targetPx) level++
        val mip = mips[level]
        // Bring the chosen level back to the full composite's world size (= 2^level).
        val levelScale = base.width.toDouble() / mip.width

        val imageAt = AffineTransform()
        imageAt.translate(wx, wy)
        imageAt.rotate(deg)
        imageAt.scale(scaleX * levelScale, scaleY * levelScale)
        imageAt.translate(-mip.width / 2.0, -mip.height / 2.0)
        // drawImage(img, at, obs) applies `at` without mutating g2's transform state.
        g2.drawImage(mip, imageAt, null)

        if (!selected && !locked) return

        // Decorations always use original world-unit scale for consistent rect coordinates.
        val decorAt = AffineTransform()
        decorAt.translate(wx, wy)
        decorAt.rotate(deg)
        decorAt.scale(scaleX, scaleY)
        decorAt.translate(-w / 2.0 - ss, -h / 2.0 - ss)

        val saved = g2.transform
        g2.transform(decorAt)

        val ct  = g2.transform
        val det = abs(ct.scaleX * ct.scaleY - ct.shearX * ct.shearY)
        val pw  = (1.0 / sqrt(det)).toFloat()

        val prevColor  = g2.color
        val prevStroke = g2.stroke

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

        if (selected) {
            val prevStroke = g2.stroke
            val prevColor = g2.color
            val ct  = g2.transform
            val det = abs(ct.scaleX * ct.scaleY - ct.shearX * ct.shearY)
            val pw  = (1.0 / sqrt(det)).toFloat()
            g2.color = Color(80, 160, 255)
            g2.stroke = BasicStroke(pw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                floatArrayOf(pw * 6, pw * 4), 0f)
            g2.drawRect(0, 0, w, h)
            g2.stroke = prevStroke
            g2.color = prevColor
        }

        g2.transform = saved
    }
}
