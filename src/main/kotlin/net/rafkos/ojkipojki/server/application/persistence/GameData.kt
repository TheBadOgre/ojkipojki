package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.Token

data class GameData(
    val spriteBags: List<SpriteBag>,
    val tokens: List<Token>,
)
