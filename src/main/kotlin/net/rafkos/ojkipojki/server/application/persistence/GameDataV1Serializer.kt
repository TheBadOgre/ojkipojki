package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.shared.domain.SpriteId

class GameDataV1Serializer : GameDataSerializer<GameDataV1> {

    override fun serialize(repository: ModelRepository): GameDataV1 {
        val tokens = repository.findAllTokens().map { token ->
            val state = token.toState()
            TokenDataV1(
                id = state.id.id.toString(),
                spriteId = encodeSpriteId(state.spriteId),
                position = state.position,
                rotation = state.rotation,
                index = state.index,
                flipped = state.flipped,
                locked = state.locked,
            )
        }
        return GameDataV1(tokens = tokens)
    }

    private fun encodeSpriteId(id: SpriteId) = "${id.spriteBagId.id}:${id.red}:${id.green}:${id.blue}"
}
