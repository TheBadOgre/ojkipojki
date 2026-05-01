package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId

class SpriteBagModel {
    var id: SpriteBagId = SpriteBagId("")
    var groupName: String = "Unknown"
    var sprites: MutableList<SpriteModel> = mutableListOf()

    fun apply(state: SpriteBag) {
        this.id = state.id
        this.groupName = state.groupName
        sprites.clear()
        sprites.addAll(state.sprites.map { sprite ->
            SpriteModel().apply { apply(sprite) }
        })
    }

    fun toState(): SpriteBag = SpriteBag(
        id = id,
        groupName = groupName,
        sprites = sprites.map { it.toState() }
    )
}
