package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class GameDataV1SerializerTest {

    private val serializer = GameDataV1Serializer()
    private val repo = ModelRepository()

    private fun token(
        spriteId: SpriteId = SpriteId(SpriteBagId("bag"), 1, 2, 3),
        position: Position = Position(0, 0),
        rotation: Rotation = Rotation(0.0),
        index: Index = Index(0),
        flipped: Boolean = false,
        locked: Boolean = false,
    ) = TokenModel().apply {
        apply(Token(TokenId(UUID.randomUUID()), spriteId, position, rotation, index, flipped, locked))
    }

    @Test
    fun `empty repository produces empty token list`() {
        val result = serializer.serialize(repo)
        assertTrue(result.tokens.isEmpty())
    }

    @Test
    fun `token fields are preserved`() {
        val spriteId = SpriteId(SpriteBagId("dice"), 10, 20, 30)
        val model = token(
            spriteId = spriteId,
            position = Position(100, 200),
            rotation = Rotation(45.0),
            index = Index(3),
            flipped = true,
            locked = true,
        )
        repo.saveToken(model)

        val result = serializer.serialize(repo)

        assertEquals(1, result.tokens.size)
        val t = result.tokens.first()
        assertEquals(model.id.id.toString(), t.id)
        assertEquals(Position(100, 200), t.position)
        assertEquals(Rotation(45.0), t.rotation)
        assertEquals(Index(3), t.index)
        assertTrue(t.flipped)
        assertTrue(t.locked)
    }

    @Test
    fun `multiple tokens all serialized`() {
        repeat(5) { repo.saveToken(token()) }
        assertEquals(5, serializer.serialize(repo).tokens.size)
    }

    @Test
    fun `spriteId encoded as bagId colon r colon g colon b`() {
        val spriteId = SpriteId(SpriteBagId("cards"), 255, 128, 0)
        repo.saveToken(token(spriteId = spriteId))

        val encoded = serializer.serialize(repo).tokens.first().spriteId
        assertEquals("cards:255:128:0", encoded)
    }

    @Test
    fun `spriteId with colon in bag name encodes correctly`() {
        val spriteId = SpriteId(SpriteBagId("my:bag"), 1, 2, 3)
        repo.saveToken(token(spriteId = spriteId))

        val encoded = serializer.serialize(repo).tokens.first().spriteId
        assertEquals("my:bag:1:2:3", encoded)
    }
}
