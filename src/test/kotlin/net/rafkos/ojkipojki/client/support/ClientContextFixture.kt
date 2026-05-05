package net.rafkos.ojkipojki.client.support

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher

class ClientContextFixture(val stateRepository: StateRepository)

fun clientContextFixture(): ClientContextFixture {
    val repo = StateRepository()
    ClientContext.stateRepository = repo
    ClientContext.eventDispatcher = EventDispatcher()
    ClientContext.onTokensUpdated = null
    ClientContext.onTokensCountChanged = null
    ClientContext.onSpriteBagsUpdated = null
    ClientContext.onPointersUpdated = null
    ClientContext.onConnectedClientsUpdated = null
    ClientContext.onGameInitializationUpdate = null
    return ClientContextFixture(repo)
}
