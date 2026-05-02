package net.rafkos.ojkipojki.shared.domain

class Sprite(
    val id: SpriteId,
    val frontImageBytes: ByteArray,
    val backImageBytes: ByteArray,
) : java.io.Serializable {
    companion object { private const val serialVersionUID = 1L }
}