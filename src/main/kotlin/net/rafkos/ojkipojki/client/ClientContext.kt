package net.rafkos.ojkipojki.client

import net.rafkos.ojkipojki.client.application.ClientStateNotifier
import net.rafkos.ojkipojki.client.application.LocalSpriteBagRegistry
import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.protocol.event.EventDispatcher

object ClientContext {
    lateinit var eventDispatcher: EventDispatcher
    lateinit var stateRepository: StateRepository
    lateinit var localSpriteBagRegistry: LocalSpriteBagRegistry
    lateinit var notifier: ClientStateNotifier
    var commandTransmitter: CommandTransmitter? = null
}
