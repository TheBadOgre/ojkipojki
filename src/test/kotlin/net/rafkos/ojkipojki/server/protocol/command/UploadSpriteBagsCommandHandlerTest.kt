package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import net.rafkos.ojkipojki.shared.protocol.event.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.verify
import java.util.UUID

class UploadSpriteBagsCommandHandlerTest {

    private val handler = UploadSpriteBagsCommandHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        resetCommandContext()
    }

    private fun spriteId(bag: String, r: Int, g: Int = 0, b: Int = 0) =
        SpriteId(SpriteBagId(bag), r, g, b)

    private fun sprite(bag: String, r: Int) =
        Sprite(spriteId(bag, r), byteArrayOf(r.toByte()), byteArrayOf())

    private fun bag(id: String, vararg sprites: Sprite) =
        SpriteBag(SpriteBagId(id), "group-$id", sprites.toList())

    @Test
    fun `new bag inserted into repository`() {
        val b = bag("dice", sprite("dice", 1))
        handler.handle(UploadSpriteBagsCommand(listOf(b)))
        assertNotNull(fixture.modelRepository.findSpriteBagById(SpriteBagId("dice")))
    }

    @Test
    fun `existing bag updated in repository`() {
        val initial = bag("dice", sprite("dice", 1))
        handler.handle(UploadSpriteBagsCommand(listOf(initial)))

        val updated = bag("dice", sprite("dice", 2))
        handler.handle(UploadSpriteBagsCommand(listOf(updated)))

        val stored = fixture.modelRepository.findSpriteBagById(SpriteBagId("dice"))
        assertNotNull(stored)
        assertEquals(1, stored!!.sprites.size)
        assertEquals(spriteId("dice", 2), stored.sprites.first().id)
    }

    @Test
    fun `tokens whose spriteId belongs to removed sprite are deleted`() {
        val s1 = sprite("dice", 1)
        val s2 = sprite("dice", 2)
        val b = bag("dice", s1, s2)
        handler.handle(UploadSpriteBagsCommand(listOf(b)))

        // spawn a token with sprite s2
        val token = TokenModel().also {
            it.id = TokenId(UUID.randomUUID())
            it.spriteId = s2.id
        }
        fixture.modelRepository.saveToken(token)

        // now re-upload without s2 → token should be deleted
        handler.handle(UploadSpriteBagsCommand(listOf(bag("dice", s1))))

        assertNull(fixture.modelRepository.findTokenById(token.id))
    }

    @Test
    fun `broadcasts TokensSyncEvent SpriteBagsUpdatedEvent and two GameInitializationEvents`() {
        handler.handle(UploadSpriteBagsCommand(listOf(bag("dice"))))

        val captor = argumentCaptor<Event>()
        verify(fixture.eventBroadcastService, atLeast(4)).broadcast(captor.capture())

        val types = captor.allValues.map { it::class }
        assertTrue(types.contains(TokensSyncEvent::class)) { "Missing TokensSyncEvent" }
        assertTrue(types.contains(SpriteBagsUpdatedEvent::class)) { "Missing SpriteBagsUpdatedEvent" }
        assertEquals(2, types.count { it == GameInitializationEvent::class }) { "Expected 2 GameInitializationEvents" }
    }
}
