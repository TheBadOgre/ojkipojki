package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import java.util.UUID

class TokenModel {
    var id: TokenId = TokenId(UUID.randomUUID())
    var spriteId: SpriteId = SpriteId(
        spriteBagId = SpriteBagId(""),
        red = 0,
        green = 0,
        blue = 0
    )
    var position: PositionModel = PositionModel()
    var rotation: RotationModel = RotationModel()
    var index: IndexModel = IndexModel()
    var flipped: Boolean = false

    fun apply(state: Token) {
        this.id = state.id
        this.spriteId = state.spriteId
        position.apply(state.position)
        rotation.apply(state.rotation)
        index.apply(state.index)
        this.flipped = state.flipped
    }

    fun toState(): Token = Token(
        id = id,
        spriteId = spriteId,
        position = position.toState(),
        rotation = rotation.toState(),
        index = index.toState(),
        flipped = flipped
    )
}
