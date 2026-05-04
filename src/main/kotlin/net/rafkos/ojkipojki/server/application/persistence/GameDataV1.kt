package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.shared.domain.Index
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.Rotation
import java.io.Serializable

data class GameDataV1(
    val tokens: List<TokenDataV1>,
) : GameSave {
    companion object { private const val serialVersionUID = 1L }
}

data class TokenDataV1(
    val id: String,
    val spriteId: String,
    val position: Position = Position(0, 0),
    val rotation: Rotation = Rotation(0.0),
    val index: Index = Index(0),
    val flipped: Boolean = false,
    val locked: Boolean = false,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
