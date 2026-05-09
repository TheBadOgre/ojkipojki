package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import java.util.concurrent.CopyOnWriteArrayList

class ClientStateNotifier {
    private val listeners = CopyOnWriteArrayList<ClientStateListener>()

    fun addListener(l: ClientStateListener) { listeners.add(l) }
    fun removeListener(l: ClientStateListener) { listeners.remove(l) }

    fun notifyTokensUpdated() { listeners.forEach { it.onTokensUpdated() } }
    fun notifyTokensCountChanged() { listeners.forEach { it.onTokensCountChanged() } }
    fun notifySpriteBagsUpdated() { listeners.forEach { it.onSpriteBagsUpdated() } }
    fun notifyPointersUpdated() { listeners.forEach { it.onPointersUpdated() } }
    fun notifyConnectedClientsUpdated(count: Int) { listeners.forEach { it.onConnectedClientsUpdated(count) } }
    fun notifyGameInitializationUpdate(event: GameInitializationEvent) { listeners.forEach { it.onGameInitializationUpdate(event) } }
}
