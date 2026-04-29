package net.rafkos.ojkipojki.client.event

import net.rafkos.ojkipojki.shared.Receiver
import net.rafkos.ojkipojki.shared.event.Event
import java.net.Socket

class EventReceiver(
    socket: Socket,
    eventDispatcher: EventDispatcher
) : Receiver<Event>(socket, eventDispatcher)