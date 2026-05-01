package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object SpriteLoader {
    fun loadSprites(directory: File): List<SpriteBag> {
        // Group files by sprite bag ID (the part before the first underscore)
        val filesByBagId = directory.listFiles { f -> f.isFile && f.extension == "png" }
            ?.groupBy { f -> f.nameWithoutExtension.substringBefore("_") }
            ?: return emptyList()

        return filesByBagId.mapNotNull { (bagIdStr, files) ->
            val frontFile = files.find { it.nameWithoutExtension.endsWith("_front") }
            val backFile  = files.find { it.nameWithoutExtension.endsWith("_back") }
            val maskFile  = files.find { it.nameWithoutExtension.endsWith("_mask") }

            // All three must be present
            if (frontFile == null || backFile == null || maskFile == null) return@mapNotNull null

            val spriteBagId = SpriteBagId(bagIdStr)
            val frontImage  = ImageIO.read(frontFile)
            val backImage   = ImageIO.read(backFile)
            val maskImage   = ImageIO.read(maskFile)

            val sprites = extractSprites(spriteBagId, frontImage, backImage, maskImage)
            SpriteBag(id = spriteBagId, groupName = directory.name, sprites = sprites)
        }
    }

    /**
     * Finds all distinct non-black colors in the mask. For each color, creates
     * a [Sprite] whose front/back images are cropped to the bounding box of that
     * color's region, with every pixel not belonging to that color made transparent.
     */
    private fun extractSprites(
        bagId: SpriteBagId,
        frontImage: BufferedImage,
        backImage: BufferedImage,
        maskImage: BufferedImage,
    ): List<Sprite> {
        val width  = maskImage.width
        val height = maskImage.height

        // Collect all unique sprite colors (ignore pure black = background)
        val spriteColors = mutableSetOf<Int>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = maskImage.getRGB(x, y) and 0x00FFFFFF  // strip alpha
                if (rgb != 0x000000) spriteColors.add(rgb)
            }
        }

        return spriteColors.map { color ->
            val r = (color shr 16) and 0xFF
            val g = (color shr 8)  and 0xFF
            val b =  color         and 0xFF

            val spriteId = SpriteId(spriteBagId = bagId, red = r, green = g, blue = b)

            // Determine bounding box for this color in the mask
            var minX = width;  var maxX = 0
            var minY = height; var maxY = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (maskImage.getRGB(x, y) and 0x00FFFFFF == color) {
                        if (x < minX) minX = x; if (x > maxX) maxX = x
                        if (y < minY) minY = y; if (y > maxY) maxY = y
                    }
                }
            }

            val cropW = maxX - minX + 1
            val cropH = maxY - minY + 1

            val croppedFront = cropAndMask(frontImage, maskImage, color, minX, minY, cropW, cropH)
            val croppedBack  = cropAndMask(backImage,  maskImage, color, minX, minY, cropW, cropH)

            Sprite(
                id = spriteId,
                frontImageBytes = croppedFront.toPngBytes(),
                backImageBytes  = croppedBack.toPngBytes(),
            )
        }
    }

    /**
     * Crops [source] to the given bounding box and makes every pixel transparent
     * where the [maskImage] pixel does not match [spriteColor] (including black bg).
     */
    private fun cropAndMask(
        source: BufferedImage,
        maskImage: BufferedImage,
        spriteColor: Int,
        startX: Int,
        startY: Int,
        cropW: Int,
        cropH: Int,
    ): BufferedImage {
        val result = BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB)

        for (dy in 0 until cropH) {
            for (dx in 0 until cropW) {
                val srcX = startX + dx
                val srcY = startY + dy
                val maskRgb = maskImage.getRGB(srcX, srcY) and 0x00FFFFFF

                if (maskRgb == spriteColor) {
                    // Keep the source pixel fully opaque
                    val pixel = source.getRGB(srcX, srcY) or (0xFF shl 24)
                    result.setRGB(dx, dy, pixel)
                } else {
                    // Transparent – background or belongs to another sprite
                    result.setRGB(dx, dy, 0x00000000)
                }
            }
        }

        return result
    }

    private fun BufferedImage.toPngBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(this, "PNG", baos)
        return baos.toByteArray()
    }
}