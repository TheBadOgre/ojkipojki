package net.rafkos.ojkipojki.client.support

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.application.ClientStateNotifier
import net.rafkos.ojkipojki.client.application.LocalSpriteBagRegistry
import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher

class ClientContextFixture(val stateRepository: StateRepository, val notifier: ClientStateNotifier)

fun clientContextFixture(): ClientContextFixture {
    val repo = StateRepository()
    val notifier = ClientStateNotifier()
    ClientContext.stateRepository = repo
    ClientContext.eventDispatcher = EventDispatcher()
    ClientContext.localSpriteBagRegistry = LocalSpriteBagRegistry()
    ClientContext.notifier = notifier
    ClientContext.commandTransmitter = null
    return ClientContextFixture(repo, notifier)
}
