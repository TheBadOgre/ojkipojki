package net.rafkos.ojkipojki.server.protocol.event

import net.rafkos.ojkipojki.shared.Transmitter
import net.rafkos.ojkipojki.shared.event.Event
import java.net.Socket

class EventTransmitter(socket: Socket) : Transmitter<Event>(socket)
