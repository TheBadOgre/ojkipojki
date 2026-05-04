package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.shared.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class GameDataV1DeserializerTest {

    private val deserializer = GameDataV1Deserializer()

    private fun save(vararg tokens: TokenDataV1) = GameDataV1(tokens = tokens.toList())

    private fun tokenData(
        id: UUID = UUID.randomUUID(),
        spriteId: String = "bag:1:2:3",
        position: Position = Position(0, 0),
        rotation: Rotation = Rotation(0.0),
        index: Index = Index(0),
        flipped: Boolean = false,
        locked: Boolean = false,
    ) = TokenDataV1(id.toString(), spriteId, position, rotation, index, flipped, locked)

    @Test
    fun `empty save produces empty collections`() {
        val result = deserializer.deserialize(save())
        assertTrue(result.spriteBags.isEmpty())
        assertTrue(result.tokens.isEmpty())
    }

    @Test
    fun `spriteBags always empty - sprites come from client upload`() {
        val result = deserializer.deserialize(save(tokenData()))
        assertTrue(result.spriteBags.isEmpty())
    }

    @Test
    fun `token fields are preserved`() {
        val id = UUID.randomUUID()
        val data = tokenData(
            id = id,
            spriteId = "dice:10:20:30",
            position = Position(100, 200),
            rotation = Rotation(90.0),
            index = Index(5),
            flipped = true,
            locked = true,
        )

        val result = deserializer.deserialize(save(data))

        assertEquals(1, result.tokens.size)
        val token = result.tokens.first()
        assertEquals(TokenId(id), token.id)
        assertEquals(Position(100, 200), token.position)
        assertEquals(Rotation(90.0), token.rotation)
        assertEquals(Index(5), token.index)
        assertTrue(token.flipped)
        assertTrue(token.locked)
    }

    @Test
    fun `spriteId decoded from bagId colon r colon g colon b`() {
        val result = deserializer.deserialize(save(tokenData(spriteId = "cards:255:128:0")))
        val spriteId = result.tokens.first().spriteId
        assertEquals(SpriteBagId("cards"), spriteId.spriteBagId)
        assertEquals(255, spriteId.red)
        assertEquals(128, spriteId.green)
        assertEquals(0, spriteId.blue)
    }

    @Test
    fun `spriteId with colon in bag name decoded correctly`() {
        val result = deserializer.deserialize(save(tokenData(spriteId = "my:bag:1:2:3")))
        val spriteId = result.tokens.first().spriteId
        assertEquals(SpriteBagId("my:bag"), spriteId.spriteBagId)
        assertEquals(1, spriteId.red)
        assertEquals(2, spriteId.green)
        assertEquals(3, spriteId.blue)
    }

    @Test
    fun `multiple tokens all deserialized`() {
        val gameSave = save(tokenData(), tokenData(), tokenData())
        assertEquals(3, deserializer.deserialize(gameSave).tokens.size)
    }
}
