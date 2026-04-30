package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.Command
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand

class CommandDispatcher : Dispatcher<Command>(
    mapOf(
        UploadSpriteBagsCommand::class to UploadSpriteBagsCommandHandler() as Handler<in Command>
    )
)
