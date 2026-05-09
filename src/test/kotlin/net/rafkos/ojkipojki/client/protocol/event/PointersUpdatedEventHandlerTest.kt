package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.application.ClientStateListener
import net.rafkos.ojkipojki.client.support.ClientContextFixture
import net.rafkos.ojkipojki.client.support.clientContextFixture
import net.rafkos.ojkipojki.shared.domain.Pointer
import net.rafkos.ojkipojki.shared.protocol.event.PointersUpdatedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PointersUpdatedEventHandlerTest {

    private val handler = PointersUpdatedEventHandler()
    private lateinit var fixture: ClientContextFixture

    @BeforeEach
    fun setup() {
        fixture = clientContextFixture()
    }

    private fun pointer(r: Int, g: Int = 0, b: Int = 0) = Pointer(10, 20, r, g, b)

    @Test
    fun `replaces all pointers in state repository`() {
        fixture.stateRepository.replaceAllPointers(listOf(pointer(1)))

        handler.handle(PointersUpdatedEvent(listOf(pointer(200), pointer(100))))

        val result = fixture.stateRepository.findAllPointers()
        assertEquals(2, result.size)
        assertTrue(result.any { it.red == 200 })
        assertFalse(result.any { it.red == 1 })
    }

    @Test
    fun `invokes onPointersUpdated listener exactly once`() {
        var count = 0
        fixture.notifier.addListener(object : ClientStateListener {
            override fun onPointersUpdated() { count++ }
        })

        handler.handle(PointersUpdatedEvent(emptyList()))

        assertEquals(1, count)
    }

    @Test
    fun `no listener does not throw`() {
        assertDoesNotThrow { handler.handle(PointersUpdatedEvent(emptyList())) }
    }
}
