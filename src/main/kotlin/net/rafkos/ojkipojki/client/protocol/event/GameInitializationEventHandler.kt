package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent

class GameInitializationEventHandler : Handler<GameInitializationEvent> {
    override fun handle(action: GameInitializationEvent) {
        ClientContext.onGameInitializationUpdate?.invoke(action)
    }
}