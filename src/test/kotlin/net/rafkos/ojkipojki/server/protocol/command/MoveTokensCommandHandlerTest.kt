package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.UUID

class MoveTokensCommandHandlerTest {

    private val handler = MoveTokensCommandHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        resetCommandContext()
    }

    private fun makeToken(index: Int = 0, locked: Boolean = false): TokenModel {
        return TokenModel().also {
            it.id = TokenId(UUID.randomUUID())
            it.spriteId = SpriteId(SpriteBagId("bag"), 1, 2, 3)
            it.index.value = index
            it.locked = locked
        }.also { fixture.modelRepository.saveToken(it) }
    }

    private fun adjustment(
        id: TokenId,
        position: Position? = null,
        rotation: Rotation? = null,
        flipped: Boolean? = null,
        index: Index? = null,
    ) = MoveTokensCommand.TokenIdAndPosition(id, position, rotation, flipped, index)

    @Test
    fun `position-only update moves token`() {
        val t = makeToken()
        val newPos = Position(100, 200)

        handler.handle(MoveTokensCommand(listOf(adjustment(t.id, position = newPos))))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val updated = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>()
        assertEquals(1, updated.size)
        assertEquals(newPos, updated.first().token.position)
    }

    @Test
    fun `rotation-only update rotates token`() {
        val t = makeToken()
        handler.handle(MoveTokensCommand(listOf(adjustment(t.id, rotation = Rotation(90.0)))))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val updated = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>()
        assertEquals(90.0, updated.first().token.rotation.degrees)
    }

    @Test
    fun `flipped update is applied`() {
        val t = makeToken()
        handler.handle(MoveTokensCommand(listOf(adjustment(t.id, flipped = true))))

        assertTrue(fixture.modelRepository.findTokenById(t.id)!!.flipped)
    }

    @Test
    fun `locked token is not moved`() {
        val t = makeToken(locked = true)
        handler.handle(MoveTokensCommand(listOf(adjustment(t.id, position = Position(99, 99)))))

        val stored = fixture.modelRepository.findTokenById(t.id)!!
        assertEquals(0, stored.position.x)
        assertEquals(0, stored.position.y)
    }

    @Test
    fun `unknown token id is no-op`() {
        val ghostId = TokenId(UUID.randomUUID())
        handler.handle(MoveTokensCommand(listOf(adjustment(ghostId, position = Position(1, 1)))))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        assertTrue(captor.firstValue.tokenActions.isEmpty())
    }

    @Test
    fun `index change triggers normalization and broadcasts all tokens`() {
        val t0 = makeToken(index = 0)
        val t1 = makeToken(index = 1)
        val t2 = makeToken(index = 2)

        // Move t0 to index 2 (above t2)
        handler.handle(MoveTokensCommand(listOf(adjustment(t0.id, index = Index(2)))))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val updated = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>()

        // All 3 tokens broadcast
        assertEquals(3, updated.size)

        // Indices after normalization are contiguous 0..2
        val indices = updated.map { it.token.index.value }.sorted()
        assertEquals(listOf(0, 1, 2), indices)
    }
}
