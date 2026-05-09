package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.application.ClientStateListener
import net.rafkos.ojkipojki.client.support.ClientContextFixture
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.domain.*
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpriteBagsUpdatedEventHandlerTest {

    private val handler = SpriteBagsUpdatedEventHandler()
    private lateinit var fixture: ClientContextFixture

    @BeforeEach
    fun setup() {
        fixture = clientContextFixture()
    }

    private fun spriteId(bag: String, r: Int) = SpriteId(SpriteBagId(bag), r, 0, 0)

    @Test
    fun `saves bags and nested sprites in state repository`() {
        val s1 = Sprite(spriteId("dice", 1), byteArrayOf(), byteArrayOf())
        val bag = SpriteBag(SpriteBagId("dice"), "dice", listOf(s1))

        handler.handle(SpriteBagsUpdatedEvent(listOf(bag)))

        assertNotNull(fixture.stateRepository.findSpriteBagById(SpriteBagId("dice")))
        assertNotNull(fixture.stateRepository.findSpriteById(spriteId("dice", 1)))
    }

    @Test
    fun `invokes onSpriteBagsUpdated listener exactly once`() {
        var count = 0
        fixture.notifier.addListener(object : ClientStateListener {
            override fun onSpriteBagsUpdated() { count++ }
        })

        handler.handle(SpriteBagsUpdatedEvent(listOf(SpriteBag(SpriteBagId("x"), "x", emptyList()))))

        assertEquals(1, count)
    }

    @Test
    fun `no listener does not throw`() {
        assertDoesNotThrow { handler.handle(SpriteBagsUpdatedEvent(emptyList())) }
    }
}
