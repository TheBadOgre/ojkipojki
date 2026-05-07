package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.UUID

class MissingSpriteBagsBroadcasterTest {

    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    private fun tokenWithMissingBag(bagId: SpriteBagId) {
        fixture.modelRepository.saveToken(
            TokenModel().also {
                it.id = TokenId(UUID.randomUUID())
                it.spriteId = SpriteId(bagId, 1, 0, 0)
            }
        )
    }

    @Test
    fun `broadcast sends MissingSpriteBagsEvent to all`() {
        val bagId = SpriteBagId("missing-bag")
        tokenWithMissingBag(bagId)

        MissingSpriteBagsBroadcaster.broadcast()

        val captor = argumentCaptor<net.rafkos.ojkipojki.shared.protocol.event.Event>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())

        val event = captor.allValues.filterIsInstance<MissingSpriteBagsEvent>().firstOrNull()
        assertTrue(event != null) { "Expected MissingSpriteBagsEvent to be broadcast" }
        assertEquals(setOf(bagId), event!!.ids)
    }

    @Test
    fun `broadcastTo sends MissingSpriteBagsEvent to specific client`() {
        val bagId = SpriteBagId("missing-bag")
        tokenWithMissingBag(bagId)

        MissingSpriteBagsBroadcaster.broadcastTo("client1")

        val eventCaptor = argumentCaptor<net.rafkos.ojkipojki.shared.protocol.event.Event>()
        val clientCaptor = argumentCaptor<String>()
        verify(fixture.eventBroadcastService).broadcast(eventCaptor.capture(), clientCaptor.capture())

        val event = eventCaptor.allValues.filterIsInstance<MissingSpriteBagsEvent>().firstOrNull()
        assertTrue(event != null) { "Expected MissingSpriteBagsEvent to be broadcast to client" }
        assertEquals(setOf(bagId), event!!.ids)
        assertEquals("client1", clientCaptor.firstValue)
    }
}
