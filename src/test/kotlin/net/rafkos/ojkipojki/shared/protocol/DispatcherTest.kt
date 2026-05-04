package net.rafkos.ojkipojki.shared.protocol

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class DispatcherTest {

    private sealed class TestAction {
        data class Known(val value: Int) : TestAction()
        data class Unknown(val value: Int) : TestAction()
    }

    private class TestDispatcher(handler: Handler<TestAction.Known>) : Dispatcher<TestAction>(
        mapOf(TestAction.Known::class to handler as Handler<in TestAction>)
    )

    @Test
    fun `dispatching mapped type invokes handler`() {
        val handler = mock<Handler<TestAction.Known>>()
        val dispatcher = TestDispatcher(handler)
        val action = TestAction.Known(42)

        dispatcher.dispatch(action)

        verify(handler).handle(action)
    }

    @Test
    fun `dispatching unmapped type does not throw`() {
        val handler = mock<Handler<TestAction.Known>>()
        val dispatcher = TestDispatcher(handler)

        assertDoesNotThrow { dispatcher.dispatch(TestAction.Unknown(1)) }
    }

    @Test
    fun `dispatching unmapped type does not invoke handler`() {
        val handler = mock<Handler<TestAction.Known>>()
        val dispatcher = TestDispatcher(handler)

        dispatcher.dispatch(TestAction.Unknown(1))

        verify(handler, never()).handle(org.mockito.kotlin.any())
    }
}
