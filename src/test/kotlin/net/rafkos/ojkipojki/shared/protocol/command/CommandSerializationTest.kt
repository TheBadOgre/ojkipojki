package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID

class CommandSerializationTest {

    private fun <T> roundTrip(obj: T): T {
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).apply { writeObject(obj); flush() } }.toByteArray()
        @Suppress("UNCHECKED_CAST")
        return ObjectInputStream(ByteArrayInputStream(bytes)).readObject() as T
    }

    private fun bagId(id: String) = SpriteBagId(id)
    private fun spriteId(bagId: String) = SpriteId(SpriteBagId(bagId), 10, 20, 30)
    private fun tokenId() = TokenId(UUID.randomUUID())

    @Test
    fun `UploadSpriteBagsCommand round-trip`() {
        val cmd = UploadSpriteBagsCommand(
            listOf(SpriteBag(bagId("dice"), "dice", emptyList()))
        )
        val result = roundTrip(cmd)
        assertEquals(cmd.spriteBags.map { it.id }, result.spriteBags.map { it.id })
    }

    @Test
    fun `MoveTokensCommand round-trip`() {
        val id = tokenId()
        val cmd = MoveTokensCommand(
            listOf(MoveTokensCommand.TokenIdAndPosition(id, Position(1, 2), Rotation(45.0), true, Index(3)))
        )
        val result = roundTrip(cmd)
        assertEquals(cmd, result)
    }

    @Test
    fun `SpawnTokensCommand round-trip`() {
        val cmd = SpawnTokensCommand(bagId("cards"), Position(10, 20), spriteId("cards"), 3)
        val result = roundTrip(cmd)
        assertEquals(cmd, result)
    }

    @Test
    fun `DeleteTokensCommand round-trip`() {
        val cmd = DeleteTokensCommand(listOf(tokenId(), tokenId()))
        val result = roundTrip(cmd)
        assertEquals(cmd, result)
    }

    @Test
    fun `ShuffleTokensCommand round-trip`() {
        val cmd = ShuffleTokensCommand(listOf(tokenId(), tokenId()))
        val result = roundTrip(cmd)
        assertEquals(cmd, result)
    }

    @Test
    fun `LockTokensCommand round-trip`() {
        val cmd = LockTokensCommand(listOf(tokenId()), true)
        val result = roundTrip(cmd)
        assertEquals(cmd, result)
    }

    @Test
    fun `MovePointerCommand round-trip`() {
        val cmd = MovePointerCommand(100, 200)
        val result = roundTrip(cmd)
        assertEquals(cmd, result)
    }
}
