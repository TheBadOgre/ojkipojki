package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Receiver
import net.rafkos.ojkipojki.shared.protocol.command.Command
import java.net.Socket

class CommandReceiver(
    socket: Socket,
    onDisconnected: (() -> Unit)? = null
) : Receiver<Command>(socket, ServerContext.commandDispatcher, onDisconnected)