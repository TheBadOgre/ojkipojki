package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.SpriteBagId

data class SpawnTokensCommand(val spriteBagId: SpriteBagId, val position: Position? = null) : Command
