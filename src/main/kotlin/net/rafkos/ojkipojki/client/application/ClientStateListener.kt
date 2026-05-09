package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent

interface ClientStateListener {
    fun onTokensUpdated() {}
    fun onTokensCountChanged() {}
    fun onSpriteBagsUpdated() {}
    fun onPointersUpdated() {}
    fun onConnectedClientsUpdated(count: Int) {}
    fun onGameInitializationUpdate(event: GameInitializationEvent) {}
}
