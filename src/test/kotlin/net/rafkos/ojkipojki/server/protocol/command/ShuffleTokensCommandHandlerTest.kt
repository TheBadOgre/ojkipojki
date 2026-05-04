package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.command.ShuffleTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.UUID

class ShuffleTokensCommandHandlerTest {

    private val handler = ShuffleTokensCommandHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        resetCommandContext()
    }

    private fun makeToken(x: Int, index: Int, locked: Boolean = false): TokenModel {
        return TokenModel().also {
            it.id = TokenId(UUID.randomUUID())
            it.spriteId = SpriteId(SpriteBagId("bag"), 1, 2, 3)
            it.position.apply(Position(x, 0))
            it.index.value = index
            it.locked = locked
        }.also { fixture.modelRepository.saveToken(it) }
    }

    @Test
    fun `fewer than 2 eligible tokens produces no broadcast`() {
        val t = makeToken(0, 0)
        handler.handle(ShuffleTokensCommand(listOf(t.id)))

        verify(fixture.eventBroadcastService, never()).broadcast(org.mockito.kotlin.any())
    }

    @Test
    fun `locked tokens are excluded from shuffle`() {
        val unlocked = makeToken(0, 0)
        val locked = makeToken(10, 1, locked = true)

        // Only 1 unlocked → no shuffle
        handler.handle(ShuffleTokensCommand(listOf(unlocked.id, locked.id)))

        verify(fixture.eventBroadcastService, never()).broadcast(org.mockito.kotlin.any())
    }

    @Test
    fun `shuffle preserves multiset of position-index pairs`() {
        val t1 = makeToken(0, 0)
        val t2 = makeToken(10, 1)
        val t3 = makeToken(20, 2)

        val beforeSlots = listOf(t1, t2, t3)
            .map { it.position.toState() to it.index.toState() }
            .toSet()

        handler.handle(ShuffleTokensCommand(listOf(t1.id, t2.id, t3.id)))

        val afterSlots = fixture.modelRepository.findAllTokens()
            .map { it.position.toState() to it.index.toState() }
            .toSet()

        assertEquals(beforeSlots, afterSlots)
    }

    @Test
    fun `shuffle broadcasts Update actions for all shuffled tokens`() {
        val t1 = makeToken(0, 0)
        val t2 = makeToken(10, 1)

        handler.handle(ShuffleTokensCommand(listOf(t1.id, t2.id)))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val updates = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>()
        assertEquals(2, updates.size)
    }
}
