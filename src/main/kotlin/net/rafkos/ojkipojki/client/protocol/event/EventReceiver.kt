package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Receiver
import net.rafkos.ojkipojki.shared.protocol.event.Event
import java.net.Socket

class EventReceiver(
    socket: Socket
) : Receiver<Event>(socket, ClientContext.eventDispatcher)
