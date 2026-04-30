package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.Index
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.domain.TokenId
import java.io.Serializable

data class MoveTokensCommand(val adjustments: List<TokenIdAndPosition>) : Command {
    data class TokenIdAndPosition(
        val tokenId: TokenId,
        val position: Position?,
        val rotation: Rotation?,
        val flipped: Boolean?,
        val index: Index?
    ) : Serializable
}