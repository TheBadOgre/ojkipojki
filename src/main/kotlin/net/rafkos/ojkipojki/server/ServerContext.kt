package net.rafkos.ojkipojki.server

import net.rafkos.ojkipojki.server.application.ClientColorRegistry
import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.application.PointerRepository
import net.rafkos.ojkipojki.server.protocol.command.CommandDispatcher
import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService

object ServerContext {
    lateinit var modelRepository: ModelRepository
    lateinit var eventBroadcastService: EventBroadcastService
    lateinit var commandDispatcher: CommandDispatcher
    lateinit var pointerRepository: PointerRepository
    lateinit var clientColorRegistry: ClientColorRegistry
}
