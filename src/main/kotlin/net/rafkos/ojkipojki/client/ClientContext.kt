package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent

object ClientContext {
    lateinit var eventDispatcher: EventDispatcher
    lateinit var stateRepository: StateRepository
    var onTokensUpdated: (() -> Unit)? = null
    var onSpriteBagsUpdated: (() -> Unit)? = null
    var onPointersUpdated: (() -> Unit)? = null
    var onConnectedClientsUpdated: ((Int) -> Unit)? = null
    var onGameInitializationUpdate: ((GameInitializationEvent) -> Unit)? = null
}
