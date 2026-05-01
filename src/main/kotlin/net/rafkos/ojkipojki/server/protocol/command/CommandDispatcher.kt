package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.Command
import net.rafkos.ojkipojki.shared.protocol.command.DeleteTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand

class CommandDispatcher : Dispatcher<Command>(
    mapOf(
        UploadSpriteBagsCommand::class to UploadSpriteBagsCommandHandler() as Handler<in Command>,
        MoveTokensCommand::class to MoveTokensCommandHandler() as Handler<in Command>,
        SpawnTokensCommand::class to SpawnTokensCommandHandler() as Handler<in Command>,
        DeleteTokensCommand::class to DeleteTokensCommandHandler() as Handler<in Command>,
    )
)
