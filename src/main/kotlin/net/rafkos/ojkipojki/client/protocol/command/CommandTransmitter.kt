package net.rafkos.ojkipojki.client.protocol.command

import net.rafkos.ojkipojki.shared.protocol.Transmitter
import net.rafkos.ojkipojki.shared.protocol.command.Command
import java.net.Socket

class CommandTransmitter(socket: Socket) : Transmitter<Command>(socket)
