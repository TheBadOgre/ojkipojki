package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher

object ClientContext {
    lateinit var eventDispatcher: EventDispatcher
    lateinit var stateRepository: StateRepository
    var onTokensUpdated: (() -> Unit)? = null
    var onSpriteBagsUpdated: (() -> Unit)? = null
}