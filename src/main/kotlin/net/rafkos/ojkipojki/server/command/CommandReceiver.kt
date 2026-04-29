package net.rafkos.ojkipojki.server.command

import net.rafkos.ojkipojki.shared.Receiver
import net.rafkos.ojkipojki.shared.command.Command
import java.net.Socket

class CommandReceiver(
    socket: Socket,
    commandDispatcher: CommandDispatcher
) : Receiver<Command>(socket, commandDispatcher)