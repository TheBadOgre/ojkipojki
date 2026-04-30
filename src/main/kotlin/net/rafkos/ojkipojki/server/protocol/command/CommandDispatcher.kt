package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.Command
import net.rafkos.ojkipojki.shared.protocol.command.DummyCommand

class CommandDispatcher(
    eventBroadcastService: EventBroadcastService
) : Dispatcher<Command>(
    mapOf(
        DummyCommand::class to DummyCommandHandler(eventBroadcastService) as Handler<in Command>
    )
)
