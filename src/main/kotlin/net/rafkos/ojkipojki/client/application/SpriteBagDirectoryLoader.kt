package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.AppDirs
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import java.io.File

object SpriteBagDirectoryLoader {
    fun loadAll(rootDir: File = AppDirs.spritesRoot): List<SpriteBag> {
        val subdirs = rootDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        return subdirs.flatMap { SpriteLoader.loadSprites(it) }
    }
}