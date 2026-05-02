package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class SpriteBag(
    val id: SpriteBagId,
    val groupName: String,
    val sprites: List<Sprite>
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
