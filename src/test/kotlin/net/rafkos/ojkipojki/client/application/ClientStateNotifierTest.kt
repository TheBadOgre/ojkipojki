package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClientStateNotifierTest {

    private val notifier = ClientStateNotifier()

    @Test
    fun `added listener receives notifications`() {
        var called = false
        val l = object : ClientStateListener { override fun onTokensUpdated() { called = true } }
        notifier.addListener(l)
        notifier.notifyTokensUpdated()
        assertTrue(called)
    }

    @Test
    fun `removed listener does not receive notifications`() {
        var called = false
        val l = object : ClientStateListener { override fun onTokensUpdated() { called = true } }
        notifier.addListener(l)
        notifier.removeListener(l)
        notifier.notifyTokensUpdated()
        assertFalse(called)
    }

    @Test
    fun `multiple listeners all receive notification`() {
        var a = 0; var b = 0
        notifier.addListener(object : ClientStateListener { override fun onTokensCountChanged() { a++ } })
        notifier.addListener(object : ClientStateListener { override fun onTokensCountChanged() { b++ } })
        notifier.notifyTokensCountChanged()
        assertEquals(1, a); assertEquals(1, b)
    }

    @Test
    fun `notifyConnectedClientsUpdated passes count`() {
        var received = -1
        notifier.addListener(object : ClientStateListener { override fun onConnectedClientsUpdated(count: Int) { received = count } })
        notifier.notifyConnectedClientsUpdated(42)
        assertEquals(42, received)
    }

    @Test
    fun `notifyGameInitializationUpdate passes event`() {
        var received: GameInitializationEvent? = null
        notifier.addListener(object : ClientStateListener { override fun onGameInitializationUpdate(event: GameInitializationEvent) { received = event } })
        val e = GameInitializationEvent(GameInitializationEvent.Status.DONE, "ok", 1.0)
        notifier.notifyGameInitializationUpdate(e)
        assertSame(e, received)
    }

    @Test
    fun `no listeners — all notify methods do not throw`() {
        assertDoesNotThrow {
            notifier.notifyTokensUpdated()
            notifier.notifyTokensCountChanged()
            notifier.notifySpriteBagsUpdated()
            notifier.notifyPointersUpdated()
            notifier.notifyConnectedClientsUpdated(0)
            notifier.notifyGameInitializationUpdate(GameInitializationEvent(GameInitializationEvent.Status.DONE, "x", 1.0))
        }
    }
}
