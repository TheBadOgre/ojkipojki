package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent

class ConnectedClientsUpdateEventHandler : Handler<ConnectedClientsUpdateEvent> {
    override fun handle(action: ConnectedClientsUpdateEvent) {
        ClientContext.notifier.notifyConnectedClientsUpdated(action.numOfClients)
    }
}
