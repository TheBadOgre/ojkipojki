package net.rafkos.ojkipojki.client.application

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import net.rafkos.ojkipojki.shared.AppDirs
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteId
import java.awt.image.BufferedImage
import java.io.File

data class SpriteLoadResult(
    val bags: List<SpriteBag>,
    val images: Map<SpriteId, Pair<BufferedImage, BufferedImage>>,
)

object SpriteBagDirectoryLoader {
    suspend fun loadAll(rootDir: File = AppDirs.spritesRoot): SpriteLoadResult = coroutineScope {

        val subdirs = rootDir.listFiles { f -> f.isDirectory }
            ?: return@coroutineScope SpriteLoadResult(emptyList(), emptyMap())

        val dispatcher = Dispatchers.Default
        // Shared across all directories so total concurrent bag decodes (and their source
        // sheets) stay bounded regardless of how many subdirs exist.
        val gate = Semaphore(SpriteLoader.DEFAULT_BAG_PERMITS)

        val results = subdirs.map { dir ->
            async(dispatcher) {
                SpriteLoader.loadSprites(dir, dispatcher, gate)
            }
        }.awaitAll().flatten()

        SpriteLoadResult(
            bags = results.map { it.bag },
            images = results.flatMap { it.images.entries }.associate { it.key to it.value },
        )
    }
}
