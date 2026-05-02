package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class SpriteId(
    val spriteBagId: SpriteBagId,
    val red: Int,
    val green: Int,
    val blue: Int
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
