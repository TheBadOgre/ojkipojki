package net.rafkos.ojkipojki.client.command

import net.rafkos.ojkipojki.shared.Transmitter
import net.rafkos.ojkipojki.shared.command.Command
import java.net.Socket

class CommandTransmitter(socket: Socket) : Transmitter<Command>(socket)
