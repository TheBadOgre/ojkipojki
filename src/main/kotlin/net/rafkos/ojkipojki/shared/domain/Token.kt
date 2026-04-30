package net.rafkos.ojkipojki.shared.domain

data class Token(
    val id: TokenId,
    val spriteId: SpriteId,
    val position: Position = Position(0, 0),
    val rotation: Rotation = Rotation(0.0),
    val index: Index = Index(0),
    val flipped: Boolean = false
)
