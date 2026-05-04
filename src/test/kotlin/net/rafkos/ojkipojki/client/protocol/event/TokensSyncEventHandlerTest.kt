package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.event.TokensSyncEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class TokensSyncEventHandlerTest {

    private val handler = TokensSyncEventHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.client.support.ClientContextFixture

    @BeforeEach
    fun setup() {
        fixture = clientContextFixture()
    }

    private fun tokenId() = TokenId(UUID.randomUUID())
    private fun token(id: TokenId) = Token(id, SpriteId(SpriteBagId("b"), 1, 2, 3))

    @Test
    fun `replaces full token set`() {
        val oldId = tokenId()
        fixture.stateRepository.saveToken(token(oldId))

        val newId = tokenId()
        handler.handle(TokensSyncEvent(listOf(token(newId))))

        assertNull(fixture.stateRepository.findTokenById(oldId))
        assertNotNull(fixture.stateRepository.findTokenById(newId))
    }

    @Test
    fun `existing token not in payload is removed`() {
        val t = token(tokenId())
        fixture.stateRepository.saveToken(t)

        handler.handle(TokensSyncEvent(emptyList()))

        assertTrue(fixture.stateRepository.findAllTokens().isEmpty())
    }

    @Test
    fun `invokes onTokensUpdated callback exactly once`() {
        var count = 0
        ClientContext.onTokensUpdated = { count++ }

        handler.handle(TokensSyncEvent(emptyList()))

        assertEquals(1, count)
    }
}
