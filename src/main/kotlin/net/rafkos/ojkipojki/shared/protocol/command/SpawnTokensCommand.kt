package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.SpriteBagId

data class SpawnTokensCommand(val spriteBagId: SpriteBagId) : Command
