package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SomeTokensUpdatedEventHandlerTest {

    private val handler = SomeTokensUpdatedEventHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.client.support.ClientContextFixture

    @BeforeEach
    fun setup() {
        fixture = clientContextFixture()
    }

    private fun tokenId() = TokenId(UUID.randomUUID())
    private fun token(id: TokenId, x: Int = 0) =
        Token(id, SpriteId(SpriteBagId("b"), 1, 2, 3), Position(x, 0))

    @Test
    fun `Update action saves token`() {
        val id = tokenId()
        val t = token(id, x = 42)

        handler.handle(SomeTokensUpdatedEvent(listOf(SomeTokensUpdatedEvent.TokenAction.Update(t))))

        assertEquals(t, fixture.stateRepository.findTokenById(id))
    }

    @Test
    fun `Delete action removes token`() {
        val id = tokenId()
        fixture.stateRepository.saveToken(token(id))

        handler.handle(SomeTokensUpdatedEvent(listOf(SomeTokensUpdatedEvent.TokenAction.Delete(id))))

        assertNull(fixture.stateRepository.findTokenById(id))
    }

    @Test
    fun `mix of Update and Delete applied correctly`() {
        val keepId = tokenId()
        val removeId = tokenId()
        fixture.stateRepository.saveToken(token(removeId))

        val newId = tokenId()
        handler.handle(SomeTokensUpdatedEvent(listOf(
            SomeTokensUpdatedEvent.TokenAction.Update(token(keepId)),
            SomeTokensUpdatedEvent.TokenAction.Delete(removeId),
            SomeTokensUpdatedEvent.TokenAction.Update(token(newId)),
        )))

        assertNotNull(fixture.stateRepository.findTokenById(keepId))
        assertNull(fixture.stateRepository.findTokenById(removeId))
        assertNotNull(fixture.stateRepository.findTokenById(newId))
    }

    @Test
    fun `invokes onTokensUpdated callback exactly once`() {
        var count = 0
        ClientContext.onTokensUpdated = { count++ }

        handler.handle(SomeTokensUpdatedEvent(emptyList()))

        assertEquals(1, count)
    }
}
