package net.rafkos.ojkipojki.server.command

import net.rafkos.ojkipojki.server.model.Model
import net.rafkos.ojkipojki.shared.command.Command
import net.rafkos.ojkipojki.shared.command.DummyCommand
import net.rafkos.ojkipojki.shared.Dispatcher
import net.rafkos.ojkipojki.shared.Handler
import org.example.net.rafkos.ojkipojki.server.event.EventBroadcastService

class CommandDispatcher(
    model: Model,
    eventBroadcastService: EventBroadcastService? = null
) : Dispatcher<Command>(
    mapOf(
        DummyCommand::class to DummyCommandHandler(model, eventBroadcastService) as Handler<in Command>
    )
)