package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class SpriteBag(
    val id: SpriteBagId,
    val sprites: List<Sprite>
) : Serializable
