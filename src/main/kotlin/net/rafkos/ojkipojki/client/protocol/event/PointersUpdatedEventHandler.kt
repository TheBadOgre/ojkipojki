package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.PointersUpdatedEvent

class PointersUpdatedEventHandler : Handler<PointersUpdatedEvent> {
    override fun handle(action: PointersUpdatedEvent) {
        ClientContext.stateRepository.replaceAllPointers(action.pointers)
        ClientContext.onPointersUpdated?.invoke()
    }
}
