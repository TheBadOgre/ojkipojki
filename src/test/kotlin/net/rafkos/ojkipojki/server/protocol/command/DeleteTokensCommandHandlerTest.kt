package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.command.DeleteTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.UUID

class DeleteTokensCommandHandlerTest {

    private val handler = DeleteTokensCommandHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        resetCommandContext()
    }

    private fun makeToken(locked: Boolean = false): TokenModel {
        return TokenModel().also {
            it.id = TokenId(UUID.randomUUID())
            it.spriteId = SpriteId(SpriteBagId("bag"), 1, 2, 3)
            it.locked = locked
        }.also { fixture.modelRepository.saveToken(it) }
    }

    @Test
    fun `non-locked token is deleted`() {
        val t = makeToken()
        handler.handle(DeleteTokensCommand(listOf(t.id)))
        assertNull(fixture.modelRepository.findTokenById(t.id))
    }

    @Test
    fun `locked token is not deleted`() {
        val t = makeToken(locked = true)
        handler.handle(DeleteTokensCommand(listOf(t.id)))
        assertNotNull(fixture.modelRepository.findTokenById(t.id))
    }

    @Test
    fun `only non-locked tokens appear in broadcast as Delete actions`() {
        val unlocked = makeToken()
        val locked = makeToken(locked = true)

        handler.handle(DeleteTokensCommand(listOf(unlocked.id, locked.id)))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val deletes = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Delete>()

        assertEquals(1, deletes.size)
        assertEquals(unlocked.id, deletes.first().tokenId)
    }

    @Test
    fun `deleting multiple non-locked tokens broadcasts all as Delete`() {
        val t1 = makeToken()
        val t2 = makeToken()

        handler.handle(DeleteTokensCommand(listOf(t1.id, t2.id)))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val deletes = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Delete>()
        assertEquals(setOf(t1.id, t2.id), deletes.map { it.tokenId }.toSet())
    }

    @Test
    fun `unknown token ids produce empty broadcast`() {
        val ghostId = TokenId(UUID.randomUUID())
        handler.handle(DeleteTokensCommand(listOf(ghostId)))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        assertTrue(captor.firstValue.tokenActions.isEmpty())
    }
}
