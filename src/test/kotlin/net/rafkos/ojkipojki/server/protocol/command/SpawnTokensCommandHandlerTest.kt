package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.server.model.SpriteModel
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SpawnTokensCommandHandlerTest {

    private val handler = SpawnTokensCommandHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        resetCommandContext()
    }

    private fun spriteId(bag: String, r: Int) = SpriteId(SpriteBagId(bag), r, 0, 0)

    private fun makeBag(bagIdStr: String, spriteCount: Int): SpriteBagModel {
        val model = SpriteBagModel().also { it.id = SpriteBagId(bagIdStr) }
        repeat(spriteCount) { i ->
            val sprite = SpriteModel().also { it.id = spriteId(bagIdStr, i + 1) }
            model.sprites.add(sprite)
        }
        fixture.modelRepository.saveSpriteBag(model)
        return model
    }

    @Test
    fun `unknown bag is no-op and no broadcast`() {
        handler.handle(SpawnTokensCommand(SpriteBagId("ghost"), Position(0, 0)))

        verify(fixture.eventBroadcastService, never()).broadcast(org.mockito.kotlin.any())
    }

    @Test
    fun `spawn single sprite at given position`() {
        val bag = makeBag("dice", 1)
        val pos = Position(10, 20)

        handler.handle(SpawnTokensCommand(SpriteBagId("dice"), pos, bag.sprites.first().id))

        val tokens = fixture.modelRepository.findAllTokens()
        assertEquals(1, tokens.size)
        assertEquals(pos, tokens.first().position.toState())
    }

    @Test
    fun `spawn all sprites in bag creates one token per sprite`() {
        makeBag("deck", 3)

        handler.handle(SpawnTokensCommand(SpriteBagId("deck"), Position(0, 0)))

        assertEquals(3, fixture.modelRepository.findAllTokens().size)
    }

    @Test
    fun `count multiplies number of tokens spawned`() {
        makeBag("coins", 2)

        handler.handle(SpawnTokensCommand(SpriteBagId("coins"), Position(0, 0), count = 3))

        assertEquals(6, fixture.modelRepository.findAllTokens().size)
    }

    @Test
    fun `spawned tokens have monotonically increasing index`() {
        makeBag("tiles", 3)

        handler.handle(SpawnTokensCommand(SpriteBagId("tiles"), Position(0, 0), count = 2))

        val indices = fixture.modelRepository.findAllTokens().map { it.index.value }.sorted()
        assertEquals((0 until 6).toList(), indices)
    }

    @Test
    fun `null position uses random in -400 to 400 range`() {
        makeBag("rand", 1)

        repeat(10) {
            fixture.modelRepository.deleteAllTokens()
            handler.handle(SpawnTokensCommand(SpriteBagId("rand"), null))
            val token = fixture.modelRepository.findAllTokens().first()
            assertTrue(token.position.x in -400..400) { "x=${token.position.x} out of range" }
            assertTrue(token.position.y in -400..400) { "y=${token.position.y} out of range" }
        }
    }

    @Test
    fun `broadcast contains update actions for spawned tokens only`() {
        makeBag("few", 2)
        handler.handle(SpawnTokensCommand(SpriteBagId("few"), Position(0, 0)))

        val captor = argumentCaptor<SomeTokensUpdatedEvent>()
        verify(fixture.eventBroadcastService).broadcast(captor.capture())
        val updates = captor.firstValue.tokenActions.filterIsInstance<SomeTokensUpdatedEvent.TokenAction.Update>()
        assertEquals(2, updates.size)
    }
}
