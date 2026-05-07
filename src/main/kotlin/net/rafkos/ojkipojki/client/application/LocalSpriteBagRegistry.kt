package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteId
import java.awt.image.BufferedImage

open class LocalSpriteBagRegistry {
    @Volatile protected var loaded: SpriteLoadResult = SpriteLoadResult(emptyList(), emptyMap())
    open suspend fun reload() { loaded = SpriteBagDirectoryLoader.loadAll() }
    fun bags(): List<SpriteBag> = loaded.bags
    fun images(): Map<SpriteId, Pair<BufferedImage, BufferedImage>> = loaded.images
}
