package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId

data class SpawnTokensCommand(
    val spriteBagId: SpriteBagId,
    val position: Position? = null,
    val spriteId: SpriteId? = null,
    val count: Int = 1,
) : Command {
    companion object { private const val serialVersionUID = 2L }
}
