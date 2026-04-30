package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.shared.protocol.Receiver
import net.rafkos.ojkipojki.shared.protocol.command.Command
import java.net.Socket

class CommandReceiver(
    socket: Socket,
    commandDispatcher: CommandDispatcher,
    onDisconnected: (() -> Unit)? = null
) : Receiver<Command>(socket, commandDispatcher, onDisconnected)