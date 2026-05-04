package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import java.util.UUID

class GameDataV1Deserializer : GameDataDeserializer<GameDataV1> {

    override fun deserialize(gameSave: GameDataV1): GameData {
        val tokens = gameSave.tokens.map { token ->
            Token(
                id = TokenId(UUID.fromString(token.id)),
                spriteId = decodeSpriteId(token.spriteId),
                position = token.position,
                rotation = token.rotation,
                index = token.index,
                flipped = token.flipped,
                locked = token.locked,
            )
        }
        return GameData(spriteBags = emptyList(), tokens = tokens)
    }

    private fun decodeSpriteId(encoded: String): SpriteId {
        val parts = encoded.split(":")
        val blue  = parts[parts.size - 1].toInt()
        val green = parts[parts.size - 2].toInt()
        val red   = parts[parts.size - 3].toInt()
        val bagId = parts.dropLast(3).joinToString(":")
        return SpriteId(SpriteBagId(bagId), red, green, blue)
    }
}
