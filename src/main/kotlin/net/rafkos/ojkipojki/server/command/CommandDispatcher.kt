package net.rafkos.ojkipojki.server.command

import net.rafkos.ojkipojki.server.event.EventBroadcastService
import net.rafkos.ojkipojki.server.model.Model
import net.rafkos.ojkipojki.shared.Dispatcher
import net.rafkos.ojkipojki.shared.Handler
import net.rafkos.ojkipojki.shared.command.Command
import net.rafkos.ojkipojki.shared.command.DummyCommand

class CommandDispatcher(
    model: Model,
    eventBroadcastService: EventBroadcastService
) : Dispatcher<Command>(
    mapOf(
        DummyCommand::class to DummyCommandHandler(model, eventBroadcastService) as Handler<in Command>
    )
)
