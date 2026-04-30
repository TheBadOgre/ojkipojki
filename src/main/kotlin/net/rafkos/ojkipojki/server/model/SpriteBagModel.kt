package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId

class SpriteBagModel {
    var id: SpriteBagId = SpriteBagId("")
    var sprites: MutableList<SpriteModel> = mutableListOf()

    fun apply(state: SpriteBag) {
        this.id = state.id
        sprites.clear()
        sprites.addAll(state.sprites.map { sprite ->
            SpriteModel().apply { apply(sprite) }
        })
    }

    fun toState(): SpriteBag = SpriteBag(
        id = id,
        sprites = sprites.map { it.toState() }
    )
}
