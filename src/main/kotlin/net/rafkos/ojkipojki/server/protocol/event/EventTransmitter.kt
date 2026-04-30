package net.rafkos.ojkipojki.server.protocol.event

import net.rafkos.ojkipojki.shared.protocol.Transmitter
import net.rafkos.ojkipojki.shared.protocol.event.Event
import java.net.Socket

class EventTransmitter(socket: Socket) : Transmitter<Event>(socket)
