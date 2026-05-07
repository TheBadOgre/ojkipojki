package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent

object MissingSpriteBagsBroadcaster {
    fun broadcast() {
        val ids = ServerContext.modelRepository.missingBagIds()
        ServerContext.eventBroadcastService.broadcast(MissingSpriteBagsEvent(ids))
    }
    fun broadcastTo(clientId: String) {
        val ids = ServerContext.modelRepository.missingBagIds()
        ServerContext.eventBroadcastService.broadcast(MissingSpriteBagsEvent(ids), clientId)
    }
}
