package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.application.CommandContext
import net.rafkos.ojkipojki.server.support.resetCommandContext
import net.rafkos.ojkipojki.server.support.serverContextFixture
import net.rafkos.ojkipojki.shared.domain.Pointer
import net.rafkos.ojkipojki.shared.protocol.command.MovePointerCommand
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class MovePointerCommandHandlerTest {

    private val handler = MovePointerCommandHandler()
    private lateinit var fixture: net.rafkos.ojkipojki.server.support.ServerContextFixture

    @BeforeEach
    fun setup() {
        fixture = serverContextFixture()
    }

    @AfterEach
    fun teardown() {
        resetCommandContext()
    }

    @Test
    fun `saves pointer and calls broadcastCustom when clientId and color present`() {
        fixture.clientColorRegistry.assign("c1")
        CommandContext.clientId = "c1"

        handler.handle(MovePointerCommand(50, 75))

        val pointers = fixture.pointerRepository.findAllExcept("nobody")
        assertEquals(1, pointers.size)
        val (r, g, b) = fixture.clientColorRegistry.getColor("c1")!!
        assertEquals(Pointer(50, 75, r, g, b), pointers.first())

        verify(fixture.eventBroadcastService).broadcastCustom(anyOrNull(), any())
    }

    @Test
    fun `null clientId returns early with no broadcast`() {
        CommandContext.clientId = null

        handler.handle(MovePointerCommand(1, 2))

        verify(fixture.eventBroadcastService, never()).broadcastCustom(any(), any())
        assertTrue(fixture.pointerRepository.findAllExcept("").isEmpty())
    }

    @Test
    fun `missing color returns early with no broadcast`() {
        CommandContext.clientId = "c-no-color"
        // color not assigned

        handler.handle(MovePointerCommand(1, 2))

        verify(fixture.eventBroadcastService, never()).broadcastCustom(any(), any())
    }

    @Test
    fun `broadcastCustom excludes the sender client`() {
        fixture.clientColorRegistry.assign("sender")
        CommandContext.clientId = "sender"

        handler.handle(MovePointerCommand(10, 20))

        val captor = argumentCaptor<String>()
        verify(fixture.eventBroadcastService).broadcastCustom(captor.capture(), any())
        assertEquals("sender", captor.firstValue)
    }
}
