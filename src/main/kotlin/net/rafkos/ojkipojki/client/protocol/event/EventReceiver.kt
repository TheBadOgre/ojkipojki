package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.shared.Receiver
import net.rafkos.ojkipojki.shared.event.Event
import java.net.Socket

class EventReceiver(
    socket: Socket,
    eventDispatcher: EventDispatcher
) : Receiver<Event>(socket, eventDispatcher)
