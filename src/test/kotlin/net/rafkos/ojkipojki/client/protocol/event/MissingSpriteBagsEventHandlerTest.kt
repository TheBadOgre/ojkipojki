package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.application.LocalSpriteBagRegistry
import net.rafkos.ojkipojki.client.application.SpriteLoadResult
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class MissingSpriteBagsEventHandlerTest {

    private val handler = MissingSpriteBagsEventHandler()
    private lateinit var transmitter: CommandTransmitter

    @BeforeEach
    fun setup() {
        clientContextFixture()
        transmitter = mock()
        ClientContext.commandTransmitter = transmitter
    }

    @AfterEach
    fun teardown() {
        ClientContext.commandTransmitter = null
    }

    private fun registryWith(bags: List<SpriteBag>): LocalSpriteBagRegistry =
        object : LocalSpriteBagRegistry() {
            init { loaded = SpriteLoadResult(bags, emptyMap()) }
            override suspend fun reload() { /* no-op */ }
        }

    private fun bag(id: String) = SpriteBag(SpriteBagId(id), id, emptyList())

    @Test
    fun `empty ids does not transmit`() {
        ClientContext.localSpriteBagRegistry = registryWith(listOf(bag("a")))

        handler.handle(MissingSpriteBagsEvent(emptySet()))

        verify(transmitter, never()).transmit(any())
    }

    @Test
    fun `ids with no local match does not transmit`() {
        ClientContext.localSpriteBagRegistry = registryWith(listOf(bag("a")))

        handler.handle(MissingSpriteBagsEvent(setOf(SpriteBagId("b"))))

        verify(transmitter, never()).transmit(any())
    }

    @Test
    fun `ids with single local match transmits UploadSpriteBagsCommand for that bag only`() {
        val bagA = bag("a")
        val bagB = bag("b")
        ClientContext.localSpriteBagRegistry = registryWith(listOf(bagA, bagB))

        handler.handle(MissingSpriteBagsEvent(setOf(SpriteBagId("a"))))

        verify(transmitter).transmit(UploadSpriteBagsCommand(listOf(bagA)))
    }

    @Test
    fun `ids with multiple local matches transmits one UploadSpriteBagsCommand with all matches`() {
        val bagA = bag("a")
        val bagB = bag("b")
        ClientContext.localSpriteBagRegistry = registryWith(listOf(bagA, bagB))

        handler.handle(MissingSpriteBagsEvent(setOf(SpriteBagId("a"), SpriteBagId("b"))))

        verify(transmitter).transmit(UploadSpriteBagsCommand(listOf(bagA, bagB)))
    }

    @Test
    fun `null commandTransmitter does not crash`() {
        ClientContext.commandTransmitter = null
        ClientContext.localSpriteBagRegistry = registryWith(listOf(bag("a")))

        assertDoesNotThrow {
            handler.handle(MissingSpriteBagsEvent(setOf(SpriteBagId("a"))))
        }
    }
}
