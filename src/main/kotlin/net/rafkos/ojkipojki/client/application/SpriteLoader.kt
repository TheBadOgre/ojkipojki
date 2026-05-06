package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.*

data class SpriteBagLoadResult(
    val bag: SpriteBag,
    val images: Map<SpriteId, Pair<BufferedImage, BufferedImage>>,
)

object SpriteLoader {
    suspend fun loadSprites(directory: File, dispatcher: CoroutineDispatcher = Dispatchers.Default): List<SpriteBagLoadResult> = coroutineScope {

        val filesByBagId = directory.listFiles { f ->
            f.isFile && f.extension.lowercase() in listOf("png", "jpg", "jpeg")
        }?.mapNotNull { f ->
            val name = f.nameWithoutExtension
            val bagId = listOf("_front", "_back", "_mask").firstOrNull { name.endsWith(it) }
                ?.let { name.removeSuffix(it) }
                ?: return@mapNotNull null
            bagId to f
        }?.groupBy({ it.first }, { it.second })
            ?: return@coroutineScope emptyList()

        filesByBagId.map { (bagIdStr, files) ->
            async(dispatcher) {

                val frontFile = files.find { it.nameWithoutExtension.endsWith("_front") }
                val backFile  = files.find { it.nameWithoutExtension.endsWith("_back") }
                val maskFile  = files.find { it.nameWithoutExtension.endsWith("_mask") }

                if (frontFile == null || backFile == null || maskFile == null)
                    return@async null

                val spriteBagId = SpriteBagId(bagIdStr)

                val frontImage = ImageIO.read(frontFile)
                val backImage  = ImageIO.read(backFile)
                val maskImage  = ImageIO.read(maskFile)

                val loaded = extractSprites(spriteBagId, frontImage, backImage, maskImage, dispatcher)

                SpriteBagLoadResult(
                    bag = SpriteBag(
                        id = spriteBagId,
                        groupName = directory.name,
                        sprites = loaded.map { it.sprite },
                    ),
                    images = loaded.associate { it.sprite.id to Pair(it.front, it.back) },
                )
            }
        }.awaitAll().filterNotNull()
    }

    private data class BBox(var minX: Int, var maxX: Int, var minY: Int, var maxY: Int)
    private data class LoadedSprite(val sprite: Sprite, val front: BufferedImage, val back: BufferedImage)

    private suspend fun extractSprites(
        bagId: SpriteBagId,
        frontImage: BufferedImage,
        backImage: BufferedImage,
        maskImage: BufferedImage,
        dispatcher: CoroutineDispatcher,
    ): List<LoadedSprite> = coroutineScope {

        val width  = maskImage.width
        val height = maskImage.height

        val maskPixels  = maskImage.getRGB(0, 0, width, height, null, 0, width)
        val frontPixels = frontImage.getRGB(0, 0, width, height, null, 0, width)
        val backPixels  = backImage.getRGB(0, 0, width, height, null, 0, width)

        val bboxByColor = mutableMapOf<Int, BBox>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = maskPixels[y * width + x] and 0x00FFFFFF
                if (rgb == 0x000000) continue
                val bb = bboxByColor.getOrPut(rgb) { BBox(x, x, y, y) }
                if (x < bb.minX) bb.minX = x
                if (x > bb.maxX) bb.maxX = x
                if (y < bb.minY) bb.minY = y
                if (y > bb.maxY) bb.maxY = y
            }
        }

        bboxByColor.map { (color, bb) ->
            async(dispatcher) {
                val r = (color shr 16) and 0xFF
                val g = (color shr 8)  and 0xFF
                val b =  color         and 0xFF

                val spriteId = SpriteId(spriteBagId = bagId, red = r, green = g, blue = b)
                val cropW = bb.maxX - bb.minX + 1
                val cropH = bb.maxY - bb.minY + 1

                val croppedFront = cropAndMask(
                    frontPixels, maskPixels, width, color,
                    bb.minX, bb.minY, cropW, cropH
                )
                val croppedBack = cropAndMask(
                    backPixels, maskPixels, width, color,
                    bb.minX, bb.minY, cropW, cropH
                )

                LoadedSprite(
                    sprite = Sprite(
                        id = spriteId,
                        frontImageBytes = croppedFront.toPngBytes(),
                        backImageBytes = croppedBack.toPngBytes(),
                    ),
                    front = croppedFront,
                    back = croppedBack,
                )
            }
        }.awaitAll()
    }

    private fun cropAndMask(
        sourcePixels: IntArray,
        maskPixels: IntArray,
        imageWidth: Int,
        spriteColor: Int,
        startX: Int,
        startY: Int,
        cropW: Int,
        cropH: Int,
    ): BufferedImage {
        val resultPixels = IntArray(cropW * cropH)
        for (dy in 0 until cropH) {
            for (dx in 0 until cropW) {
                val idx = (startY + dy) * imageWidth + (startX + dx)
                resultPixels[dy * cropW + dx] = if (maskPixels[idx] and 0x00FFFFFF == spriteColor) {
                    sourcePixels[idx] or (0xFF shl 24)
                } else {
                    0x00000000
                }
            }
        }
        val result = BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB)
        result.setRGB(0, 0, cropW, cropH, resultPixels, 0, cropW)
        return result
    }

    private fun BufferedImage.toPngBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(this, "webp", baos)
        return baos.toByteArray()
    }
}
