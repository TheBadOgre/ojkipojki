package net.rafkos.ojkipojki.client.application

import kotlinx.coroutines.*
import net.rafkos.ojkipojki.shared.AppDirs
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import java.io.File

object SpriteBagDirectoryLoader {
    suspend fun loadAll(rootDir: File = AppDirs.spritesRoot): List<SpriteBag> = coroutineScope {

        val subdirs = rootDir.listFiles { f -> f.isDirectory }
            ?: return@coroutineScope emptyList()

        val dispatcher = Dispatchers.Default

        subdirs.map { dir ->
            async(dispatcher) {
                SpriteLoader.loadSprites(dir, dispatcher)
            }
        }.awaitAll().flatten()
    }
}
