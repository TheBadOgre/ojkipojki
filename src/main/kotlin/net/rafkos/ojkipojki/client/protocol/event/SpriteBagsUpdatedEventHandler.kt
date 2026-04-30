package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent

class SpriteBagsUpdatedEventHandler : Handler<SpriteBagsUpdatedEvent> {
    override fun handle(action: SpriteBagsUpdatedEvent) {
        println("test")
    }
}