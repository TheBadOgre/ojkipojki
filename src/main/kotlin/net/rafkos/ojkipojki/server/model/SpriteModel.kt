package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteId

class SpriteModel {
    var id: SpriteId = SpriteId(
        spriteBagId = net.rafkos.ojkipojki.shared.domain.SpriteBagId(""),
        red = 0,
        green = 0,
        blue = 0
    )
    var frontImageBytes: ByteArray = byteArrayOf()
    var backImageBytes: ByteArray = byteArrayOf()

    fun apply(state: Sprite) {
        this.id = state.id
        this.frontImageBytes = state.frontImageBytes.copyOf()
        this.backImageBytes = state.backImageBytes.copyOf()
    }

    fun toState(): Sprite = Sprite(
        id = id,
        frontImageBytes = frontImageBytes.copyOf(),
        backImageBytes = backImageBytes.copyOf()
    )
}
