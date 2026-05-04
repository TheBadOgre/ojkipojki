package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.command.LockTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.UUID

class LockTokensCommandHandlerTest {

    private val handler = LockTokensCommandHandler()
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
    fun `sets locked=true on token`() {
        val t = makeToken(locked = false)
        handler.handle(LockTokensCommand(listOf(t.id), true))
        assertTrue(fixture.modelRepository.findTokenById(t.id)!!.locked)
    }

    @Test
    fun `sets locked=false on token`() {
        val t = makeToken(locked = true)
        handler.handle(LockTokensCommand(listOf(t.id), false))
        assertFalse(fixture.modelRepository.findTokenById(t.id)!!.locked)
    }

    @Test
    fun `unknown ids are skipped, only found tokens in broadcast`() {
        val real = makeToken()
        val ghost = TokenId(UUID.randomUUID())

        handler.handle(LockTokensCommand(listOf(real.id, ghost), true))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val updates = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>()
        assertEquals(1, updates.size)
        assertEquals(real.id, updates.first().token.id)
    }

    @Test
    fun `broadcasts Update action with updated locked state`() {
        val t = makeToken(locked = false)
        handler.handle(LockTokensCommand(listOf(t.id), true))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val update = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>().first()
        assertTrue(update.token.locked)
    }
}
