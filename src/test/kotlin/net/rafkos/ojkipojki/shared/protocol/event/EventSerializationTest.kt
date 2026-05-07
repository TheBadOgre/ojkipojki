package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID

class EventSerializationTest {

    private fun <T> roundTrip(obj: T): T {
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).apply { writeObject(obj); flush() } }.toByteArray()
        @Suppress("UNCHECKED_CAST")
        return ObjectInputStream(ByteArrayInputStream(bytes)).readObject() as T
    }

    private fun bagId(id: String) = SpriteBagId(id)
    private fun tokenId() = TokenId(UUID.randomUUID())
    private fun token(id: TokenId) = Token(id, SpriteId(bagId("b"), 1, 2, 3), Position(5, 6), Rotation(10.0), Index(0))
    private fun pointer() = Pointer(10, 20, 100, 150, 200)

    @Test
    fun `SpriteBagsUpdatedEvent round-trip`() {
        val event = SpriteBagsUpdatedEvent(listOf(SpriteBag(bagId("dice"), "dice", emptyList())))
        val result = roundTrip(event)
        assertEquals(event.spriteBags.map { it.id }, result.spriteBags.map { it.id })
    }

    @Test
    fun `TokensSyncEvent round-trip`() {
        val id = tokenId()
        val event = TokensSyncEvent(listOf(token(id)))
        val result = roundTrip(event)
        assertEquals(event, result)
    }

    @Test
    fun `SomeTokensUpdatedEvent round-trip with Update and Delete`() {
        val id1 = tokenId()
        val id2 = tokenId()
        val event = SomeTokensUpdatedEvent(
            listOf(
                SomeTokensUpdatedEvent.TokenAction.Update(token(id1)),
                SomeTokensUpdatedEvent.TokenAction.Delete(id2),
            )
        )
        val result = roundTrip(event)
        assertEquals(2, result.tokenActions.size)
        assert(result.tokenActions[0] is SomeTokensUpdatedEvent.TokenAction.Update)
        assert(result.tokenActions[1] is SomeTokensUpdatedEvent.TokenAction.Delete)
    }

    @Test
    fun `PointersUpdatedEvent round-trip`() {
        val event = PointersUpdatedEvent(listOf(pointer()))
        val result = roundTrip(event)
        assertEquals(event, result)
    }

    @Test
    fun `ConnectedClientsUpdateEvent round-trip`() {
        val event = ConnectedClientsUpdateEvent(5)
        val result = roundTrip(event)
        assertEquals(event, result)
    }

    @Test
    fun `GameInitializationEvent round-trip`() {
        val event = GameInitializationEvent(GameInitializationEvent.Status.IN_PROGRESS, "loading", 0.5)
        val result = roundTrip(event)
        assertEquals(event, result)
    }

    @Test
    fun `GameInitializationEvent rejects progress below 0`() {
        assertThrows<IllegalArgumentException> {
            GameInitializationEvent(GameInitializationEvent.Status.IN_PROGRESS, "msg", -0.01)
        }
    }

    @Test
    fun `GameInitializationEvent rejects progress above 1`() {
        assertThrows<IllegalArgumentException> {
            GameInitializationEvent(GameInitializationEvent.Status.DONE, "msg", 1.01)
        }
    }

    @Test
    fun `GameInitializationEvent accepts boundary values 0 and 1`() {
        assertEquals(0.0, GameInitializationEvent(GameInitializationEvent.Status.IN_PROGRESS, "msg", 0.0).progress)
        assertEquals(1.0, GameInitializationEvent(GameInitializationEvent.Status.DONE, "msg", 1.0).progress)
    }

    @Test
    fun `MissingSpriteBagsEvent round-trip`() {
        val event = MissingSpriteBagsEvent(setOf(SpriteBagId("dice"), SpriteBagId("cards")))
        val result = roundTrip(event)
        assertEquals(event.ids, result.ids)
    }
}
