package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import java.io.File

object SpriteBagDirectoryLoader {
    fun loadAll(rootDir: File = File("sprites")): List<SpriteBag> {
        val subdirs = rootDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        return subdirs.flatMap { SpriteLoader.loadSprites(it) }
    }

    fun getFolderStructure(rootDir: File = File("sprites")): Map<String, List<SpriteBagId>> {
        val subdirs = rootDir.listFiles { f -> f.isDirectory } ?: return emptyMap()
        return subdirs.sortedBy { it.name }.associate { subdir ->
            val bagIds = (subdir.listFiles { f -> f.isFile && f.extension == "png" } ?: emptyArray())
                .map { it.nameWithoutExtension.substringBefore("_") }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
                .map { SpriteBagId(it) }
            subdir.name to bagIds
        }
    }
}